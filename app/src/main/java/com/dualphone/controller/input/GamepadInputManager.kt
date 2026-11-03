package com.dualphone.controller.input

import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.input.buttons.ButtonStateTracker
import com.dualphone.controller.input.joystick.JoystickState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Central input manager that owns joystick and button state outside Compose.
 * Publishes a single GamepadState via StateFlow for UI observation and future transport.
 */
class GamepadInputManager(
    private val deadzone: () -> Float = { 0.12f },
    private val sensitivity: () -> Float = { 1.0f }
) {
    private val leftStick = JoystickState()
    private val rightStick = JoystickState()
    private val buttons = ButtonStateTracker()

    private val _state = MutableStateFlow(GamepadState.EMPTY)
    val state: StateFlow<GamepadState> = _state.asStateFlow()

    // --- Left stick ---
    fun leftStickDown(pointerId: Int) = leftStick.startTracking(pointerId)
    fun leftStickMove(pointerId: Int, normalizedX: Float, normalizedY: Float) {
        if (leftStick.matchesPointer(pointerId)) {
            leftStick.updateRaw(normalizedX, normalizedY)
            publish()
        }
    }
    fun leftStickUp(pointerId: Int) {
        if (leftStick.matchesPointer(pointerId)) {
            leftStick.stopTracking()
            publish()
        }
    }

    // --- Right stick ---
    fun rightStickDown(pointerId: Int) = rightStick.startTracking(pointerId)
    fun rightStickMove(pointerId: Int, normalizedX: Float, normalizedY: Float) {
        if (rightStick.matchesPointer(pointerId)) {
            rightStick.updateRaw(normalizedX, normalizedY)
            publish()
        }
    }
    fun rightStickUp(pointerId: Int) {
        if (rightStick.matchesPointer(pointerId)) {
            rightStick.stopTracking()
            publish()
        }
    }

    // --- Buttons ---
    fun buttonDown(button: ControllerButton, pointerId: Int) {
        buttons.press(button, pointerId)
        publish()
    }

    fun buttonUp(button: ControllerButton, pointerId: Int) {
        buttons.release(button, pointerId)
        publish()
    }

    fun pointerCancelled(pointerId: Int) {
        leftStickUp(pointerId)
        rightStickUp(pointerId)
        buttons.releasePointer(pointerId)
        publish()
    }

    fun releaseAll() {
        leftStick.reset()
        rightStick.reset()
        buttons.releaseAll()
        publish()
    }

    private fun publish() {
        val dz = deadzone()
        val sens = sensitivity()
        val (lx, ly) = leftStick.normalized(dz, sens)
        val (rx, ry) = rightStick.normalized(dz, sens)
        val mask = buttons.bitmask()
        val l2 = if (buttons.isPressed(ControllerButton.L2)) 1f else 0f
        val r2 = if (buttons.isPressed(ControllerButton.R2)) 1f else 0f

        _state.update {
            GamepadState(
                leftStickX = lx,
                leftStickY = ly,
                rightStickX = rx,
                rightStickY = ry,
                buttonsBitmask = mask,
                leftTrigger = l2,
                rightTrigger = r2,
                timestamp = System.nanoTime()
            )
        }
    }
}
