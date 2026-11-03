package com.dualphone.controller.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dualphone.controller.DualPhoneApp
import com.dualphone.controller.core.ControlPosition
import com.dualphone.controller.core.ControlType
import com.dualphone.controller.core.ControllerLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LayoutEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val layoutRepo = (application as DualPhoneApp).layoutRepository
    private val settingsRepo = (application as DualPhoneApp).settingsRepository

    private val _layout = MutableStateFlow(layoutRepo.createDefaultLayout())
    val layout: StateFlow<ControllerLayout> = _layout.asStateFlow()

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    init {
        viewModelScope.launch {
            layoutRepo.ensureDefaultExists()
            val loaded = layoutRepo.getLayout("default") ?: layoutRepo.createDefaultLayout()
            _layout.value = loaded
        }
    }

    fun select(id: String) {
        _selectedId.value = id
    }

    fun nudgeSelected(dx: Float, dy: Float) {
        val id = _selectedId.value ?: return
        _layout.update { layout ->
            layout.copy(
                controls = layout.controls.map { c ->
                    if (c.controlId != id) c
                    else c.copy(
                        x = (c.x + dx).coerceIn(0.02f, 0.98f),
                        y = (c.y + dy).coerceIn(0.02f, 0.98f)
                    )
                }
            )
        }
    }

    fun resizeSelected(width: Float) {
        val id = _selectedId.value ?: return
        _layout.update { layout ->
            layout.copy(
                controls = layout.controls.map { c ->
                    if (c.controlId != id) c
                    else {
                        val aspect = if (c.height > 0) c.width / c.height else 1f
                        val w = width.coerceIn(0.05f, 0.35f)
                        c.copy(width = w, height = (w / aspect).coerceIn(0.05f, 0.4f))
                    }
                }
            )
        }
    }

    fun setSelectedOpacity(opacity: Float) {
        val id = _selectedId.value ?: return
        _layout.update { layout ->
            layout.copy(
                controls = layout.controls.map {
                    if (it.controlId == id) it.copy(opacity = opacity.coerceIn(0.3f, 1f)) else it
                }
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            val current = _layout.value.copy(layoutId = "default", layoutName = "Classic Reference")
            layoutRepo.saveLayout(current)
            settingsRepo.setActiveLayoutId("default")
            _layout.value = current
        }
    }

    fun resetToDefault() {
        _layout.value = layoutRepo.createDefaultLayout()
        _selectedId.value = null
    }

    fun loadPresetDualStick() {
        val base = layoutRepo.createDefaultLayout()
        // Dual stick: left lower, right lower-right, ABXY upper-right of RS
        val controls = listOf(
            ControlPosition("btn_select", 0.40f, 0.08f, 0.10f, 0.08f, 1f, ControlType.BUTTON_SELECT),
            ControlPosition("btn_start", 0.60f, 0.08f, 0.10f, 0.08f, 1f, ControlType.BUTTON_START),
            ControlPosition("btn_l2", 0.12f, 0.18f, 0.09f, 0.10f, 1f, ControlType.BUTTON_L2),
            ControlPosition("btn_l1", 0.18f, 0.26f, 0.09f, 0.10f, 1f, ControlType.BUTTON_L1),
            ControlPosition("btn_r2", 0.88f, 0.18f, 0.09f, 0.10f, 1f, ControlType.BUTTON_R2),
            ControlPosition("btn_r1", 0.82f, 0.26f, 0.09f, 0.10f, 1f, ControlType.BUTTON_R1),
            ControlPosition("stick_left", 0.20f, 0.65f, 0.22f, 0.35f, 1f, ControlType.LEFT_STICK),
            ControlPosition("stick_right", 0.80f, 0.65f, 0.22f, 0.35f, 1f, ControlType.RIGHT_STICK),
            ControlPosition("dpad", 0.20f, 0.35f, 0.14f, 0.22f, 1f, ControlType.DPAD),
            ControlPosition("btn_y", 0.80f, 0.28f, 0.08f, 0.12f, 1f, ControlType.BUTTON_Y),
            ControlPosition("btn_x", 0.72f, 0.38f, 0.08f, 0.12f, 1f, ControlType.BUTTON_X),
            ControlPosition("btn_b", 0.88f, 0.38f, 0.08f, 0.12f, 1f, ControlType.BUTTON_B),
            ControlPosition("btn_a", 0.80f, 0.48f, 0.08f, 0.12f, 1f, ControlType.BUTTON_A),
            ControlPosition("btn_home", 0.50f, 0.12f, 0.07f, 0.08f, 1f, ControlType.BUTTON_HOME)
        )
        _layout.value = base.copy(layoutName = "Dual Joystick", controls = controls)
        _selectedId.value = null
    }
}
