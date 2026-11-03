package com.dualphone.controller.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.core.GamepadState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AccessibilityService that converts received gamepad state into on-screen touch gestures.
 *
 * User must enable this service manually in system Accessibility settings.
 * Does not read screen content, passwords, or transmit any UI data.
 *
 * Multitouch limitation: Android GestureDescription supports multiple strokes in one
 * gesture (up to ~10), but continuous independent stick + buttons is best-effort.
 * Concurrent stick motion uses periodic gesture updates that may briefly interrupt.
 */
class TouchMappingAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "TouchMappingAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally empty — we do not inspect screen content.
    }

    override fun onInterrupt() {
        releaseAllTouches()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        releaseAllTouches()
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        releaseAllTouches()
        instance = null
        super.onDestroy()
    }

    fun applyState(state: GamepadState, mapping: TouchMappingProfile) {
        if (!running.get()) return
        val w = resources.displayMetrics.widthPixels.toFloat()
        val h = resources.displayMetrics.heightPixels.toFloat()

        // Button taps / holds
        for (btn in ControllerButton.entries) {
            val pressed = state.isButtonPressed(btn)
            val was = activeButtons[btn] == true
            val point = mapping.buttonPoints[btn] ?: continue
            val sx = point.nx * w
            val sy = point.ny * h
            when {
                pressed && !was -> {
                    activeButtons[btn] = true
                    dispatchTap(sx, sy, hold = true, id = btn.name)
                }
                !pressed && was -> {
                    activeButtons[btn] = false
                    // End hold by short release tap at same point (best-effort)
                    dispatchTap(sx, sy, hold = false, id = btn.name)
                }
            }
        }

        // Left stick
        mapping.leftStick?.let { stick ->
            val cx = stick.centerX * w
            val cy = stick.centerY * h
            val r = stick.radius * minOf(w, h)
            val mag = kotlin.math.sqrt(state.leftStickX * state.leftStickX + state.leftStickY * state.leftStickY)
            if (mag > 0.05f) {
                val tx = cx + state.leftStickX * r
                val ty = cy + state.leftStickY * r
                dispatchDrag(cx, cy, tx, ty, id = "LS")
            } else if (activeSticks.remove("LS") == true) {
                dispatchTap(cx, cy, hold = false, id = "LS")
            }
        }

        // Right stick
        mapping.rightStick?.let { stick ->
            val cx = stick.centerX * w
            val cy = stick.centerY * h
            val r = stick.radius * minOf(w, h)
            val mag = kotlin.math.sqrt(state.rightStickX * state.rightStickX + state.rightStickY * state.rightStickY)
            if (mag > 0.05f) {
                val tx = cx + state.rightStickX * r
                val ty = cy + state.rightStickY * r
                dispatchDrag(cx, cy, tx, ty, id = "RS")
            } else if (activeSticks.remove("RS") == true) {
                dispatchTap(cx, cy, hold = false, id = "RS")
            }
        }
    }

    fun releaseAllTouches() {
        activeButtons.clear()
        activeSticks.clear()
        // No explicit cancel API for all gestures; future strokes will replace
    }

    private fun dispatchTap(x: Float, y: Float, hold: Boolean, id: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val path = Path().apply { moveTo(x, y) }
        val duration = if (hold) 80L else 40L
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        try {
            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.w(TAG, "gesture failed id=$id: ${e.message}")
        }
    }

    private fun dispatchDrag(x0: Float, y0: Float, x1: Float, y1: Float, id: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        activeSticks[id] = true
        val path = Path().apply {
            moveTo(x0, y0)
            lineTo(x1, y1)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        try {
            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.w(TAG, "drag failed id=$id: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "TouchMapA11y"
        @Volatile
        var instance: TouchMappingAccessibilityService? = null
            private set

        private val running = AtomicBoolean(true)
        private val activeButtons = ConcurrentHashMap<ControllerButton, Boolean>()
        private val activeSticks = ConcurrentHashMap<String, Boolean>()

        fun isEnabled(): Boolean = instance != null
    }
}

data class StickTouchPoint(
    val centerX: Float,
    val centerY: Float,
    val radius: Float
)

data class ButtonTouchPoint(
    val nx: Float,
    val ny: Float
)

data class TouchMappingProfile(
    val name: String = "Default",
    val leftStick: StickTouchPoint? = StickTouchPoint(0.2f, 0.7f, 0.12f),
    val rightStick: StickTouchPoint? = StickTouchPoint(0.8f, 0.7f, 0.12f),
    val buttonPoints: Map<ControllerButton, ButtonTouchPoint> = defaultButtons()
) {
    companion object {
        fun defaultButtons(): Map<ControllerButton, ButtonTouchPoint> = mapOf(
            ControllerButton.A to ButtonTouchPoint(0.85f, 0.75f),
            ControllerButton.B to ButtonTouchPoint(0.92f, 0.65f),
            ControllerButton.X to ButtonTouchPoint(0.78f, 0.65f),
            ControllerButton.Y to ButtonTouchPoint(0.85f, 0.55f),
            ControllerButton.L1 to ButtonTouchPoint(0.15f, 0.25f),
            ControllerButton.L2 to ButtonTouchPoint(0.12f, 0.18f),
            ControllerButton.R1 to ButtonTouchPoint(0.85f, 0.25f),
            ControllerButton.R2 to ButtonTouchPoint(0.88f, 0.18f),
            ControllerButton.START to ButtonTouchPoint(0.55f, 0.15f),
            ControllerButton.SELECT to ButtonTouchPoint(0.45f, 0.15f),
            ControllerButton.DPAD_UP to ButtonTouchPoint(0.25f, 0.55f),
            ControllerButton.DPAD_DOWN to ButtonTouchPoint(0.25f, 0.75f),
            ControllerButton.DPAD_LEFT to ButtonTouchPoint(0.18f, 0.65f),
            ControllerButton.DPAD_RIGHT to ButtonTouchPoint(0.32f, 0.65f)
        )
    }
}
