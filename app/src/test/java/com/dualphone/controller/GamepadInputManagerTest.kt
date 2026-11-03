package com.dualphone.controller

import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.input.GamepadInputManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GamepadInputManagerTest {

    @Test
    fun `button press updates bitmask`() {
        val manager = GamepadInputManager(deadzone = { 0f }, sensitivity = { 1f })
        manager.buttonDown(ControllerButton.A, 1)
        assertThat(manager.state.value.isButtonPressed(ControllerButton.A)).isTrue()
        manager.buttonUp(ControllerButton.A, 1)
        assertThat(manager.state.value.isButtonPressed(ControllerButton.A)).isFalse()
    }

    @Test
    fun `left stick move updates normalized values`() {
        val manager = GamepadInputManager(deadzone = { 0f }, sensitivity = { 1f })
        manager.leftStickDown(10)
        manager.leftStickMove(10, 0.7f, -0.3f)
        val state = manager.state.value
        assertThat(state.leftStickX).isWithin(0.05f).of(0.7f)
        assertThat(state.leftStickY).isWithin(0.05f).of(-0.3f)
    }

    @Test
    fun `pointer cancel releases everything for that pointer`() {
        val manager = GamepadInputManager()
        manager.buttonDown(ControllerButton.X, 3)
        manager.leftStickDown(3)
        manager.leftStickMove(3, 0.5f, 0.5f)
        manager.pointerCancelled(3)
        val state = manager.state.value
        assertThat(state.isButtonPressed(ControllerButton.X)).isFalse()
        assertThat(state.leftStickX).isEqualTo(0f)
        assertThat(state.leftStickY).isEqualTo(0f)
    }

    @Test
    fun `releaseAll clears all state`() {
        val manager = GamepadInputManager()
        manager.buttonDown(ControllerButton.START, 1)
        manager.rightStickDown(2)
        manager.rightStickMove(2, -1f, 0f)
        manager.releaseAll()
        val state = manager.state.value
        assertThat(state.buttonsBitmask).isEqualTo(0)
        assertThat(state.rightStickX).isEqualTo(0f)
    }
}
