package com.dualphone.controller

import com.dualphone.controller.transport.TransportKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransportKindTest {

    @Test
    fun `only one logical active transport at a time`() {
        // Pure logic: switching means previous must be cleared
        var active = TransportKind.NONE
        fun switchTo(next: TransportKind) {
            if (active != TransportKind.NONE && active != next) {
                active = TransportKind.NONE // disconnect first
            }
            active = next
        }
        switchTo(TransportKind.WIFI_TCP_UDP)
        assertThat(active).isEqualTo(TransportKind.WIFI_TCP_UDP)
        switchTo(TransportKind.BLUETOOTH_RFCOMM)
        assertThat(active).isEqualTo(TransportKind.BLUETOOTH_RFCOMM)
        switchTo(TransportKind.NONE)
        assertThat(active).isEqualTo(TransportKind.NONE)
    }
}
