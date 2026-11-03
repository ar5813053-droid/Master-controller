package com.dualphone.controller.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dualphone.controller.DualPhoneApp
import com.dualphone.controller.core.ConnectionStatus
import com.dualphone.controller.core.DeviceRole
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val deviceRole: DeviceRole = DeviceRole.NONE,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val deviceName: String = android.os.Build.MODEL
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = (application as DualPhoneApp).settingsRepository

    val uiState: StateFlow<HomeUiState> = settingsRepo.deviceRole
        .map { role ->
            HomeUiState(
                deviceRole = role,
                connectionStatus = ConnectionStatus.DISCONNECTED,
                deviceName = android.os.Build.MODEL
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )

    fun setRole(role: DeviceRole) {
        viewModelScope.launch {
            settingsRepo.setDeviceRole(role)
        }
    }
}
