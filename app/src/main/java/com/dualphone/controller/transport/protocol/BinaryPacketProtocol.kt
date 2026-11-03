package com.dualphone.controller.transport.protocol

import com.dualphone.controller.core.GamepadState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

/**
 * Versioned binary input packet protocol.
 *
 * Layout (little-endian):
 *  0    : protocol version (u8)
 *  1    : packet type (u8)
 *  2-5  : session id (u32)
 *  6-9  : sequence number (u32)
 * 10-17 : timestamp millis (u64)
 * 18-21 : leftStickX (f32)
 * 22-25 : leftStickY (f32)
 * 26-29 : rightStickX (f32)
 * 30-33 : rightStickY (f32)
 * 34-37 : buttonsBitmask (u32)
 * 38-41 : leftTrigger (f32)
 * 42-45 : rightTrigger (f32)
 * Total: 46 bytes for INPUT_STATE
 */
object BinaryPacketProtocol : PacketProtocol {

    const val PROTOCOL_VERSION: Byte = 1
    const val PACKET_SIZE = 46
    const val MAGIC_HEADER_SIZE = 2 // version + type

    enum class PacketType(val code: Byte) {
        INPUT_STATE(0x01),
        RELEASE_ALL(0x02),
        HEARTBEAT(0x03),
        AUTH_REQUEST(0x10),
        AUTH_RESPONSE(0x11),
        DISCONNECT(0x12);

        companion object {
            fun fromCode(code: Byte): PacketType? = entries.find { it.code == code }
        }
    }

    private val sequence = AtomicInteger(0)

    override val headerSize: Int = MAGIC_HEADER_SIZE
    override val maxPacketSize: Int = PACKET_SIZE

    fun nextSequence(): Int = sequence.getAndIncrement()

    fun resetSequence() {
        sequence.set(0)
    }

    override fun encode(state: GamepadState): ByteArray {
        return encodeInputState(
            sessionId = 0,
            sequence = nextSequence(),
            state = state
        )
    }

    fun encodeInputState(
        sessionId: Int,
        sequence: Int,
        state: GamepadState,
        timestampMs: Long = System.currentTimeMillis()
    ): ByteArray {
        val buf = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(PROTOCOL_VERSION)
        buf.put(PacketType.INPUT_STATE.code)
        buf.putInt(sessionId)
        buf.putInt(sequence)
        buf.putLong(timestampMs)
        buf.putFloat(state.leftStickX)
        buf.putFloat(state.leftStickY)
        buf.putFloat(state.rightStickX)
        buf.putFloat(state.rightStickY)
        buf.putInt(state.buttonsBitmask)
        buf.putFloat(state.leftTrigger)
        buf.putFloat(state.rightTrigger)
        return buf.array()
    }

    fun encodeReleaseAll(sessionId: Int, sequence: Int): ByteArray {
        val buf = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(PROTOCOL_VERSION)
        buf.put(PacketType.RELEASE_ALL.code)
        buf.putInt(sessionId)
        buf.putInt(sequence)
        buf.putLong(System.currentTimeMillis())
        // remaining zeros
        return buf.array()
    }

    fun encodeHeartbeat(sessionId: Int, sequence: Int): ByteArray {
        val buf = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(PROTOCOL_VERSION)
        buf.put(PacketType.HEARTBEAT.code)
        buf.putInt(sessionId)
        buf.putInt(sequence)
        buf.putLong(System.currentTimeMillis())
        return buf.array()
    }

    data class DecodedPacket(
        val version: Byte,
        val type: PacketType,
        val sessionId: Int,
        val sequence: Int,
        val timestampMs: Long,
        val state: GamepadState?
    )

    /**
     * Decode and validate a raw UDP/TCP payload.
     * Returns null for malformed, wrong version, or unknown type packets.
     */
    fun decodePacket(data: ByteArray): DecodedPacket? {
        if (data.size < PACKET_SIZE) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val version = buf.get()
        if (version != PROTOCOL_VERSION) return null
        val typeCode = buf.get()
        val type = PacketType.fromCode(typeCode) ?: return null
        val sessionId = buf.int
        val sequence = buf.int
        val timestampMs = buf.long

        val state = when (type) {
            PacketType.INPUT_STATE -> {
                val lx = buf.float
                val ly = buf.float
                val rx = buf.float
                val ry = buf.float
                val mask = buf.int
                val lt = buf.float
                val rt = buf.float
                // Reject NaN / Inf
                if (!lx.isFinite() || !ly.isFinite() || !rx.isFinite() || !ry.isFinite() ||
                    !lt.isFinite() || !rt.isFinite()
                ) return null
                GamepadState(
                    leftStickX = lx.coerceIn(-1f, 1f),
                    leftStickY = ly.coerceIn(-1f, 1f),
                    rightStickX = rx.coerceIn(-1f, 1f),
                    rightStickY = ry.coerceIn(-1f, 1f),
                    buttonsBitmask = mask,
                    leftTrigger = lt.coerceIn(0f, 1f),
                    rightTrigger = rt.coerceIn(0f, 1f),
                    timestamp = timestampMs * 1_000_000L // store as ns-ish for consistency
                )
            }
            PacketType.RELEASE_ALL -> GamepadState.EMPTY
            else -> null
        }
        return DecodedPacket(version, type, sessionId, sequence, timestampMs, state)
    }

    override fun decode(data: ByteArray): GamepadState? {
        return decodePacket(data)?.state
    }
}
