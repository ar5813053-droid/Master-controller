package com.dualphone.controller

import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.transport.protocol.BinaryPacketProtocol
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SequenceAndSessionTest {

    @Test
    fun `sequence numbers increase`() {
        BinaryPacketProtocol.resetSequence()
        val a = BinaryPacketProtocol.nextSequence()
        val b = BinaryPacketProtocol.nextSequence()
        assertThat(b).isEqualTo(a + 1)
    }

    @Test
    fun `session id mismatch can be detected by receiver logic`() {
        val sessionA = 111
        val sessionB = 222
        val packet = BinaryPacketProtocol.encodeInputState(sessionA, 1, GamepadState.EMPTY)
        val decoded = BinaryPacketProtocol.decodePacket(packet)!!
        assertThat(decoded.sessionId).isEqualTo(sessionA)
        assertThat(decoded.sessionId).isNotEqualTo(sessionB)
    }

    @Test
    fun `loss estimate from sequence gap`() {
        // Simulate receiver-side gap detection
        var lastSeq = 10
        var lost = 0L
        val incoming = listOf(11, 12, 15, 16) // gap of 2 between 12 and 15
        for (seq in incoming) {
            if (lastSeq >= 0 && seq > lastSeq + 1) {
                lost += (seq - lastSeq - 1).toLong()
            }
            if (seq > lastSeq) lastSeq = seq
        }
        assertThat(lost).isEqualTo(2)
    }
}
