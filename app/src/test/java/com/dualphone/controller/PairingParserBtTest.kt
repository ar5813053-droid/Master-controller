package com.dualphone.controller

import com.dualphone.controller.transport.PairingParser
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PairingParserBtTest {
    @Test
    fun tokenOnlyLooksLikeBluetooth() {
        assertThat(PairingParser.looksLikeBluetooth("6BF27991")).isTrue()
    }

    @Test
    fun wifiDoesNotLookLikeBt() {
        assertThat(PairingParser.looksLikeBluetooth("192.168.1.5:17420:6BF27991")).isFalse()
    }

    @Test
    fun fullBtParses() {
        val r = PairingParser.parseBluetooth("BT:AA:BB:CC:DD:EE:FF:6BF27991")
        assertThat(r.isSuccess).isTrue()
        assertThat(r.getOrThrow().address).isEqualTo("AA:BB:CC:DD:EE:FF")
        assertThat(r.getOrThrow().token).isEqualTo(0x6BF27991)
    }

    @Test
    fun hexTokenParses() {
        assertThat(PairingParser.parseToken("6BF27991").getOrThrow()).isEqualTo(0x6BF27991)
    }
}
