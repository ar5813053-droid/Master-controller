package com.dualphone.controller

import com.dualphone.controller.input.joystick.JoystickState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class JoystickNormalizationTest {

    @Test
    fun `deadzone zeroes small movements`() {
        val stick = JoystickState()
        stick.updateRaw(0.05f, 0.05f)
        val (x, y) = stick.normalized(deadzone = 0.12f, sensitivity = 1f)
        assertThat(x).isEqualTo(0f)
        assertThat(y).isEqualTo(0f)
    }

    @Test
    fun `full deflection produces unit vector`() {
        val stick = JoystickState()
        stick.updateRaw(1f, 0f)
        val (x, y) = stick.normalized(deadzone = 0f, sensitivity = 1f)
        assertThat(x).isWithin(0.01f).of(1f)
        assertThat(y).isWithin(0.01f).of(0f)
    }

    @Test
    fun `sensitivity scales output`() {
        val stick = JoystickState()
        stick.updateRaw(0.5f, 0f)
        val (x1, _) = stick.normalized(deadzone = 0f, sensitivity = 1f)
        val (x2, _) = stick.normalized(deadzone = 0f, sensitivity = 2f)
        assertThat(x2).isGreaterThan(x1)
        assertThat(x2).isAtMost(1f)
    }

    @Test
    fun `diagonal is clamped to unit circle`() {
        val stick = JoystickState()
        stick.updateRaw(1f, 1f)
        val (x, y) = stick.normalized(deadzone = 0f, sensitivity = 1f)
        val mag = kotlin.math.sqrt(x * x + y * y)
        assertThat(mag).isWithin(0.02f).of(1f)
    }

    @Test
    fun `reset clears state`() {
        val stick = JoystickState()
        stick.startTracking(1)
        stick.updateRaw(0.8f, 0.3f)
        stick.reset()
        assertThat(stick.isActive).isFalse()
        val (x, y) = stick.normalized(0f, 1f)
        assertThat(x).isEqualTo(0f)
        assertThat(y).isEqualTo(0f)
    }
}
