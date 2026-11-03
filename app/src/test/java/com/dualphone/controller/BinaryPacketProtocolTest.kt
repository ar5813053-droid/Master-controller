package com.dualphone.controller

import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.core.GamepadState
import com.dualphone.controller.transport.protocol.BinaryPacketProtocol
import com.dualphone.controller.transport.protocol.SessionAuth
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BinaryPacketProtocolTest {

    @Test
    fun `encode decode round trip preserves state`() {
        val state = GamepadState(
            leftStickX = 0.5f,
            leftStickY = -0.25f,
            rightStickX = -1f,
            rightStickY = 0.1f,
            buttonsBitmask = ControllerButton.A.bit or ControllerButton.L1.bit,
            leftTrigger = 0.8f,
            rightTrigger = 0f
        )
        val encoded = BinaryPacketProtocol.encodeInputState(
            sessionId = 42,
            sequence = 7,
            state = state,
            timestampMs = 1_700_000_000_000L
        )
        assertThat(encoded.size).isEqualTo(BinaryPacketProtocol.PACKET_SIZE)
        val decoded = BinaryPacketProtocol.decodePacket(encoded)
        assertThat(decoded).isNotNull()
        assertThat(decoded!!.sessionId).isEqualTo(42)
        assertThat(decoded.sequence).isEqualTo(7)
        assertThat(decoded.type).isEqualTo(BinaryPacketProtocol.PacketType.INPUT_STATE)
        val s = decoded.state!!
        assertThat(s.leftStickX).isWithin(0.001f).of(0.5f)
        assertThat(s.leftStickY).isWithin(0.001f).of(-0.25f)
        assertThat(s.rightStickX).isWithin(0.001f).of(-1f)
        assertThat(s.buttonsBitmask and ControllerButton.A.bit).isNotEqualTo(0)
        assertThat(s.leftTrigger).isWithin(0.001f).of(0.8f)
    }

    @Test
    fun `rejects short packets`() {
        assertThat(BinaryPacketProtocol.decodePacket(ByteArray(10))).isNull()
    }

    @Test
    fun `rejects wrong protocol version`() {
        val state = GamepadState.EMPTY
        val encoded = BinaryPacketProtocol.encodeInputState(1, 1, state)
        encoded[0] = 99
        assertThat(BinaryPacketProtocol.decodePacket(encoded)).isNull()
    }

    @Test
    fun `rejects unknown packet type`() {
        val encoded = BinaryPacketProtocol.encodeInputState(1, 1, GamepadState.EMPTY)
        encoded[1] = 0x7F
        assertThat(BinaryPacketProtocol.decodePacket(encoded)).isNull()
    }

    @Test
    fun `rejects NaN joystick values`() {
        val state = GamepadState(leftStickX = Float.NaN)
        val encoded = BinaryPacketProtocol.encodeInputState(1, 1, state)
        assertThat(BinaryPacketProtocol.decodePacket(encoded)).isNull()
    }

    @Test
    fun `release all packet decodes to empty state`() {
        val packet = BinaryPacketProtocol.encodeReleaseAll(sessionId = 9, sequence = 3)
        val decoded = BinaryPacketProtocol.decodePacket(packet)
        assertThat(decoded).isNotNull()
        assertThat(decoded!!.type).isEqualTo(BinaryPacketProtocol.PacketType.RELEASE_ALL)
        assertThat(decoded.state).isEqualTo(GamepadState.EMPTY)
    }

    @Test
    fun `session auth request response round trip`() {
        val token = SessionAuth.generatePairingToken()
        val req = SessionAuth.encodeAuthRequest(token, "TestPhone")
        val decodedReq = SessionAuth.decodeAuthRequest(req)
        assertThat(decodedReq).isNotNull()
        assertThat(decodedReq!!.pairingToken).isEqualTo(token)
        assertThat(decodedReq.deviceName).isEqualTo("TestPhone")

        val sessionId = SessionAuth.generateSessionId()
        val resp = SessionAuth.encodeAuthResponse(sessionId, true, 17421)
        val decodedResp = SessionAuth.decodeAuthResponse(resp)
        assertThat(decodedResp).isNotNull()
        assertThat(decodedResp!!.accepted).isTrue()
        assertThat(decodedResp.sessionId).isEqualTo(sessionId)
        assertThat(decodedResp.udpPort).isEqualTo(17421)
    }

    @Test
    fun `auth rejects wrong type`() {
        val bad = BinaryPacketProtocol.encodeInputState(1, 1, GamepadState.EMPTY)
        assertThat(SessionAuth.decodeAuthRequest(bad)).isNull()
    }
}
