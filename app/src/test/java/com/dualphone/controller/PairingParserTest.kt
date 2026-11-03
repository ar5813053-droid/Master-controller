package com.dualphone.controller

import com.dualphone.controller.transport.PairingParser
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PairingParserTest {

    @Test
    fun `parses hex token with letters`() {
        val r = PairingParser.parseWifi("10.140.249.51:17420:A1B2C3D4")
        assertThat(r.isSuccess).isTrue()
        val p = r.getOrThrow()
        assertThat(p.host).isEqualTo("10.140.249.51")
        assertThat(p.tcpPort).isEqualTo(17420)
        assertThat(p.token).isEqualTo(0xA1B2C3D4.toInt())
    }

    @Test
    fun `all-digit hex is not misread as decimal`() {
        val token = 0x01234567
        val display = PairingParser.formatToken(token)
        assertThat(display).isEqualTo("01234567")
        val r = PairingParser.parseWifi("192.168.1.1:17420:$display")
        assertThat(r.getOrThrow().token).isEqualTo(token)
        // Old bug: toIntOrNull would parse 1234567
        assertThat(r.getOrThrow().token).isNotEqualTo(1234567)
    }

    @Test
    fun `negative token survives hex round trip`() {
        val token = -1
        val display = PairingParser.formatToken(token)
        assertThat(display).isEqualTo("FFFFFFFF")
        assertThat(PairingParser.parseToken(display).getOrThrow()).isEqualTo(-1)
    }

    @Test
    fun `trims whitespace and newlines`() {
        val r = PairingParser.parseWifi("  10.0.0.2:17420:DEADBEEF \n")
        assertThat(r.isSuccess).isTrue()
        assertThat(r.getOrThrow().token).isEqualTo(0xDEADBEEF.toInt())
    }

    @Test
    fun `rejects wrong field count`() {
        assertThat(PairingParser.parseWifi("10.0.0.1:17420").isFailure).isTrue()
    }

    @Test
    fun `bluetooth mac and token`() {
        val r = PairingParser.parseBluetooth("BT:AA:BB:CC:DD:EE:FF:00ABCDEF")
        assertThat(r.isSuccess).isTrue()
        assertThat(r.getOrThrow().address).isEqualTo("AA:BB:CC:DD:EE:FF")
    }

    @Test
    fun `format and parse round trip`() {
        val token = SessionTokenSample
        val s = PairingParser.formatWifiPairing("10.1.2.3", 17420, token)
        val p = PairingParser.parseWifi(s).getOrThrow()
        assertThat(p.token).isEqualTo(token)
        assertThat(p.tcpPort).isEqualTo(17420)
    }

    companion object {
        // non-trivial mixed hex
        private const val SessionTokenSample = 0x4F3A21B0
    }
}
