package com.dualphone.controller.hid

import com.dualphone.controller.core.GamepadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BackendMode {
    TESTER_ONLY,
    ACCESSIBILITY_TOUCH
}

/**
 * Always feeds LiveInputTester; optionally also Accessibility touch mapping.
 */
class CompositeInputBackend(
    val tester: LiveInputTesterBackend,
    val accessibility: AccessibilityTouchMappingBackend
) : GameInputBackend {

    private val _mode = MutableStateFlow(BackendMode.TESTER_ONLY)
    val mode: StateFlow<BackendMode> = _mode.asStateFlow()

    fun setMode(mode: BackendMode) {
        _mode.value = mode
        when (mode) {
            BackendMode.TESTER_ONLY -> accessibility.setEnabled(false)
            BackendMode.ACCESSIBILITY_TOUCH -> accessibility.setEnabled(true)
        }
    }

    override val isActive: Boolean
        get() = when (_mode.value) {
            BackendMode.TESTER_ONLY -> tester.isActive
            BackendMode.ACCESSIBILITY_TOUCH -> accessibility.isActive || tester.isActive
        }

    override suspend fun inject(state: GamepadState): Result<Unit> {
        tester.inject(state)
        return when (_mode.value) {
            BackendMode.TESTER_ONLY -> Result.success(Unit)
            BackendMode.ACCESSIBILITY_TOUCH -> accessibility.inject(state)
        }
    }

    override suspend fun releaseAll() {
        tester.releaseAll()
        accessibility.releaseAll()
    }
}
