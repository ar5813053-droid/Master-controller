package com.dualphone.controller

import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.hid.HidReportDescriptor
import com.dualphone.controller.hid.HidReportEncoder
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HidReportEncoderTest {

    @Test
    fun neutralIsCentered() {
        val n = HidReportEncoder.neutral()
        assertThat(n.size).isEqualTo(8)
        assertThat(n[2].toInt() and 0xFF).isEqualTo(128)
        assertThat(n[3].toInt() and 0xFF).isEqualTo(128)
        assertThat(n[0].toInt()).isEqualTo(0)
    }

    @Test
    fun buttonASetsBit0() {
        val r = HidReportEncoder.encode(GamepadState(buttonsBitmask = ControllerButton.A.bit))
        assertThat(r[0].toInt() and 1).isEqualTo(1)
    }

    @Test
    fun simultaneousButtons() {
        val mask = ControllerButton.A.bit or ControllerButton.B.bit or ControllerButton.X.bit
        val r = HidReportEncoder.encode(GamepadState(buttonsBitmask = mask))
        assertThat(r[0].toInt() and 0b111).isEqualTo(0b111)
    }

    @Test
    fun stickExtremes() {
        val r = HidReportEncoder.encode(GamepadState(leftStickX = 1f, leftStickY = -1f))
        assertThat(r[2].toInt() and 0xFF).isEqualTo(255)
        assertThat(r[3].toInt() and 0xFF).isEqualTo(1) // -1 * 127 + 128 ≈ 1
    }

    @Test
    fun triggersFull() {
        val r = HidReportEncoder.encode(GamepadState(leftTrigger = 1f, rightTrigger = 1f))
        assertThat(r[6].toInt() and 0xFF).isEqualTo(255)
        assertThat(r[7].toInt() and 0xFF).isEqualTo(255)
    }

    @Test
    fun dpadBits() {
        val r = HidReportEncoder.encode(GamepadState(buttonsBitmask = ControllerButton.DPAD_UP.bit or ControllerButton.DPAD_RIGHT.bit))
        assertThat(r[1].toInt() and (1 shl 3)).isNotEqualTo(0)
        assertThat(r[1].toInt() and (1 shl 6)).isNotEqualTo(0)
    }

    @Test
    fun descriptorNotEmpty() {
        assertThat(HidReportDescriptor.COMPACT_DESCRIPTOR.size).isGreaterThan(20)
        assertThat(HidReportDescriptor.COMPACT_DESCRIPTOR[0]).isEqualTo(0x05)
    }

    @Test
    fun encodeNeutralMatchesEmptyState() {
        val a = HidReportEncoder.encode(GamepadState.EMPTY)
        val b = HidReportEncoder.neutral()
        assertThat(HidReportEncoder.reportsEqual(a, b)).isTrue()
    }
}
