package com.dualphone.controller.transport.protocol

import com.dualphone.controller.core.GamepadState

/**
 * Binary packet protocol contract for low-latency input transmission.
 * Implementations will encode/decode GamepadState into compact binary frames.
 */
interface PacketProtocol {
    fun encode(state: GamepadState): ByteArray
    fun decode(data: ByteArray): GamepadState?
    val headerSize: Int
    val maxPacketSize: Int
}
