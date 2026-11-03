package com.dualphone.controller.core

/**
 * Hardware volume key → virtual trigger mapping for Controller Mode only.
 */
enum class TriggerTarget {
    DISABLED,
    LT,
    RT,
    LT_RT
}

data class VolumeButtonMapping(
    val volumeUp: TriggerTarget = TriggerTarget.DISABLED,
    val volumeDown: TriggerTarget = TriggerTarget.DISABLED
) {
    val isEnabled: Boolean
        get() = volumeUp != TriggerTarget.DISABLED || volumeDown != TriggerTarget.DISABLED

    companion object {
        val SEPARATE = VolumeButtonMapping(TriggerTarget.LT, TriggerTarget.RT)
        val REVERSE = VolumeButtonMapping(TriggerTarget.RT, TriggerTarget.LT)
        val BOTH_ON_UP = VolumeButtonMapping(TriggerTarget.LT_RT, TriggerTarget.DISABLED)
        val SINGLE_LT = VolumeButtonMapping(TriggerTarget.LT, TriggerTarget.DISABLED)
        val SINGLE_RT = VolumeButtonMapping(TriggerTarget.RT, TriggerTarget.DISABLED)
        val BOTH_KEYS_BOTH = VolumeButtonMapping(TriggerTarget.LT_RT, TriggerTarget.LT_RT)
        val DISABLED = VolumeButtonMapping(TriggerTarget.DISABLED, TriggerTarget.DISABLED)
    }
}

fun TriggerTarget.toButtons(): List<ControllerButton> = when (this) {
    TriggerTarget.DISABLED -> emptyList()
    TriggerTarget.LT -> listOf(ControllerButton.L2)
    TriggerTarget.RT -> listOf(ControllerButton.R2)
    TriggerTarget.LT_RT -> listOf(ControllerButton.L2, ControllerButton.R2)
}
