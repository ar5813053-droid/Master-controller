package com.dualphone.controller.transport.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.transport.ConnectionDiagnostics
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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * App-to-app Bluetooth RFCOMM transport.
 *
 * Service UUID is fixed and documented for dual-phone pairing.
 * Binary packets share the same BinaryPacketProtocol as Wi-Fi.
 * Not Bluetooth HID — both devices must run DualPhone Controller.
 */
class BluetoothRfcommTransport(
    private val context: Context
) : InputTransport, BluetoothTransport {

    companion object {
        /** Well-known UUID for DualPhone Controller RFCOMM service. */
        val SERVICE_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        const val SERVICE_NAME = "DualPhoneController"
        private const val TARGET_HZ = 60 // BT typically lower sustained rate than Wi-Fi UDP
        private const val FRAME_INTERVAL_MS = 1000L / TARGET_HZ
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: Flow<Boolean> = _isConnected

    private val _diagnostics = MutableStateFlow(ConnectionDiagnostics(connectionType = "Bluetooth RFCOMM"))
    val diagnostics: StateFlow<ConnectionDiagnostics> = _diagnostics.asStateFlow()

    private val _incomingState = MutableSharedFlow<GamepadState>(
        extraBufferCapacity = 4,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    private val _bondedDevices = MutableStateFlow<List<BtDeviceInfo>>(emptyList())
    val bondedDevices: StateFlow<List<BtDeviceInfo>> = _bondedDevices.asStateFlow()

    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var acceptJob: Job? = null
    private var receiveJob: Job? = null
    private var sendJob: Job? = null

    private val running = AtomicBoolean(false)
    private var isReceiver = false
    private var sessionId: Int = 0
    private var expectedPairingToken: Int = 0
    private val packetsSent = AtomicLong(0)
    private val packetsReceived = AtomicLong(0)
    private var rateWindowStart = 0L
    private var rateCount = 0

    @Volatile private var latestState: GamepadState = GamepadState.EMPTY

    data class BtDeviceInfo(val name: String, val address: String, val bonded: Boolean)

    fun isBluetoothAvailable(): Boolean = adapter != null && adapter.isEnabled

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        val a = adapter ?: return
        try {
            val list = a.bondedDevices?.map {
                BtDeviceInfo(it.name ?: "Unknown", it.address, true)
            } ?: emptyList()
            _bondedDevices.value = list
        } catch (_: SecurityException) {
            _bondedDevices.value = emptyList()
        }
    }

    // --- Receiver (Gaming Mode) ---

    @SuppressLint("MissingPermission")
    override suspend fun startAdvertising(serviceName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (adapter == null || !adapter.isEnabled) {
                return@withContext Result.failure(IllegalStateException("Bluetooth disabled or unavailable"))
            }
            if (running.get()) stopInternal()
            try {
                isReceiver = true
                expectedPairingToken = SessionAuth.generatePairingToken()
                sessionId = 0
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                    serviceName.ifBlank { SERVICE_NAME },
                    SERVICE_UUID
                )
                running.set(true)
                updateDiag {
                    it.copy(
                        isConnected = false,
                        connectionType = "Bluetooth RFCOMM",
                        pairingTokenDisplay = String.format("%08X", expectedPairingToken),
                        statusMessage = "BT listening – pair from controller"
                    )
                }
                acceptJob = scope.launch { acceptLoop() }
                Result.success(Unit)
            } catch (e: Exception) {
                stopInternal()
                Result.failure(e)
            }
        }

    override suspend fun stopAdvertising() {
        withContext(Dispatchers.IO) { stopInternal() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun acceptLoop() {
        while (running.get() && scope.isActive) {
            try {
                val socket = serverSocket?.accept() ?: break
                if (_isConnected.value) {
                    try { socket.close() } catch (_: Exception) {}
                    continue
                }
                handleClient(socket)
            } catch (e: IOException) {
                if (running.get()) delay(200)
            }
        }
    }

    private suspend fun handleClient(socket: BluetoothSocket) {
        try {
            val input = socket.inputStream
            val output = socket.outputStream
            // Auth frame
            val authBuf = ByteArray(SessionAuth.AUTH_FRAME_SIZE)
            readFully(input, authBuf)
            val request = SessionAuth.decodeAuthRequest(authBuf)
            if (request == null || request.pairingToken != expectedPairingToken) {
                output.write(SessionAuth.encodeAuthResponse(0, false, 0))
                output.flush()
                socket.close()
                updateDiag { it.copy(statusMessage = "BT auth rejected") }
                return
            }
            sessionId = SessionAuth.generateSessionId()
            output.write(SessionAuth.encodeAuthResponse(sessionId, true, 0))
            output.flush()

            clientSocket = socket
            _isConnected.value = true
            packetsReceived.set(0)
            BinaryPacketProtocol.resetSequence()
            updateDiag {
                it.copy(
                    isConnected = true,
                    remoteDeviceName = request.deviceName,
                    remoteAddress = socket.remoteDevice?.address ?: "",
                    sessionId = sessionId,
                    statusMessage = "BT connected to ${request.deviceName}"
                )
            }
            receiveJob = scope.launch { receiveLoop(input) }
            // Watch for disconnect
            scope.launch {
                try {
                    while (running.get() && socket.isConnected) delay(500)
                } finally {
                    onDisconnect(sendRelease = false)
                }
            }
        } catch (e: Exception) {
            try { socket.close() } catch (_: Exception) {}
            updateDiag { it.copy(statusMessage = "BT handshake failed: ${e.message}") }
        }
    }

    // --- Controller ---

    override suspend fun connect(targetId: String): Result<Unit> {
        // targetId = "BT:ADDRESS:TOKEN" or "ADDRESS:TOKEN"
        val cleaned = targetId.removePrefix("BT:")
        val parts = cleaned.split(":")
        if (parts.size < 2) {
            return Result.failure(IllegalArgumentException("Expected ADDRESS:TOKEN"))
        }
        val address = parts[0]
        val token = parts[1].toIntOrNull()
            ?: parts[1].toLongOrNull(16)?.toInt()
            ?: return Result.failure(IllegalArgumentException("Invalid token"))
        return connectToDevice(address, token)
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(
        address: String,
        pairingToken: Int,
        deviceName: String = Build.MODEL
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (adapter == null || !adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth disabled"))
        }
        if (running.get()) stopInternal()
        try {
            isReceiver = false
            expectedPairingToken = pairingToken
            val device: BluetoothDevice = adapter.getRemoteDevice(address)
            val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
            // Cancel discovery to speed connection
            try { adapter.cancelDiscovery() } catch (_: Exception) {}
            socket.connect()
            val output = socket.outputStream
            val input = socket.inputStream
            output.write(SessionAuth.encodeAuthRequest(pairingToken, deviceName))
            output.flush()
            val respBuf = ByteArray(SessionAuth.AUTH_FRAME_SIZE)
            readFully(input, respBuf)
            val response = SessionAuth.decodeAuthResponse(respBuf)
                ?: return@withContext Result.failure(IllegalStateException("Invalid BT auth response"))
            if (!response.accepted) {
                socket.close()
                return@withContext Result.failure(IllegalStateException("BT authentication rejected"))
            }
            sessionId = response.sessionId
            clientSocket = socket
            running.set(true)
            _isConnected.value = true
            packetsSent.set(0)
            BinaryPacketProtocol.resetSequence()
            updateDiag {
                it.copy(
                    isConnected = true,
                    connectionType = "Bluetooth RFCOMM",
                    remoteDeviceName = device.name ?: address,
                    remoteAddress = address,
                    sessionId = sessionId,
                    statusMessage = "BT connected to ${device.name ?: address}"
                )
            }
            receiveJob = scope.launch { receiveLoop(input) }
            sendJob = scope.launch { sendLoop(output) }
            Result.success(Unit)
        } catch (e: Exception) {
            stopInternal()
            Result.failure(e)
        }
    }

    override suspend fun pair(deviceAddress: String): Result<Unit> {
        // Pairing is system-managed; we only connect after bond
        refreshBondedDevices()
        return Result.success(Unit)
    }

    fun publishState(state: GamepadState) {
        latestState = state
    }

    override suspend fun sendState(state: GamepadState): Result<Unit> {
        publishState(state)
        return Result.success(Unit)
    }

    override fun receiveState(): Flow<GamepadState> = _incomingState

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            onDisconnect(sendRelease = !isReceiver)
            stopInternal()
        }
    }

    private suspend fun sendLoop(output: OutputStream) {
        rateWindowStart = System.currentTimeMillis()
        rateCount = 0
        while (running.get() && _isConnected.value && scope.isActive) {
            val start = System.nanoTime()
            try {
                val seq = BinaryPacketProtocol.nextSequence()
                val packet = BinaryPacketProtocol.encodeInputState(sessionId, seq, latestState)
                // Length-prefixed for stream framing
                output.write(packet.size and 0xFF)
                output.write((packet.size shr 8) and 0xFF)
                output.write(packet)
                output.flush()
                packetsSent.incrementAndGet()
                rateCount++
                val now = System.currentTimeMillis()
                if (now - rateWindowStart >= 1000) {
                    updateDiag {
                        it.copy(packetsSent = packetsSent.get(), updateRateHz = rateCount.toFloat())
                    }
                    rateCount = 0
                    rateWindowStart = now
                }
            } catch (_: Exception) {
                onDisconnect(sendRelease = true)
                break
            }
            val elapsed = (System.nanoTime() - start) / 1_000_000L
            delay((FRAME_INTERVAL_MS - elapsed).coerceAtLeast(1))
        }
    }

    private suspend fun receiveLoop(input: InputStream) {
        rateWindowStart = System.currentTimeMillis()
        rateCount = 0
        val lenBuf = ByteArray(2)
        while (running.get() && scope.isActive) {
            try {
                readFully(input, lenBuf)
                val len = (lenBuf[0].toInt() and 0xFF) or ((lenBuf[1].toInt() and 0xFF) shl 8)
                if (len <= 0 || len > BinaryPacketProtocol.PACKET_SIZE + 8) {
                    // malformed — drop connection
                    onDisconnect(sendRelease = false)
                    break
                }
                val data = ByteArray(len)
                readFully(input, data)
                val decoded = BinaryPacketProtocol.decodePacket(data) ?: continue
                if (decoded.sessionId != sessionId && sessionId != 0) continue
                when (decoded.type) {
                    BinaryPacketProtocol.PacketType.INPUT_STATE -> {
                        packetsReceived.incrementAndGet()
                        rateCount++
                        decoded.state?.let { _incomingState.tryEmit(it) }
                    }
                    BinaryPacketProtocol.PacketType.RELEASE_ALL -> {
                        _incomingState.tryEmit(GamepadState.EMPTY)
                    }
                    else -> {}
                }
                val now = System.currentTimeMillis()
                if (now - rateWindowStart >= 1000) {
                    updateDiag {
                        it.copy(
                            packetsReceived = packetsReceived.get(),
                            updateRateHz = rateCount.toFloat(),
                            lastPacketTimestampMs = decoded.timestampMs,
                            estimatedRttMs = null
                        )
                    }
                    rateCount = 0
                    rateWindowStart = now
                }
            } catch (_: Exception) {
                if (running.get()) onDisconnect(sendRelease = false)
                break
            }
        }
    }

    private fun onDisconnect(sendRelease: Boolean) {
        if (!_isConnected.value && !running.get()) return
        _isConnected.value = false
        if (sendRelease && !isReceiver) {
            try {
                val out = clientSocket?.outputStream
                if (out != null) {
                    val packet = BinaryPacketProtocol.encodeReleaseAll(
                        sessionId, BinaryPacketProtocol.nextSequence()
                    )
                    out.write(packet.size and 0xFF)
                    out.write((packet.size shr 8) and 0xFF)
                    out.write(packet)
                    out.flush()
                }
            } catch (_: Exception) {}
        }
        if (isReceiver) _incomingState.tryEmit(GamepadState.EMPTY)
        updateDiag {
            it.copy(isConnected = false, statusMessage = "BT disconnected", updateRateHz = 0f)
        }
        if (isReceiver && running.get()) {
            try { clientSocket?.close() } catch (_: Exception) {}
            clientSocket = null
            sessionId = 0
            expectedPairingToken = SessionAuth.generatePairingToken()
            updateDiag {
                it.copy(
                    pairingTokenDisplay = String.format("%08X", expectedPairingToken),
                    statusMessage = "BT listening – ready for new connection"
                )
            }
        }
    }

    private fun stopInternal() {
        running.set(false)
        _isConnected.value = false
        acceptJob?.cancel()
        receiveJob?.cancel()
        sendJob?.cancel()
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        clientSocket = null
        serverSocket = null
        sessionId = 0
    }

    fun shutdown() {
        running.set(false)
        stopInternal()
        scope.cancel()
    }

    fun currentPairingToken(): Int = expectedPairingToken

    private fun updateDiag(t: (ConnectionDiagnostics) -> ConnectionDiagnostics) {
        _diagnostics.value = t(_diagnostics.value)
    }

    private fun readFully(input: InputStream, buf: ByteArray) {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) throw IOException("EOF")
            off += n
        }
    }
}
