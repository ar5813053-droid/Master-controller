package com.dualphone.controller.ui.gaming

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dualphone.controller.DualPhoneApp
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.transport.ConnectionDiagnostics
import com.dualphone.controller.transport.wifi.TcpUdpInputTransport
import com.dualphone.controller.transport.wifi.WiFiDirectManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GamingUiState(
    val isListening: Boolean = false,
    val diagnostics: ConnectionDiagnostics = ConnectionDiagnostics(),
    val lastInput: GamepadState = GamepadState.EMPTY,
    val qrBitmap: Bitmap? = null,
    val pairingString: String = "",
    val error: String? = null,
    val wifiDirectAvailable: Boolean = false,
    val wifiDirectStatus: String = ""
)

class GamingViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DualPhoneApp
    private val transport: TcpUdpInputTransport = app.transport
    private val wifiDirect: WiFiDirectManager = app.wifiDirectManager

    private val _uiState = MutableStateFlow(GamingUiState())
    val uiState: StateFlow<GamingUiState> = _uiState.asStateFlow()

    val diagnostics = transport.diagnostics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionDiagnostics())

    init {
        wifiDirect.initialize()
        viewModelScope.launch {
            transport.diagnostics.collect { diag ->
                _uiState.value = _uiState.value.copy(
                    diagnostics = diag,
                    isListening = diag.statusMessage.contains("Listening") || diag.isConnected,
                    pairingString = buildPairingString(diag)
                )
                if (diag.localIp.isNotBlank() && diag.pairingTokenDisplay.isNotBlank()) {
                    refreshQr(diag)
                }
            }
        }
        viewModelScope.launch {
            transport.receiveState().collect { state ->
                _uiState.value = _uiState.value.copy(lastInput = state)
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
    }

    fun startReceiver() {
        viewModelScope.launch {
            val result = transport.startReceiver(
                TcpUdpInputTransport.DEFAULT_TCP_PORT,
                TcpUdpInputTransport.DEFAULT_UDP_PORT
            )
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Failed to start receiver"
                )
            } else {
                _uiState.value = _uiState.value.copy(error = null, isListening = true)
            }
        }
    }

    fun stopReceiver() {
        viewModelScope.launch {
            transport.disconnect()
            _uiState.value = _uiState.value.copy(isListening = false, lastInput = GamepadState.EMPTY)
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
        // Format consumed by controller: host:tcpPort:tokenHex
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

    override fun onCleared() {
        // Do not shut down global transport here – user may navigate away briefly
        super.onCleared()
    }
}
