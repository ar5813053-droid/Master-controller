package com.dualphone.controller

import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.transport.protocol.BinaryPacketProtocol
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionStabilityTest {

    @Test
    fun `10000 consecutive packets decode without session change`() {
        val session = 0x42ABCDEF
        BinaryPacketProtocol.resetSequence()
        var lastSeq = -1
        repeat(10_000) {
            val state = GamepadState(
                leftStickX = (it % 100) / 100f,
                leftStickY = -((it % 50) / 50f),
                rightStickX = 0.25f,
                buttonsBitmask = if (it % 3 == 0) ControllerButton.A.bit else 0
            )
            val seq = BinaryPacketProtocol.nextSequence()
            val packet = BinaryPacketProtocol.encodeInputState(session, seq, state)
            val decoded = BinaryPacketProtocol.decodePacket(packet)
            assertThat(decoded).isNotNull()
            assertThat(decoded!!.sessionId).isEqualTo(session)
            assertThat(decoded.sequence).isEqualTo(seq)
            if (lastSeq >= 0) assertThat(seq).isEqualTo(lastSeq + 1)
            lastSeq = seq
        }
    }

    @Test
    fun `button press release transitions preserved`() {
        val session = 7
        val press = GamepadState(buttonsBitmask = ControllerButton.A.bit or ControllerButton.B.bit)
        val release = GamepadState.EMPTY
        val p1 = BinaryPacketProtocol.decodePacket(
            BinaryPacketProtocol.encodeInputState(session, 1, press)
        )!!.state!!
        val p2 = BinaryPacketProtocol.decodePacket(
            BinaryPacketProtocol.encodeInputState(session, 2, release)
        )!!.state!!
        assertThat(p1.isButtonPressed(ControllerButton.A)).isTrue()
        assertThat(p2.buttonsBitmask).isEqualTo(0)
    }
}
