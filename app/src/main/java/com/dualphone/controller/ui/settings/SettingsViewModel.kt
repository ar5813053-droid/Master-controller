package com.dualphone.controller.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dualphone.controller.DualPhoneApp
import com.dualphone.controller.core.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = (application as DualPhoneApp).settingsRepository

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setHaptic(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateHaptic(enabled) }
    }

    fun setDeadzone(value: Float) {
        viewModelScope.launch { settingsRepo.updateDeadzone(value) }
    }

    fun setSensitivity(value: Float) {
        viewModelScope.launch { settingsRepo.updateSensitivity(value) }
    }

    fun setButtonOpacity(value: Float) {
        viewModelScope.launch { settingsRepo.updateButtonOpacity(value) }
    }

    fun setLandscapeMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateLandscapeMode(enabled) }
    }

    fun resetDefaults() {
        viewModelScope.launch { settingsRepo.resetToDefaults() }
    }

    fun setVolumeMapping(up: String, down: String) {
        viewModelScope.launch { settingsRepo.updateVolumeMapping(up, down) }
    }
}

