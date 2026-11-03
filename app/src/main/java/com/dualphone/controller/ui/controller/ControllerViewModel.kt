package com.dualphone.controller.ui.controller

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dualphone.controller.DualPhoneApp
import com.dualphone.controller.core.AppSettings
import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.core.ControllerLayout
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.input.GamepadInputManager
import com.dualphone.controller.transport.TransportKind
import com.dualphone.controller.transport.bluetooth.BluetoothRfcommTransport
import com.dualphone.controller.transport.wifi.TcpUdpInputTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ControllerConnectionUi(
    val connected: Boolean = false,
    val statusMessage: String = "Local only",
    val packetsSent: Long = 0,
    val updateRateHz: Float = 0f,
    val estimatedRttMs: Float? = null,
    val transportName: String = "None",
    val transportKind: TransportKind = TransportKind.NONE,
    val bondedDevices: List<BluetoothRfcommTransport.BtDeviceInfo> = emptyList()
)

class ControllerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DualPhoneApp
    private val settingsRepo = app.settingsRepository
    private val layoutRepo = app.layoutRepository
    private val connectionManager = app.connectionManager
    private val wifiTransport = app.wifiTransport
    private val btTransport = app.bluetoothTransport

    private val inputManager = GamepadInputManager(
        deadzone = { currentSettings.joystickDeadzone },
        sensitivity = { currentSettings.joystickSensitivity }
    )

    private var currentSettings = AppSettings()
    private var streamJob: Job? = null

    val gamepadState: StateFlow<GamepadState> = inputManager.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, GamepadState.EMPTY)

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _connectionUi = MutableStateFlow(ControllerConnectionUi())
    val connectionUi: StateFlow<ControllerConnectionUi> = _connectionUi.asStateFlow()

    private val _connectError = MutableStateFlow<String?>(null)
    val connectError: StateFlow<String?> = _connectError.asStateFlow()

    private var _layout: ControllerLayout? = null
    val layout: ControllerLayout
        get() = _layout ?: layoutRepo.createDefaultLayout()

    init {
        viewModelScope.launch {
            settingsRepo.settings.collect { currentSettings = it }
        }
        viewModelScope.launch {
            layoutRepo.ensureDefaultExists()
            _layout = layoutRepo.getLayout("default") ?: layoutRepo.createDefaultLayout()
        }
        viewModelScope.launch {
            wifiTransport.diagnostics.collect { d ->
                if (connectionManager.activeTransport.value != TransportKind.BLUETOOTH_RFCOMM) {
                    mergeDiag(d, TransportKind.WIFI_TCP_UDP)
                }
            }
        }
        viewModelScope.launch {
            btTransport.diagnostics.collect { d ->
                if (connectionManager.activeTransport.value == TransportKind.BLUETOOTH_RFCOMM) {
                    mergeDiag(d, TransportKind.BLUETOOTH_RFCOMM)
                }
            }
        }
        viewModelScope.launch {
            btTransport.bondedDevices.collect { list ->
                _connectionUi.value = _connectionUi.value.copy(bondedDevices = list)
            }
        }
        streamJob = viewModelScope.launch {
            inputManager.state.collect { state ->
                if (_connectionUi.value.connected) {
                    connectionManager.publishState(state)
                }
            }
        }
        refreshBluetoothDevices()
    }

    private fun mergeDiag(d: com.dualphone.controller.transport.ConnectionDiagnostics, kind: TransportKind) {
        _connectionUi.value = _connectionUi.value.copy(
            connected = d.isConnected,
            statusMessage = d.statusMessage,
            packetsSent = d.packetsSent,
            updateRateHz = d.updateRateHz,
            estimatedRttMs = d.estimatedRttMs,
            transportName = d.connectionType,
            transportKind = if (d.isConnected) kind else connectionManager.activeTransport.value
        )
    }

    fun refreshBluetoothDevices() {
        btTransport.refreshBondedDevices()
    }

    /**
     * Wi-Fi pairing: "IP:PORT:TOKEN"
     * Bluetooth: "BT:ADDRESS:TOKEN"
     */
    fun connect(pairing: String) {
        viewModelScope.launch {
            _connectError.value = null
            val trimmed = pairing.trim()
            if (trimmed.startsWith("BT:", ignoreCase = true) || looksLikeBtAddress(trimmed)) {
                connectBluetooth(trimmed)
            } else {
                connectWifi(trimmed)
            }
        }
    }

    private fun looksLikeBtAddress(s: String): Boolean {
        val parts = s.split(":")
        // MAC has 6 hex groups, or ADDRESS:TOKEN with MAC-style address
        return parts.size >= 7 || (parts.size >= 2 && parts[0].contains("-"))
    }

    private suspend fun connectWifi(pairing: String) {
        val result = connectionManager.connectWifi(pairing)
        if (result.isFailure) {
            _connectError.value = result.exceptionOrNull()?.message ?: "Wi-Fi connection failed"
        }
    }

    private suspend fun connectBluetooth(pairing: String) {
        val cleaned = pairing.removePrefix("BT:").removePrefix("bt:")
        // Formats: AA:BB:CC:DD:EE:FF:TOKEN  or  AA:BB:CC:DD:EE:FF with separate token field
        val parts = cleaned.split(":")
        if (parts.size < 7) {
            // ADDRESS might use dashes: AA-BB-CC-DD-EE-FF:TOKEN
            val dashParts = cleaned.split(":")
            if (dashParts.size == 2 && dashParts[0].count { it == '-' } == 5) {
                val address = dashParts[0].replace('-', ':')
                val token = dashParts[1].toIntOrNull()
                    ?: dashParts[1].toLongOrNull(16)?.toInt()
                if (token != null) {
                    val r = connectionManager.connectBluetooth(address, token)
                    if (r.isFailure) _connectError.value = r.exceptionOrNull()?.message
                    return
                }
            }
            _connectError.value = "BT format: AA:BB:CC:DD:EE:FF:TOKEN"
            return
        }
        val address = parts.take(6).joinToString(":")
        val tokenStr = parts[6]
        val token = tokenStr.toIntOrNull()
            ?: tokenStr.toLongOrNull(16)?.toInt()
            ?: run {
                _connectError.value = "Invalid BT token"
                return
            }
        val r = connectionManager.connectBluetooth(address, token)
        if (r.isFailure) {
            _connectError.value = r.exceptionOrNull()?.message ?: "Bluetooth connection failed"
        }
    }

    fun connectBluetoothDevice(address: String, tokenHex: String) {
        viewModelScope.launch {
            _connectError.value = null
            val token = tokenHex.toIntOrNull()
                ?: tokenHex.toLongOrNull(16)?.toInt()
                ?: run {
                    _connectError.value = "Invalid token"
                    return@launch
                }
            val r = connectionManager.connectBluetooth(address, token)
            if (r.isFailure) {
                _connectError.value = r.exceptionOrNull()?.message ?: "BT failed"
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            connectionManager.disconnect()
            inputManager.releaseAll()
        }
    }

    fun onLeftStickDown(pointerId: Int) = inputManager.leftStickDown(pointerId)
    fun onLeftStickMove(pointerId: Int, x: Float, y: Float) = inputManager.leftStickMove(pointerId, x, y)
    fun onLeftStickUp(pointerId: Int) = inputManager.leftStickUp(pointerId)

    fun onRightStickDown(pointerId: Int) = inputManager.rightStickDown(pointerId)
    fun onRightStickMove(pointerId: Int, x: Float, y: Float) = inputManager.rightStickMove(pointerId, x, y)
    fun onRightStickUp(pointerId: Int) = inputManager.rightStickUp(pointerId)

    fun onButtonDown(button: ControllerButton, pointerId: Int) {
        inputManager.buttonDown(button, pointerId)
        if (currentSettings.hapticEnabled) vibrate()
    }

    fun onButtonUp(button: ControllerButton, pointerId: Int) {
        inputManager.buttonUp(button, pointerId)
    }

    fun onPointerCancelled(pointerId: Int) = inputManager.pointerCancelled(pointerId)

    fun releaseAll() = inputManager.releaseAll()

    private fun vibrate() {
        val ctx = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        inputManager.releaseAll()
    }
}
