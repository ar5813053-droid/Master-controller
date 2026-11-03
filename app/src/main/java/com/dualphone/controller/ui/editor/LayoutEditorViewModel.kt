package com.dualphone.controller.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dualphone.controller.DualPhoneApp
import com.dualphone.controller.core.ControlPosition
import com.dualphone.controller.core.ControllerLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class EditorUiState(
    val layout: ControllerLayout? = null,
    val selectedControlId: String? = null,
    val isDirty: Boolean = false,
    val message: String? = null
)

class LayoutEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val layoutRepo = (application as DualPhoneApp).layoutRepository
    private val settingsRepo = (application as DualPhoneApp).settingsRepository

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            layoutRepo.ensureDefaultExists()
            val layout = layoutRepo.getLayout("default") ?: layoutRepo.createDefaultLayout()
            _uiState.update { it.copy(layout = layout) }
        }
    }

    fun selectControl(id: String?) {
        _uiState.update { it.copy(selectedControlId = id) }
    }

    fun moveControl(id: String, newX: Float, newY: Float) {
        _uiState.update { state ->
            val layout = state.layout ?: return@update state
            val updated = layout.controls.map {
                if (it.controlId == id) {
                    it.copy(
                        x = newX.coerceIn(0f, 1f - it.width),
                        y = newY.coerceIn(0f, 1f - it.height)
                    )
                } else it
            }
            state.copy(layout = layout.copy(controls = updated), isDirty = true)
        }
    }

    fun resizeControl(id: String, newWidth: Float, newHeight: Float) {
        _uiState.update { state ->
            val layout = state.layout ?: return@update state
            val updated = layout.controls.map {
                if (it.controlId == id) {
                    val w = newWidth.coerceIn(0.04f, 0.4f)
                    val h = newHeight.coerceIn(0.04f, 0.4f)
                    it.copy(
                        width = w,
                        height = h,
                        x = it.x.coerceIn(0f, 1f - w),
                        y = it.y.coerceIn(0f, 1f - h)
                    )
                } else it
            }
            state.copy(layout = layout.copy(controls = updated), isDirty = true)
        }
    }

    fun setControlOpacity(id: String, opacity: Float) {
        _uiState.update { state ->
            val layout = state.layout ?: return@update state
            val updated = layout.controls.map {
                if (it.controlId == id) it.copy(opacity = opacity.coerceIn(0.2f, 1f))
                else it
            }
            state.copy(layout = layout.copy(controls = updated), isDirty = true)
        }
    }

    fun setGlobalOpacity(opacity: Float) {
        _uiState.update { state ->
            val layout = state.layout ?: return@update state
            state.copy(
                layout = layout.copy(opacity = opacity.coerceIn(0.2f, 1f)),
                isDirty = true
            )
        }
    }

    fun resetToDefault() {
        val default = layoutRepo.createDefaultLayout()
        _uiState.update {
            it.copy(layout = default, isDirty = true, selectedControlId = null, message = "Reset to default")
        }
    }

    fun saveLayout() {
        val layout = _uiState.value.layout ?: return
        viewModelScope.launch {
            val toSave = if (layout.layoutId == "default") {
                layout.copy(
                    layoutId = UUID.randomUUID().toString(),
                    layoutName = "Custom ${System.currentTimeMillis() % 10000}"
                )
            } else layout
            layoutRepo.saveLayout(toSave)
            settingsRepo.setActiveLayoutId(toSave.layoutId)
            _uiState.update {
                it.copy(layout = toSave, isDirty = false, message = "Layout saved")
            }
        }
    }

    fun loadDefault() {
        viewModelScope.launch {
            val layout = layoutRepo.getLayout("default") ?: layoutRepo.createDefaultLayout()
            _uiState.update {
                it.copy(layout = layout, isDirty = false, selectedControlId = null, message = "Default loaded")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
