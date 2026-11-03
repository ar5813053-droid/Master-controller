package com.dualphone.controller.core

import kotlinx.serialization.Serializable

/**
 * Represents the complete instantaneous state of a virtual gamepad.
 * Designed for low-allocation updates and efficient serialization in future transport layers.
 */
data class GamepadState(
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val buttonsBitmask: Int = 0,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
    val timestamp: Long = System.nanoTime()
) {
    fun isButtonPressed(button: ControllerButton): Boolean {
        return (buttonsBitmask and button.bit) != 0
    }

    companion object {
        val EMPTY = GamepadState()
    }
}

/**
 * All supported controller buttons with stable bit positions for the bitmask.
 */
enum class ControllerButton(val bit: Int, val displayName: String) {
    A(1 shl 0, "A"),
    B(1 shl 1, "B"),
    X(1 shl 2, "X"),
    Y(1 shl 3, "Y"),
    L1(1 shl 4, "L1"),
    R1(1 shl 5, "R1"),
    L2(1 shl 6, "L2"),
    R2(1 shl 7, "R2"),
    START(1 shl 8, "Start"),
    SELECT(1 shl 9, "Select"),
    HOME(1 shl 10, "Home"),
    DPAD_UP(1 shl 11, "D-Pad Up"),
    DPAD_DOWN(1 shl 12, "D-Pad Down"),
    DPAD_LEFT(1 shl 13, "D-Pad Left"),
    DPAD_RIGHT(1 shl 14, "D-Pad Right");

    companion object {
        fun fromBit(bit: Int): ControllerButton? = entries.find { it.bit == bit }
    }
}

/**
 * Type of control on the virtual layout canvas.
 */
enum class ControlType {
    LEFT_STICK,
    RIGHT_STICK,
    DPAD,
    BUTTON_A,
    BUTTON_B,
    BUTTON_X,
    BUTTON_Y,
    BUTTON_L1,
    BUTTON_L2,
    BUTTON_R1,
    BUTTON_R2,
    BUTTON_START,
    BUTTON_SELECT,
    BUTTON_HOME
}

/**
 * Position and appearance of a single control on the layout canvas.
 */
@Serializable
data class ControlPosition(
    val controlId: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val opacity: Float = 1f,
    val controlType: ControlType
)

/**
 * A complete saved controller layout.
 */
@Serializable
data class ControllerLayout(
    val layoutId: String,
    val layoutName: String,
    val controls: List<ControlPosition>,
    val opacity: Float = 1f,
    val orientation: String = "landscape"
)

/**
 * Application-wide settings persisted via DataStore.
 */
data class AppSettings(
    val hapticEnabled: Boolean = true,
    val joystickDeadzone: Float = 0.12f,
    val joystickSensitivity: Float = 1.0f,
    val buttonOpacity: Float = 0.85f,
    val landscapeMode: Boolean = true,
    val volumeUpTarget: String = "DISABLED",
    val volumeDownTarget: String = "DISABLED"
)

/**
 * Current device role in the dual-phone system.
 */
enum class DeviceRole {
    NONE,
    CONTROLLER,
    GAMING_RECEIVER
}

/**
 * Connection status for UI display (no real connection in Milestone 1).
 */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}
