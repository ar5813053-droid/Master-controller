package com.dualphone.controller.hid

import com.dualphone.controller.core.GamepadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Receiver-side live input tester.
 *
 * Android limitation (documented):
 * Ordinary apps cannot inject synthetic gamepad HID events into other applications
 * without system privileges, a custom ROM, or a signed accessibility service that
 * performs touch injection. Bluetooth HID peripheral mode is also restricted on
 * standard Android (the phone is typically a central/host, not a HID device).
 *
 * This backend:
 *  - Always updates a live diagnostic GamepadState for the receiver UI.
 *  - Optionally records the last state for future supported injection backends.
 *  - Does NOT claim to control third-party games.
 *
 * Future: a user-enabled AccessibilityService could map sticks/buttons to
 * on-screen touch gestures for specific titles — that path is intentionally
 * left as a clean interface extension, not a fake implementation.
 */
class LiveInputTesterBackend : GameInputBackend {

    private val _latest = MutableStateFlow(GamepadState.EMPTY)
    val latest: StateFlow<GamepadState> = _latest.asStateFlow()

    private val _active = MutableStateFlow(false)
    override val isActive: Boolean get() = _active.value

    fun setActive(active: Boolean) {
        _active.value = active
        if (!active) {
            _latest.value = GamepadState.EMPTY
        }
    }

    override suspend fun inject(state: GamepadState): Result<Unit> {
        if (!_active.value) return Result.failure(IllegalStateException("Backend inactive"))
        _latest.value = state
        return Result.success(Unit)
    }

    override suspend fun releaseAll() {
        _latest.value = GamepadState.EMPTY
    }
}
