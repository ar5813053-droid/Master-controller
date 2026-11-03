package com.dualphone.controller.hid

import android.util.Log
import com.dualphone.controller.accessibility.TouchMappingAccessibilityService
import com.dualphone.controller.accessibility.TouchMappingProfile
import com.dualphone.controller.core.GamepadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Routes gamepad state into AccessibilityService touch gestures.
 * Requires the user to enable TouchMappingAccessibilityService in system settings.
 */
class AccessibilityTouchMappingBackend : GameInputBackend {

    private val _active = MutableStateFlow(false)
    override val isActive: Boolean get() = _active.value && TouchMappingAccessibilityService.isEnabled()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _serviceEnabled = MutableStateFlow(false)
    val serviceEnabled: StateFlow<Boolean> = _serviceEnabled.asStateFlow()

    @Volatile
    var profile: TouchMappingProfile = TouchMappingProfile()

    fun setEnabled(enabled: Boolean) {
        _active.value = enabled
        refreshServiceState()
        if (!enabled) {
            TouchMappingAccessibilityService.instance?.releaseAllTouches()
        }
    }

    fun refreshServiceState() {
        _serviceEnabled.value = TouchMappingAccessibilityService.isEnabled()
    }

    override suspend fun inject(state: GamepadState): Result<Unit> {
        refreshServiceState()
        if (!_active.value) {
            return Result.failure(IllegalStateException("Touch mapping backend disabled"))
        }
        val svc = TouchMappingAccessibilityService.instance
        if (svc == null) {
            _lastError.value = "Accessibility service not enabled"
            return Result.failure(IllegalStateException("Enable DualPhone Touch Mapping in Accessibility settings"))
        }
        return try {
            svc.applyState(state, profile)
            _lastError.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "inject failed: ${e.message}")
            _lastError.value = e.message
            Result.failure(e)
        }
    }

    override suspend fun releaseAll() {
        TouchMappingAccessibilityService.instance?.releaseAllTouches()
    }

    companion object {
        private const val TAG = "A11yTouchBackend"
    }
}
