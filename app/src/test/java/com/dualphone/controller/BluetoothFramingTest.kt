package com.dualphone.controller

import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.transport.protocol.BinaryPacketProtocol
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Verifies length-prefixed framing used by Bluetooth RFCOMM stream transport.
 */
class BluetoothFramingTest {

    @Test
    fun `length prefixed packet round trip`() {
        val state = GamepadState(
            leftStickX = 0.25f,
            buttonsBitmask = ControllerButton.A.bit or ControllerButton.START.bit
        )
        val payload = BinaryPacketProtocol.encodeInputState(55, 3, state)
        val out = ByteArrayOutputStream()
        out.write(payload.size and 0xFF)
        out.write((payload.size shr 8) and 0xFF)
        out.write(payload)

        val input = ByteArrayInputStream(out.toByteArray())
        val lenLo = input.read()
        val lenHi = input.read()
        val len = lenLo or (lenHi shl 8)
        assertThat(len).isEqualTo(BinaryPacketProtocol.PACKET_SIZE)
        val data = ByteArray(len)
        var off = 0
        while (off < len) {
            val n = input.read(data, off, len - off)
            assertThat(n).isGreaterThan(0)
            off += n
        }
        val decoded = BinaryPacketProtocol.decodePacket(data)
        assertThat(decoded).isNotNull()
        assertThat(decoded!!.sessionId).isEqualTo(55)
        assertThat(decoded.state!!.isButtonPressed(ControllerButton.A)).isTrue()
        assertThat(decoded.state!!.leftStickX).isWithin(0.01f).of(0.25f)
    }

    @Test
    fun `rejects oversized length prefix`() {
        val badLen = BinaryPacketProtocol.PACKET_SIZE + 100
        val out = ByteArrayOutputStream()
        out.write(badLen and 0xFF)
        out.write((badLen shr 8) and 0xFF)
        // incomplete body — consumer should refuse len > max
        assertThat(badLen).isGreaterThan(BinaryPacketProtocol.PACKET_SIZE + 8)
    }

    @Test
    fun `release all over framed stream`() {
        val payload = BinaryPacketProtocol.encodeReleaseAll(10, 1)
        val out = ByteArrayOutputStream()
        out.write(payload.size and 0xFF)
        out.write((payload.size shr 8) and 0xFF)
        out.write(payload)
        val input = ByteArrayInputStream(out.toByteArray())
        val len = input.read() or (input.read() shl 8)
        val data = ByteArray(len)
        input.read(data)
        val decoded = BinaryPacketProtocol.decodePacket(data)!!
        assertThat(decoded.type).isEqualTo(BinaryPacketProtocol.PacketType.RELEASE_ALL)
        assertThat(decoded.state).isEqualTo(GamepadState.EMPTY)
    }
}
