package com.dualphone.controller.input.buttons

import com.dualphone.controller.core.ControllerButton
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks pressed state of all controller buttons with pointer-id ownership
 * to prevent stuck buttons when a finger leaves the screen or is cancelled.
 */
class ButtonStateTracker {
    private val pressed = ConcurrentHashMap<ControllerButton, Int>() // button -> pointerId

    fun press(button: ControllerButton, pointerId: Int) {
        pressed[button] = pointerId
    }

    fun release(button: ControllerButton, pointerId: Int) {
        if (pressed[button] == pointerId) {
            pressed.remove(button)
        }
    }

    fun releasePointer(pointerId: Int) {
        val toRemove = pressed.entries.filter { it.value == pointerId }.map { it.key }
        toRemove.forEach { pressed.remove(it) }
    }

    fun releaseAll() {
        pressed.clear()
    }

    fun isPressed(button: ControllerButton): Boolean = pressed.containsKey(button)

    fun bitmask(): Int {
        var mask = 0
        for (button in pressed.keys) {
            mask = mask or button.bit
        }
        return mask
    }

    fun activeButtons(): Set<ControllerButton> = pressed.keys.toSet()
}
