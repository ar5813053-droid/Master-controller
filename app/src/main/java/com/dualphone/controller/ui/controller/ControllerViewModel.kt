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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ControllerViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = (application as DualPhoneApp).settingsRepository
    private val layoutRepo = (application as DualPhoneApp).layoutRepository

    private val inputManager = GamepadInputManager(
        deadzone = { currentSettings.joystickDeadzone },
        sensitivity = { currentSettings.joystickSensitivity }
    )

    private var currentSettings = AppSettings()

    val gamepadState: StateFlow<GamepadState> = inputManager.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, GamepadState.EMPTY)

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private var _layout: ControllerLayout? = null
    val layout: ControllerLayout
        get() = _layout ?: layoutRepo.createDefaultLayout()

    init {
        viewModelScope.launch {
            settingsRepo.settings.collect { currentSettings = it }
        }
        viewModelScope.launch {
            layoutRepo.ensureDefaultExists()
            val activeId = settingsRepo.activeLayoutId
            // Load default for now; full load in editor
            _layout = layoutRepo.getLayout("default") ?: layoutRepo.createDefaultLayout()
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
        if (currentSettings.hapticEnabled) {
            vibrate()
        }
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
        inputManager.releaseAll()
    }
}
