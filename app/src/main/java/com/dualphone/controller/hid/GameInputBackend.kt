package com.dualphone.controller.hid

import com.dualphone.controller.core.GamepadState

/**
 * Backend that injects received gamepad state into the Android input system or an
 * accessibility / virtual device layer. Placeholder for future milestones.
 */
interface GameInputBackend {
    suspend fun inject(state: GamepadState): Result<Unit>
    suspend fun releaseAll()
    val isActive: Boolean
}
