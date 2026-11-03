package com.dualphone.controller.transport.wifi

import android.content.Context
import android.os.Build
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.transport.ConnectionDiagnostics
import com.dualphone.controller.transport.PairingParser
import com.dualphone.controller.transport.ReceiverState
import com.dualphone.controller.transport.InputTransport
import com.dualphone.controller.transport.protocol.BinaryPacketProtocol
import com.dualphone.controller.transport.protocol.SessionAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * TCP session + UDP live input transport.
 *
 * Receiver side: startServer(tcpPort) → accepts TCP, authenticates, opens UDP listener.
 * Controller side: connect(host, tcpPort, pairingToken) → auth → send UDP at ~120 Hz.
 */
class TcpUdpInputTransport(
    private val context: Context
) : InputTransport, WiFiTransport {

    companion object {
        private const val TAG = "TcpUdpTransport"
        const val DEFAULT_TCP_PORT = 17420
        const val DEFAULT_UDP_PORT = 17421
        const val TARGET_HZ = 120
        private const val FRAME_INTERVAL_MS = 1000L / TARGET_HZ
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: Flow<Boolean> = _isConnected

    private val _diagnostics = MutableStateFlow(ConnectionDiagnostics())
    val diagnostics: StateFlow<ConnectionDiagnostics> = _diagnostics.asStateFlow()

    private val _receiverState = MutableStateFlow(ReceiverState.STOPPED)
    val receiverState: StateFlow<ReceiverState> = _receiverState.asStateFlow()

    private val _incomingState = MutableSharedFlow<GamepadState>(
        extraBufferCapacity = 8,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var udpSocket: DatagramSocket? = null
    private var acceptJob: Job? = null
    private var receiveJob: Job? = null
    private var sendJob: Job? = null
    private var heartbeatJob: Job? = null

    private val running = AtomicBoolean(false)
    private var sessionId: Int = 0
    private var expectedPairingToken: Int = 0
    private var remoteUdpAddress: InetAddress? = null
    private var remoteUdpPort: Int = DEFAULT_UDP_PORT
    private var localUdpPort: Int = DEFAULT_UDP_PORT
    private var isReceiver = false

    private val packetsSent = AtomicLong(0)
    private val packetsReceived = AtomicLong(0)
    private val lastSeq = AtomicInteger(-1)
    private val lostEstimate = AtomicLong(0)
    private var lastPacketMs = 0L
    private var rateWindowStart = 0L
    private var rateCount = 0
    private var currentHz = 0f

    private val latestState = AtomicReferenceGamepad()
    private val hasPending = AtomicBoolean(false)

    // --- Receiver API ---

    suspend fun startReceiver(
        tcpPort: Int = DEFAULT_TCP_PORT,
        udpPort: Int = DEFAULT_UDP_PORT
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (running.get()) return@withContext Result.failure(IllegalStateException("Already running"))
        try {
            isReceiver = true
            localUdpPort = udpPort
            expectedPairingToken = SessionAuth.generatePairingToken()
            sessionId = 0

            serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(tcpPort))
                soTimeout = 1000
            }

            udpSocket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(udpPort))
                soTimeout = 50
            }

            running.set(true)
            _receiverState.value = ReceiverState.LISTENING
            val localIp = NetworkUtils.getLocalIpAddress(context) ?: "0.0.0.0"
            android.util.Log.i(TAG, "Receiver listening on $localIp:$tcpPort (token redacted)")
            updateDiagnostics {
                it.copy(
                    isConnected = false,
                    connectionType = "Wi-Fi TCP/UDP",
                    localIp = localIp,
                    tcpPort = tcpPort,
                    udpPort = udpPort,
                    pairingTokenDisplay = formatToken(expectedPairingToken),
                    statusMessage = "Listening on $localIp:$tcpPort"
                )
            }

            acceptJob = scope.launch { acceptLoop(tcpPort, udpPort) }
            receiveJob = scope.launch { udpReceiveLoop() }
            Result.success(Unit)
        } catch (e: Exception) {
            stopInternal()
            Result.failure(e)
        }
    }

    private suspend fun acceptLoop(tcpPort: Int, udpPort: Int) {
        while (running.get() && scope.isActive) {
            try {
                val socket = serverSocket?.accept() ?: continue
                // Only one controller at a time for Milestone 2
                if (_isConnected.value) {
                    socket.close()
                    continue
                }
                handleIncomingClient(socket, udpPort)
            } catch (_: SocketTimeoutException) {
                // normal
            } catch (e: Exception) {
                if (running.get()) {
                    updateDiagnostics { it.copy(statusMessage = "Accept error: ${e.message}") }
                }
            }
        }
    }

    private suspend fun handleIncomingClient(socket: Socket, udpPort: Int) {
        try {
            socket.soTimeout = 8000 // auth only
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            val authBuf = ByteArray(SessionAuth.AUTH_FRAME_SIZE)
            input.readFully(authBuf)
            val request = SessionAuth.decodeAuthRequest(authBuf)
            if (request == null || request.pairingToken != expectedPairingToken) {
                val fail = SessionAuth.encodeAuthResponse(0, false, udpPort)
                output.write(fail)
                output.flush()
                socket.close()
                updateDiagnostics { it.copy(statusMessage = "Auth rejected") }
                return
            }

            sessionId = SessionAuth.generateSessionId()
            val ok = SessionAuth.encodeAuthResponse(sessionId, true, udpPort)
            output.write(ok)
            output.flush()

            clientSocket = socket
            remoteUdpAddress = socket.inetAddress
            // Controller will send UDP from ephemeral port; we accept from any after auth
            _isConnected.value = true
            _receiverState.value = ReceiverState.CONNECTED
            android.util.Log.i(TAG, "Authentication accepted, session established")
            lastSeq.set(-1)
            packetsReceived.set(0)
            lostEstimate.set(0)

            updateDiagnostics {
                it.copy(
                    isConnected = true,
                    remoteDeviceName = request.deviceName,
                    remoteAddress = socket.inetAddress.hostAddress ?: "",
                    sessionId = sessionId,
                    statusMessage = "Connected to ${request.deviceName}"
                )
            }

            // Keep TCP open for disconnect signalling only — never time out the session
            socket.soTimeout = 0
            scope.launch {
                val buf = ByteArray(64)
                try {
                    while (running.get() && !socket.isClosed && _isConnected.value) {
                        try {
                            val n = socket.getInputStream().read(buf)
                            if (n < 0) break // peer closed
                            // ignore payload (heartbeats / future control frames)
                        } catch (e: SocketTimeoutException) {
                            // should not happen with soTimeout=0; ignore if set
                        } catch (e: java.net.SocketException) {
                            break
                        }
                    }
                } catch (_: Exception) {
                }
                if (_isConnected.value) {
                    android.util.Log.i(TAG, "TCP peer closed — ending session")
                    onDisconnect(sendRelease = true)
                }
            }
        } catch (e: Exception) {
            try { socket.close() } catch (_: Exception) {}
            updateDiagnostics { it.copy(statusMessage = "Handshake failed: ${e.message}") }
        }
    }

    // --- Controller API ---

    override suspend fun connect(targetId: String): Result<Unit> {
        val parsed = PairingParser.parseWifi(targetId)
        if (parsed.isFailure) {
            return Result.failure(parsed.exceptionOrNull() ?: IllegalArgumentException("Invalid pairing string"))
        }
        val p = parsed.getOrThrow()
        android.util.Log.i(TAG, "Pairing string parsed host=${p.host} port=${p.tcpPort} token=****")
        return connectToReceiver(p.host, p.tcpPort, p.token)
    }

    suspend fun connectToReceiver(
        host: String,
        tcpPort: Int,
        pairingToken: Int,
        deviceName: String = Build.MODEL
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (running.get()) disconnect()
        try {
            isReceiver = false
            expectedPairingToken = pairingToken
            val socket = Socket()
            socket.connect(InetSocketAddress(host, tcpPort), 8000)
            socket.soTimeout = 8000 // only for auth handshake
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            val req = SessionAuth.encodeAuthRequest(pairingToken, deviceName)
            output.write(req)
            output.flush()

            val respBuf = ByteArray(SessionAuth.AUTH_FRAME_SIZE)
            input.readFully(respBuf)
            val response = SessionAuth.decodeAuthResponse(respBuf)
                ?: return@withContext Result.failure(IllegalStateException("Invalid auth response"))

            if (!response.accepted) {
                socket.close()
                return@withContext Result.failure(IllegalStateException("Authentication rejected"))
            }

            sessionId = response.sessionId
            remoteUdpPort = response.udpPort
            remoteUdpAddress = InetAddress.getByName(host)
            clientSocket = socket
            socket.soTimeout = 0 // persistent session — never auto-timeout
            BinaryPacketProtocol.resetSequence()

            udpSocket = DatagramSocket()
            running.set(true)
            _isConnected.value = true
            packetsSent.set(0)

            updateDiagnostics {
                it.copy(
                    isConnected = true,
                    connectionType = "Wi-Fi TCP/UDP",
                    remoteDeviceName = host,
                    remoteAddress = host,
                    sessionId = sessionId,
                    localIp = NetworkUtils.getLocalIpAddress(context) ?: "",
                    tcpPort = tcpPort,
                    udpPort = remoteUdpPort,
                    statusMessage = "Connected to $host"
                )
            }

            // Monitor TCP for remote disconnect only (EOF / hard error)
            scope.launch {
                val buf = ByteArray(64)
                try {
                    while (running.get() && !socket.isClosed && _isConnected.value) {
                        try {
                            val n = socket.getInputStream().read(buf)
                            if (n < 0) break
                        } catch (e: SocketTimeoutException) {
                            // ignore
                        } catch (e: java.net.SocketException) {
                            break
                        }
                    }
                } catch (_: Exception) {
                }
                if (_isConnected.value) {
                    android.util.Log.i(TAG, "Controller TCP closed by peer")
                    onDisconnect(sendRelease = false)
                }
            }

            // Start 120 Hz sender
            sendJob = scope.launch { sendLoop() }
            heartbeatJob = scope.launch {
                // Lightweight TCP keepalive write every 2s so middleboxes / soTimeout never tear down
                while (running.get() && _isConnected.value) {
                    try {
                        clientSocket?.getOutputStream()?.write(byteArrayOf(0))
                        clientSocket?.getOutputStream()?.flush()
                    } catch (_: Exception) {
                        break
                    }
                    delay(2000)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            stopInternal()
            Result.failure(e)
        }
    }

    /**
     * Push latest gamepad state for transmission (latest-state semantics).
     */
    fun publishState(state: GamepadState) {
        latestState.set(state)
        hasPending.set(true)
    }

    override suspend fun sendState(state: GamepadState): Result<Unit> {
        publishState(state)
        return Result.success(Unit)
    }

    override fun receiveState(): Flow<GamepadState> = _incomingState

    private suspend fun sendLoop() {
        val socket = udpSocket ?: return
        val addr = remoteUdpAddress ?: return
        val port = remoteUdpPort
        rateWindowStart = System.currentTimeMillis()
        rateCount = 0

        while (running.get() && _isConnected.value && scope.isActive) {
            val start = System.nanoTime()
            if (hasPending.getAndSet(false) || true) {
                // Always send latest state at target rate (keeps receiver fresh)
                val state = latestState.get()
                val seq = BinaryPacketProtocol.nextSequence()
                val packet = BinaryPacketProtocol.encodeInputState(sessionId, seq, state)
                try {
                    val dp = DatagramPacket(packet, packet.size, addr, port)
                    socket.send(dp)
                    packetsSent.incrementAndGet()
                    rateCount++
                    val now = System.currentTimeMillis()
                    if (now - rateWindowStart >= 1000) {
                        currentHz = rateCount.toFloat()
                        rateCount = 0
                        rateWindowStart = now
                        updateDiagnostics {
                            it.copy(
                                packetsSent = packetsSent.get(),
                                updateRateHz = currentHz
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "UDP send failed: ${e.message}")
                    // Transient network glitches must not kill the session
                    delay(5)
                    if (!running.get() || !_isConnected.value) break
                }
            }
            val elapsed = (System.nanoTime() - start) / 1_000_000L
            val sleep = (FRAME_INTERVAL_MS - elapsed).coerceAtLeast(1)
            delay(sleep)
        }
    }

    private suspend fun udpReceiveLoop() {
        val socket = udpSocket ?: return
        val buf = ByteArray(BinaryPacketProtocol.PACKET_SIZE + 16)
        rateWindowStart = System.currentTimeMillis()
        rateCount = 0

        while (running.get() && scope.isActive) {
            try {
                val packet = DatagramPacket(buf, buf.size)
                socket.receive(packet)
                if (!_isConnected.value) continue
                // Optionally restrict source IP after first packet
                val data = packet.data.copyOf(packet.length)
                val decoded = BinaryPacketProtocol.decodePacket(data) ?: continue
                if (decoded.sessionId != sessionId) continue // reject foreign sessions

                when (decoded.type) {
                    BinaryPacketProtocol.PacketType.INPUT_STATE -> {
                        val seq = decoded.sequence
                        val prev = lastSeq.get()
                        if (prev >= 0 && seq > prev + 1) {
                            lostEstimate.addAndGet((seq - prev - 1).toLong())
                        }
                        if (seq > prev) lastSeq.set(seq)
                        packetsReceived.incrementAndGet()
                        lastPacketMs = decoded.timestampMs
                        rateCount++
                        decoded.state?.let { _incomingState.tryEmit(it) }
                    }
                    BinaryPacketProtocol.PacketType.RELEASE_ALL -> {
                        _incomingState.tryEmit(GamepadState.EMPTY)
                    }
                    else -> { /* ignore */ }
                }

                val now = System.currentTimeMillis()
                if (now - rateWindowStart >= 1000) {
                    currentHz = rateCount.toFloat()
                    rateCount = 0
                    rateWindowStart = now
                    updateDiagnostics {
                        it.copy(
                            packetsReceived = packetsReceived.get(),
                            packetsLostEstimate = lostEstimate.get(),
                            updateRateHz = currentHz,
                            lastPacketTimestampMs = lastPacketMs,
                            estimatedRttMs = null // one-way only in M2; RTT needs echo
                        )
                    }
                }
            } catch (_: SocketTimeoutException) {
                // normal poll
            } catch (e: Exception) {
                if (running.get()) {
                    delay(10)
                }
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            onDisconnect(sendRelease = !isReceiver)
            stopInternal()
        }
    }

    private fun onDisconnect(sendRelease: Boolean) {
        if (!_isConnected.value && !running.get()) return
        _isConnected.value = false
        if (sendRelease && !isReceiver) {
            try {
                val socket = udpSocket
                val addr = remoteUdpAddress
                if (socket != null && addr != null) {
                    val packet = BinaryPacketProtocol.encodeReleaseAll(
                        sessionId,
                        BinaryPacketProtocol.nextSequence()
                    )
                    socket.send(DatagramPacket(packet, packet.size, addr, remoteUdpPort))
                }
            } catch (_: Exception) {
            }
        }
        if (isReceiver) {
            // Notify local consumers that all buttons are released
            _incomingState.tryEmit(GamepadState.EMPTY)
        }
        updateDiagnostics {
            it.copy(
                isConnected = false,
                statusMessage = "Disconnected",
                updateRateHz = 0f
            )
        }
        // Keep server listening if we are still in receiver mode
        if (isReceiver && running.get()) {
            try { clientSocket?.close() } catch (_: Exception) {}
            clientSocket = null
            sessionId = 0
            // rotate pairing token for next connection
            expectedPairingToken = SessionAuth.generatePairingToken()
            _receiverState.value = ReceiverState.LISTENING
            updateDiagnostics {
                it.copy(
                    pairingTokenDisplay = formatToken(expectedPairingToken),
                    statusMessage = "Listening – ready for new connection"
                )
            }
        }
    }

    private fun stopInternal() {
        running.set(false)
        _isConnected.value = false
        _receiverState.value = ReceiverState.STOPPED
        acceptJob?.cancel()
        receiveJob?.cancel()
        sendJob?.cancel()
        heartbeatJob?.cancel()
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        try { udpSocket?.close() } catch (_: Exception) {}
        clientSocket = null
        serverSocket = null
        udpSocket = null
        sessionId = 0
    }

    fun shutdown() {
        running.set(false)
        stopInternal()
        scope.cancel()
    }

    // WiFiTransport stubs used by higher layers
    override suspend fun startHotspot(): Result<String> =
        Result.failure(UnsupportedOperationException("Use system hotspot settings"))

    override suspend fun stopHotspot() {}

    override suspend fun discoverPeers(): Result<List<String>> =
        Result.failure(UnsupportedOperationException("Use WiFiDirectManager"))

    private fun updateDiagnostics(transform: (ConnectionDiagnostics) -> ConnectionDiagnostics) {
        _diagnostics.value = transform(_diagnostics.value)
    }

    private fun formatToken(token: Int): String = PairingParser.formatToken(token)

    /** Expose current pairing token for QR / manual entry (receiver only). */
    fun currentPairingToken(): Int = expectedPairingToken

    fun currentSessionId(): Int = sessionId
}

/** Allocation-light holder for latest GamepadState. */
private class AtomicReferenceGamepad {
    @Volatile
    private var state: GamepadState = GamepadState.EMPTY
    fun set(s: GamepadState) { state = s }
    fun get(): GamepadState = state
}
