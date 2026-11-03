package com.dualphone.controller.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dualphone.controller.core.AppSettings
import com.dualphone.controller.core.DeviceRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dualphone_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val HAPTIC = booleanPreferencesKey("haptic_enabled")
        val DEADZONE = floatPreferencesKey("joystick_deadzone")
        val SENSITIVITY = floatPreferencesKey("joystick_sensitivity")
        val BUTTON_OPACITY = floatPreferencesKey("button_opacity")
        val LANDSCAPE = booleanPreferencesKey("landscape_mode")
        val DEVICE_ROLE = stringPreferencesKey("device_role")
        val ACTIVE_LAYOUT_ID = stringPreferencesKey("active_layout_id")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            hapticEnabled = prefs[Keys.HAPTIC] ?: true,
            joystickDeadzone = prefs[Keys.DEADZONE] ?: 0.12f,
            joystickSensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f,
            buttonOpacity = prefs[Keys.BUTTON_OPACITY] ?: 0.85f,
            landscapeMode = prefs[Keys.LANDSCAPE] ?: true
        )
    }

    val deviceRole: Flow<DeviceRole> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.DEVICE_ROLE]) {
            "CONTROLLER" -> DeviceRole.CONTROLLER
            "GAMING_RECEIVER" -> DeviceRole.GAMING_RECEIVER
            else -> DeviceRole.NONE
        }
    }

    val activeLayoutId: Flow<String?> = context.dataStore.data.map { it[Keys.ACTIVE_LAYOUT_ID] }

    suspend fun updateHaptic(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC] = enabled }
    }

    suspend fun updateDeadzone(value: Float) {
        context.dataStore.edit { it[Keys.DEADZONE] = value.coerceIn(0f, 0.5f) }
    }

    suspend fun updateSensitivity(value: Float) {
        context.dataStore.edit { it[Keys.SENSITIVITY] = value.coerceIn(0.5f, 2.0f) }
    }

    suspend fun updateButtonOpacity(value: Float) {
        context.dataStore.edit { it[Keys.BUTTON_OPACITY] = value.coerceIn(0.2f, 1f) }
    }

    suspend fun updateLandscapeMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LANDSCAPE] = enabled }
    }

    suspend fun setDeviceRole(role: DeviceRole) {
        context.dataStore.edit {
            it[Keys.DEVICE_ROLE] = when (role) {
                DeviceRole.CONTROLLER -> "CONTROLLER"
                DeviceRole.GAMING_RECEIVER -> "GAMING_RECEIVER"
                DeviceRole.NONE -> "NONE"
            }
        }
    }

    suspend fun setActiveLayoutId(id: String?) {
        context.dataStore.edit {
            if (id == null) it.remove(Keys.ACTIVE_LAYOUT_ID)
            else it[Keys.ACTIVE_LAYOUT_ID] = id
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTIC] = true
            prefs[Keys.DEADZONE] = 0.12f
            prefs[Keys.SENSITIVITY] = 1.0f
            prefs[Keys.BUTTON_OPACITY] = 0.85f
            prefs[Keys.LANDSCAPE] = true
        }
    }
}
