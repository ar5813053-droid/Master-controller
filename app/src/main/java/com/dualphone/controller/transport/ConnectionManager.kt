package com.dualphone.controller.transport

import android.content.Context
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.transport.bluetooth.BluetoothRfcommTransport
import com.dualphone.controller.transport.wifi.TcpUdpInputTransport
import com.dualphone.controller.transport.wifi.WiFiDirectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge

enum class TransportKind {
    NONE,
    WIFI_TCP_UDP,
    WIFI_DIRECT,
    BLUETOOTH_RFCOMM
}

/**
 * Unified connection manager — only one active transport session at a time.
 * Prevents competing transports from corrupting input state.
 */
class ConnectionManager(
    context: Context,
    val wifiTransport: TcpUdpInputTransport,
    val bluetoothTransport: BluetoothRfcommTransport,
    val wifiDirectManager: WiFiDirectManager
) {
    private val _activeTransport = MutableStateFlow(TransportKind.NONE)
    val activeTransport: StateFlow<TransportKind> = _activeTransport.asStateFlow()

    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> = _status.asStateFlow()

    val incomingState: Flow<GamepadState> = merge(
        wifiTransport.receiveState(),
        bluetoothTransport.receiveState()
    )

    suspend fun startWifiReceiver(): Result<Unit> {
        ensureExclusive(TransportKind.WIFI_TCP_UDP)
        val r = wifiTransport.startReceiver()
        if (r.isSuccess) {
            _activeTransport.value = TransportKind.WIFI_TCP_UDP
            _status.value = "Wi-Fi receiver listening"
        }
        return r
    }

    /**
     * Auto-detect Wi-Fi (IP:PORT:TOKEN) or Bluetooth (BT:MAC:TOKEN / MAC:TOKEN / TOKEN-only with bonded device).
     */
    suspend fun connectAuto(pairing: String): Result<Unit> {
        val t = pairing.trim()
        if (t.isEmpty()) return Result.failure(IllegalArgumentException("Empty pairing string"))
        val looksBt = t.startsWith("BT:", ignoreCase = true) ||
            PairingParser.looksLikeBluetooth(t)
        return if (looksBt) {
            val parsed = PairingParser.parseBluetooth(t)
            if (parsed.isFailure) {
                // Token-only: try bonded devices
                val tokenOnly = PairingParser.parseToken(t.removePrefix("BT:").trim())
                if (tokenOnly.isSuccess) {
                    connectBluetoothWithTokenOnly(tokenOnly.getOrThrow())
                } else {
                    Result.failure(parsed.exceptionOrNull()!!)
                }
            } else {
                val p = parsed.getOrThrow()
                connectBluetooth(p.address, p.token)
            }
        } else {
            connectWifi(t)
        }
    }

    suspend fun connectBluetoothWithTokenOnly(token: Int): Result<Unit> {
        bluetoothTransport.refreshBondedDevices()
        val devices = bluetoothTransport.bondedDevices.value
        if (devices.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "No bonded Bluetooth devices. Pair both phones in system Settings → Bluetooth first, then enter TOKEN or BT:MAC:TOKEN"
                )
            )
        }
        var last: Result<Unit> = Result.failure(IllegalStateException("No device accepted token"))
        for (d in devices) {
            last = connectBluetooth(d.address, token)
            if (last.isSuccess) return last
        }
        return Result.failure(
            IllegalArgumentException(
                "Token rejected by ${devices.size} bonded device(s). Check Gaming Mode BT token matches. Last: ${last.exceptionOrNull()?.message}"
            )
        )
    }

    suspend fun connectWifi(pairing: String): Result<Unit> {
        ensureExclusive(TransportKind.WIFI_TCP_UDP)
        val r = wifiTransport.connect(pairing)
        if (r.isSuccess) {
            _activeTransport.value = TransportKind.WIFI_TCP_UDP
            _status.value = "Wi-Fi connected"
        }
        return r
    }

    suspend fun startBluetoothReceiver(): Result<Unit> {
        ensureExclusive(TransportKind.BLUETOOTH_RFCOMM)
        val r = bluetoothTransport.startAdvertising(BluetoothRfcommTransport.SERVICE_NAME)
        if (r.isSuccess) {
            _activeTransport.value = TransportKind.BLUETOOTH_RFCOMM
            _status.value = "Bluetooth listening"
        }
        return r
    }

    suspend fun connectBluetooth(address: String, token: Int): Result<Unit> {
        ensureExclusive(TransportKind.BLUETOOTH_RFCOMM)
        val r = bluetoothTransport.connectToDevice(address, token)
        if (r.isSuccess) {
            _activeTransport.value = TransportKind.BLUETOOTH_RFCOMM
            _status.value = "Bluetooth connected"
        }
        return r
    }

    fun publishState(state: GamepadState) {
        when (_activeTransport.value) {
            TransportKind.WIFI_TCP_UDP, TransportKind.WIFI_DIRECT ->
                wifiTransport.publishState(state)
            TransportKind.BLUETOOTH_RFCOMM ->
                bluetoothTransport.publishState(state)
            TransportKind.NONE -> { /* local only */ }
        }
    }

    suspend fun disconnect() {
        when (_activeTransport.value) {
            TransportKind.WIFI_TCP_UDP, TransportKind.WIFI_DIRECT ->
                wifiTransport.disconnect()
            TransportKind.BLUETOOTH_RFCOMM ->
                bluetoothTransport.disconnect()
            TransportKind.NONE -> {}
        }
        _activeTransport.value = TransportKind.NONE
        _status.value = "Disconnected"
    }

    fun diagnostics(): StateFlow<ConnectionDiagnostics> {
        return when (_activeTransport.value) {
            TransportKind.BLUETOOTH_RFCOMM -> bluetoothTransport.diagnostics
            else -> wifiTransport.diagnostics
        }
    }

    private suspend fun ensureExclusive(next: TransportKind) {
        val current = _activeTransport.value
        if (current != TransportKind.NONE && current != next) {
            disconnect()
        }
    }

    fun shutdown() {
        wifiTransport.shutdown()
        bluetoothTransport.shutdown()
        wifiDirectManager.release()
        _activeTransport.value = TransportKind.NONE
    }
}
