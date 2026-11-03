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
        // Classic reference — normalized 16:9 landscape (x,y = center; width/height fractions of canvas)
        val controls = listOf(
            ControlPosition("btn_select", 0.26f, 0.08f, 0.10f, 0.08f, 1f, ControlType.BUTTON_SELECT),
            ControlPosition("btn_start", 0.74f, 0.08f, 0.10f, 0.08f, 1f, ControlType.BUTTON_START),
            ControlPosition("btn_l2", 0.12f, 0.20f, 0.09f, 0.10f, 1f, ControlType.BUTTON_L2),
            ControlPosition("btn_l1", 0.18f, 0.28f, 0.09f, 0.10f, 1f, ControlType.BUTTON_L1),
            ControlPosition("btn_r2", 0.88f, 0.20f, 0.09f, 0.10f, 1f, ControlType.BUTTON_R2),
            ControlPosition("btn_r1", 0.82f, 0.28f, 0.09f, 0.10f, 1f, ControlType.BUTTON_R1),
            ControlPosition("stick_left", 0.16f, 0.58f, 0.20f, 0.32f, 1f, ControlType.LEFT_STICK),
            ControlPosition("stick_right", 0.86f, 0.42f, 0.16f, 0.26f, 1f, ControlType.RIGHT_STICK),
            ControlPosition("dpad", 0.34f, 0.62f, 0.16f, 0.26f, 1f, ControlType.DPAD),
            ControlPosition("btn_y", 0.81f, 0.48f, 0.08f, 0.13f, 1f, ControlType.BUTTON_Y),
            ControlPosition("btn_x", 0.73f, 0.58f, 0.08f, 0.13f, 1f, ControlType.BUTTON_X),
            ControlPosition("btn_b", 0.89f, 0.58f, 0.08f, 0.13f, 1f, ControlType.BUTTON_B),
            ControlPosition("btn_a", 0.81f, 0.68f, 0.08f, 0.13f, 1f, ControlType.BUTTON_A),
            ControlPosition("btn_home", 0.50f, 0.12f, 0.07f, 0.08f, 1f, ControlType.BUTTON_HOME)
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
