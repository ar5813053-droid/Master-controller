package com.dualphone.controller.input.joystick

/**
 * Mutable joystick state kept outside Compose for low-allocation updates.
 * Normalized X/Y are in range [-1, 1] after deadzone and sensitivity.
 */
class JoystickState(
    private var rawX: Float = 0f,
    private var rawY: Float = 0f,
    private var pointerId: Int = -1
) {
    val isActive: Boolean get() = pointerId >= 0

    fun startTracking(id: Int) {
        pointerId = id
    }

    fun stopTracking() {
        pointerId = -1
        rawX = 0f
        rawY = 0f
    }

    fun matchesPointer(id: Int): Boolean = pointerId == id

    fun updateRaw(x: Float, y: Float) {
        rawX = x
        rawY = y
    }

    /**
     * Returns normalized [x, y] after applying deadzone and sensitivity.
     * Values outside the unit circle are clamped.
     */
    fun normalized(deadzone: Float, sensitivity: Float): Pair<Float, Float> {
        val magnitude = kotlin.math.sqrt(rawX * rawX + rawY * rawY)
        if (magnitude < deadzone) {
            return 0f to 0f
        }
        val scale = ((magnitude - deadzone) / (1f - deadzone)).coerceIn(0f, 1f) * sensitivity
        val nx = (rawX / magnitude) * scale
        val ny = (rawY / magnitude) * scale
        val clampMag = kotlin.math.sqrt(nx * nx + ny * ny)
        return if (clampMag > 1f) {
            (nx / clampMag) to (ny / clampMag)
        } else {
            nx to ny
        }
    }

    fun reset() {
        stopTracking()
    }
}
