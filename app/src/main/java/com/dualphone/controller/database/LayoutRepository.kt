package com.dualphone.controller.database

import com.dualphone.controller.core.ControllerLayout
import com.dualphone.controller.core.ControlPosition
import com.dualphone.controller.core.ControlType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LayoutRepository(private val dao: LayoutDao) {

    val allLayouts: Flow<List<ControllerLayout>> = dao.getAllLayouts().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getLayout(id: String): ControllerLayout? = dao.getLayoutById(id)?.toDomain()

    suspend fun saveLayout(layout: ControllerLayout) {
        dao.insert(LayoutEntity.fromDomain(layout))
    }

    suspend fun deleteLayout(id: String) = dao.delete(id)

    fun createDefaultLayout(): ControllerLayout {
        // Default landscape layout positions (normalized 0-1 relative to canvas)
        val controls = listOf(
            ControlPosition("left_stick", 0.12f, 0.55f, 0.18f, 0.32f, 1f, ControlType.LEFT_STICK),
            ControlPosition("right_stick", 0.70f, 0.55f, 0.18f, 0.32f, 1f, ControlType.RIGHT_STICK),
            ControlPosition("dpad", 0.08f, 0.18f, 0.16f, 0.28f, 1f, ControlType.DPAD),
            ControlPosition("btn_y", 0.78f, 0.12f, 0.08f, 0.12f, 1f, ControlType.BUTTON_Y),
            ControlPosition("btn_x", 0.70f, 0.22f, 0.08f, 0.12f, 1f, ControlType.BUTTON_X),
            ControlPosition("btn_b", 0.86f, 0.22f, 0.08f, 0.12f, 1f, ControlType.BUTTON_B),
            ControlPosition("btn_a", 0.78f, 0.32f, 0.08f, 0.12f, 1f, ControlType.BUTTON_A),
            ControlPosition("btn_l1", 0.18f, 0.05f, 0.12f, 0.08f, 1f, ControlType.BUTTON_L1),
            ControlPosition("btn_l2", 0.18f, 0.00f, 0.12f, 0.08f, 1f, ControlType.BUTTON_L2),
            ControlPosition("btn_r1", 0.70f, 0.05f, 0.12f, 0.08f, 1f, ControlType.BUTTON_R1),
            ControlPosition("btn_r2", 0.70f, 0.00f, 0.12f, 0.08f, 1f, ControlType.BUTTON_R2),
            ControlPosition("btn_select", 0.40f, 0.08f, 0.08f, 0.07f, 1f, ControlType.BUTTON_SELECT),
            ControlPosition("btn_start", 0.52f, 0.08f, 0.08f, 0.07f, 1f, ControlType.BUTTON_START),
            ControlPosition("btn_home", 0.46f, 0.18f, 0.08f, 0.07f, 1f, ControlType.BUTTON_HOME)
        )
        return ControllerLayout(
            layoutId = "default",
            layoutName = "Default Landscape",
            controls = controls,
            opacity = 1f,
            orientation = "landscape"
        )
    }

    suspend fun ensureDefaultExists() {
        if (dao.getLayoutById("default") == null) {
            saveLayout(createDefaultLayout())
        }
    }

    fun newLayoutFromDefault(name: String = "Custom Layout"): ControllerLayout {
        val base = createDefaultLayout()
        return base.copy(
            layoutId = UUID.randomUUID().toString(),
            layoutName = name
        )
    }
}
