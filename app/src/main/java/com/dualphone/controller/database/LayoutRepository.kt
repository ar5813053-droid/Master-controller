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
        // Classic reference (normalized center x,y + width/height on 16:9 canvas)
        // RS is bottom-right so it does not cover ABXY diamond.
        val controls = listOf(
            ControlPosition("btn_select", 0.28f, 0.07f, 0.09f, 0.09f, 1f, ControlType.BUTTON_SELECT),
            ControlPosition("btn_start", 0.72f, 0.07f, 0.09f, 0.09f, 1f, ControlType.BUTTON_START),
            ControlPosition("btn_home", 0.50f, 0.07f, 0.07f, 0.09f, 1f, ControlType.BUTTON_HOME),
            ControlPosition("btn_l2", 0.11f, 0.20f, 0.08f, 0.11f, 1f, ControlType.BUTTON_L2),
            ControlPosition("btn_l1", 0.17f, 0.30f, 0.08f, 0.11f, 1f, ControlType.BUTTON_L1),
            ControlPosition("btn_r2", 0.89f, 0.20f, 0.08f, 0.11f, 1f, ControlType.BUTTON_R2),
            ControlPosition("btn_r1", 0.83f, 0.30f, 0.08f, 0.11f, 1f, ControlType.BUTTON_R1),
            ControlPosition("stick_left", 0.15f, 0.62f, 0.22f, 0.36f, 1f, ControlType.LEFT_STICK),
            ControlPosition("dpad", 0.36f, 0.68f, 0.15f, 0.24f, 1f, ControlType.DPAD),
            ControlPosition("btn_y", 0.80f, 0.48f, 0.075f, 0.12f, 1f, ControlType.BUTTON_Y),
            ControlPosition("btn_x", 0.72f, 0.58f, 0.075f, 0.12f, 1f, ControlType.BUTTON_X),
            ControlPosition("btn_b", 0.88f, 0.58f, 0.075f, 0.12f, 1f, ControlType.BUTTON_B),
            ControlPosition("btn_a", 0.80f, 0.68f, 0.075f, 0.12f, 1f, ControlType.BUTTON_A),
            ControlPosition("stick_right", 0.90f, 0.82f, 0.14f, 0.22f, 1f, ControlType.RIGHT_STICK)
        )
        return ControllerLayout(
            layoutId = "default",
            layoutName = "Classic Reference",
            controls = controls,
            opacity = 1f,
            orientation = "landscape"
        )
    }

    suspend fun ensureDefaultExists() {
        // Always refresh classic reference so UI matches the design after updates
        saveLayout(createDefaultLayout())
    }

    fun newLayoutFromDefault(name: String = "Custom Layout"): ControllerLayout {
        val base = createDefaultLayout()
        return base.copy(
            layoutId = UUID.randomUUID().toString(),
            layoutName = name
        )
    }
}
