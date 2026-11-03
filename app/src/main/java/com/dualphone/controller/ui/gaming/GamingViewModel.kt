package com.dualphone.controller.ui.gaming

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dualphone.controller.DualPhoneApp
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.transport.ConnectionDiagnostics
import com.dualphone.controller.transport.TransportKind
import com.dualphone.controller.transport.ReceiverState
import com.dualphone.controller.transport.wifi.TcpUdpInputTransport
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GamingUiState(
    val isListening: Boolean = false,
    val diagnostics: ConnectionDiagnostics = ConnectionDiagnostics(),
    val lastInput: GamepadState = GamepadState.EMPTY,
    val qrBitmap: Bitmap? = null,
    val pairingString: String = "",
    val error: String? = null,
    val wifiDirectAvailable: Boolean = false,
    val wifiDirectStatus: String = "",
    val bluetoothAvailable: Boolean = false,
    val bluetoothToken: String = "",
    val activeTransport: TransportKind = TransportKind.NONE,
    val receiverState: ReceiverState = ReceiverState.STOPPED
)

class GamingViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DualPhoneApp
    private val connectionManager = app.connectionManager
    private val wifiTransport = app.wifiTransport
    private val btTransport = app.bluetoothTransport
    private val wifiDirect = app.wifiDirectManager
    private val backend = app.inputBackend

    private val _uiState = MutableStateFlow(GamingUiState())
    val uiState: StateFlow<GamingUiState> = _uiState.asStateFlow()

    init {
        wifiDirect.initialize()
        backend.setActive(true)
        _uiState.value = _uiState.value.copy(
            bluetoothAvailable = btTransport.isBluetoothAvailable()
        )

        viewModelScope.launch {
            wifiTransport.diagnostics.collect { diag ->
                if (connectionManager.activeTransport.value != TransportKind.BLUETOOTH_RFCOMM) {
                    applyDiag(diag)
                }
            }
        }
        viewModelScope.launch {
            btTransport.diagnostics.collect { diag ->
                if (connectionManager.activeTransport.value == TransportKind.BLUETOOTH_RFCOMM) {
                    applyDiag(diag)
                    _uiState.value = _uiState.value.copy(
                        bluetoothToken = diag.pairingTokenDisplay
                    )
                }
            }
        }
        viewModelScope.launch {
            connectionManager.incomingState.collect { state ->
                _uiState.value = _uiState.value.copy(lastInput = state)
                backend.inject(state)
            }
        }
        viewModelScope.launch {
            connectionManager.activeTransport.collect { kind ->
                _uiState.value = _uiState.value.copy(activeTransport = kind)
            }
        }
        viewModelScope.launch {
            wifiTransport.receiverState.collect { st ->
                if (connectionManager.activeTransport.value != TransportKind.BLUETOOTH_RFCOMM) {
                    _uiState.value = _uiState.value.copy(receiverState = st)
                }
            }
        }
        viewModelScope.launch {
            wifiDirect.available.collect { avail ->
                _uiState.value = _uiState.value.copy(wifiDirectAvailable = avail)
            }
        }
        viewModelScope.launch {
            wifiDirect.statusMessage.collect { msg ->
                _uiState.value = _uiState.value.copy(wifiDirectStatus = msg)
            }
        }
        viewModelScope.launch {
            backend.latest.collect { state ->
                // mirror for UI
                if (connectionManager.activeTransport.value != TransportKind.NONE) {
                    _uiState.value = _uiState.value.copy(lastInput = state)
                }
            }
        }
    }

    private fun applyDiag(diag: ConnectionDiagnostics) {
        val listening = diag.isConnected ||
            diag.statusMessage.contains("Listening", ignoreCase = true)
        _uiState.value = _uiState.value.copy(
            diagnostics = diag,
            isListening = listening && _uiState.value.receiverState != ReceiverState.STOPPED,
            pairingString = buildPairingString(diag)
        )
        if (diag.localIp.isNotBlank() && diag.pairingTokenDisplay.isNotBlank()) {
            refreshQr(diag)
        }
    }

    fun startWifiReceiver() {
        viewModelScope.launch {
            val result = connectionManager.startWifiReceiver()
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Failed to start Wi-Fi receiver"
                )
            } else {
                _uiState.value = _uiState.value.copy(error = null, isListening = true)
            }
        }
    }

    fun startBluetoothReceiver() {
        viewModelScope.launch {
            val result = connectionManager.startBluetoothReceiver()
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Failed to start Bluetooth"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = null,
                    isListening = true,
                    bluetoothToken = btTransport.diagnostics.value.pairingTokenDisplay
                )
            }
        }
    }

    fun stopReceiver() {
        viewModelScope.launch {
            connectionManager.disconnect()
            backend.releaseAll()
            _uiState.value = _uiState.value.copy(
                isListening = false,
                lastInput = GamepadState.EMPTY
            )
        }
    }

    fun startWifiDirectDiscovery() {
        if (!wifiDirect.initialize()) {
            _uiState.value = _uiState.value.copy(error = "Wi-Fi Direct unavailable")
            return
        }
        wifiDirect.discoverPeers()
    }

    private fun buildPairingString(diag: ConnectionDiagnostics): String {
        if (diag.localIp.isBlank() || diag.pairingTokenDisplay.isBlank()) return ""
        return "${diag.localIp}:${diag.tcpPort}:${diag.pairingTokenDisplay}"
    }

    private fun refreshQr(diag: ConnectionDiagnostics) {
        val content = buildPairingString(diag)
        if (content.isBlank()) return
        try {
            val size = 512
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            _uiState.value = _uiState.value.copy(qrBitmap = bmp, pairingString = content)
        } catch (_: Exception) {
        }
    }
}
