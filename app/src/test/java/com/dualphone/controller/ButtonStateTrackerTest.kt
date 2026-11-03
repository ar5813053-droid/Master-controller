package com.dualphone.controller

import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.input.buttons.ButtonStateTracker
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ButtonStateTrackerTest {

    @Test
    fun `press and release single button`() {
        val tracker = ButtonStateTracker()
        tracker.press(ControllerButton.A, 1)
        assertThat(tracker.isPressed(ControllerButton.A)).isTrue()
        tracker.release(ControllerButton.A, 1)
        assertThat(tracker.isPressed(ControllerButton.A)).isFalse()
    }

    @Test
    fun `wrong pointer id does not release`() {
        val tracker = ButtonStateTracker()
        tracker.press(ControllerButton.B, 5)
        tracker.release(ControllerButton.B, 99)
        assertThat(tracker.isPressed(ControllerButton.B)).isTrue()
    }

    @Test
    fun `multiple simultaneous buttons`() {
        val tracker = ButtonStateTracker()
        tracker.press(ControllerButton.A, 1)
        tracker.press(ControllerButton.B, 2)
        tracker.press(ControllerButton.X, 3)
        assertThat(tracker.isPressed(ControllerButton.A)).isTrue()
        assertThat(tracker.isPressed(ControllerButton.B)).isTrue()
        assertThat(tracker.isPressed(ControllerButton.X)).isTrue()
        val mask = tracker.bitmask()
        assertThat(mask and ControllerButton.A.bit).isNotEqualTo(0)
        assertThat(mask and ControllerButton.B.bit).isNotEqualTo(0)
        assertThat(mask and ControllerButton.X.bit).isNotEqualTo(0)
    }

    @Test
    fun `releasePointer clears all owned by that pointer`() {
        val tracker = ButtonStateTracker()
        tracker.press(ControllerButton.A, 7)
        tracker.press(ControllerButton.Y, 7)
        tracker.press(ControllerButton.START, 8)
        tracker.releasePointer(7)
        assertThat(tracker.isPressed(ControllerButton.A)).isFalse()
        assertThat(tracker.isPressed(ControllerButton.Y)).isFalse()
        assertThat(tracker.isPressed(ControllerButton.START)).isTrue()
    }

    @Test
    fun `releaseAll clears everything`() {
        val tracker = ButtonStateTracker()
        tracker.press(ControllerButton.L1, 1)
        tracker.press(ControllerButton.R1, 2)
        tracker.releaseAll()
        assertThat(tracker.bitmask()).isEqualTo(0)
        assertThat(tracker.activeButtons()).isEmpty()
    }
}
