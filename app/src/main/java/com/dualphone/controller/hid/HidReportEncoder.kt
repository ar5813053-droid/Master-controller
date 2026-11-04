package com.dualphone.controller.hid

import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.core.GamepadState
import kotlin.math.roundToInt

/**
 * Encodes [GamepadState] into an 8-byte HID input report matching
 * [HidReportDescriptor.COMPACT_DESCRIPTOR].
 *
 * Byte layout:
 *  0: buttons bit0=A bit1=B bit2=X bit3=Y bit4=L1 bit5=R1 bit6=Select bit7=Start
 *  1: buttons bit0=L2 bit1=R2 bit2=Home bit3=L3 bit4=R3 + dpad bits 5-7 unused;
 *     dpad encoded as individual button bits in high nibble alternative:
 *     bit3=Up bit4=Down bit5=Left bit6=Right (within byte1)
 *  2: LX 0–255 (128 center)
 *  3: LY 0–255 (128 center)
 *  4: RX 0–255
 *  5: RY 0–255
 *  6: LT 0–255
 *  7: RT 0–255
 */
object HidReportEncoder {

    const val REPORT_SIZE = 8
    const val AXIS_CENTER = 128
    const val AXIS_MAX = 255

    private val NEUTRAL = ByteArray(REPORT_SIZE).also {
        it[2] = AXIS_CENTER.toByte()
        it[3] = AXIS_CENTER.toByte()
        it[4] = AXIS_CENTER.toByte()
        it[5] = AXIS_CENTER.toByte()
    }

    fun neutral(): ByteArray = NEUTRAL.copyOf()

    fun encode(state: GamepadState): ByteArray {
        val out = ByteArray(REPORT_SIZE)
        var b0 = 0
        var b1 = 0
        if (state.isButtonPressed(ControllerButton.A)) b0 = b0 or (1 shl 0)
        if (state.isButtonPressed(ControllerButton.B)) b0 = b0 or (1 shl 1)
        if (state.isButtonPressed(ControllerButton.X)) b0 = b0 or (1 shl 2)
        if (state.isButtonPressed(ControllerButton.Y)) b0 = b0 or (1 shl 3)
        if (state.isButtonPressed(ControllerButton.L1)) b0 = b0 or (1 shl 4)
        if (state.isButtonPressed(ControllerButton.R1)) b0 = b0 or (1 shl 5)
        if (state.isButtonPressed(ControllerButton.SELECT)) b0 = b0 or (1 shl 6)
        if (state.isButtonPressed(ControllerButton.START)) b0 = b0 or (1 shl 7)

        if (state.isButtonPressed(ControllerButton.L2) || state.leftTrigger > 0.5f) {
            b1 = b1 or (1 shl 0)
        }
        if (state.isButtonPressed(ControllerButton.R2) || state.rightTrigger > 0.5f) {
            b1 = b1 or (1 shl 1)
        }
        if (state.isButtonPressed(ControllerButton.HOME)) b1 = b1 or (1 shl 2)
        if (state.isButtonPressed(ControllerButton.DPAD_UP)) b1 = b1 or (1 shl 3)
        if (state.isButtonPressed(ControllerButton.DPAD_DOWN)) b1 = b1 or (1 shl 4)
        if (state.isButtonPressed(ControllerButton.DPAD_LEFT)) b1 = b1 or (1 shl 5)
        if (state.isButtonPressed(ControllerButton.DPAD_RIGHT)) b1 = b1 or (1 shl 6)

        out[0] = b0.toByte()
        out[1] = b1.toByte()
        out[2] = axisToByte(state.leftStickX)
        out[3] = axisToByte(state.leftStickY)
        out[4] = axisToByte(state.rightStickX)
        out[5] = axisToByte(state.rightStickY)
        out[6] = triggerToByte(state.leftTrigger, state.isButtonPressed(ControllerButton.L2))
        out[7] = triggerToByte(state.rightTrigger, state.isButtonPressed(ControllerButton.R2))
        return out
    }

    /** Normalized stick [-1,1] → 0..255 with 128 center. */
    fun axisToByte(normalized: Float): Byte {
        val v = (AXIS_CENTER + normalized.coerceIn(-1f, 1f) * 127f).roundToInt()
            .coerceIn(0, AXIS_MAX)
        return v.toByte()
    }

    fun triggerToByte(analog: Float, digitalPressed: Boolean): Byte {
        val a = analog.coerceIn(0f, 1f)
        val value = when {
            a > 0.01f -> (a * AXIS_MAX).roundToInt()
            digitalPressed -> AXIS_MAX
            else -> 0
        }
        return value.coerceIn(0, AXIS_MAX).toByte()
    }

    fun reportsEqual(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) if (a[i] != b[i]) return false
        return true
    }
}
