package com.dualphone.controller

import com.dualphone.controller.core.ControlPosition
import com.dualphone.controller.core.ControlType
import com.dualphone.controller.core.ControllerLayout
import com.dualphone.controller.database.LayoutEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class LayoutSerializationTest {

    @Test
    fun `round trip ControllerLayout via JSON`() {
        val original = ControllerLayout(
            layoutId = "test-1",
            layoutName = "Test Layout",
            controls = listOf(
                ControlPosition("left", 0.1f, 0.5f, 0.15f, 0.25f, 0.9f, ControlType.LEFT_STICK),
                ControlPosition("a", 0.8f, 0.6f, 0.08f, 0.12f, 1f, ControlType.BUTTON_A)
            ),
            opacity = 0.95f,
            orientation = "landscape"
        )
        val json = Json.encodeToString(original)
        val restored = Json.decodeFromString<ControllerLayout>(json)
        assertThat(restored.layoutId).isEqualTo(original.layoutId)
        assertThat(restored.layoutName).isEqualTo(original.layoutName)
        assertThat(restored.controls).hasSize(2)
        assertThat(restored.controls[0].controlType).isEqualTo(ControlType.LEFT_STICK)
        assertThat(restored.opacity).isEqualTo(0.95f)
    }

    @Test
    fun `LayoutEntity domain conversion preserves data`() {
        val domain = ControllerLayout(
            layoutId = "def",
            layoutName = "Default",
            controls = listOf(
                ControlPosition("dpad", 0.05f, 0.2f, 0.14f, 0.22f, 1f, ControlType.DPAD)
            ),
            opacity = 1f,
            orientation = "landscape"
        )
        val entity = LayoutEntity.fromDomain(domain)
        val back = entity.toDomain()
        assertThat(back.layoutId).isEqualTo(domain.layoutId)
        assertThat(back.controls[0].controlId).isEqualTo("dpad")
        assertThat(back.controls[0].x).isWithin(0.001f).of(0.05f)
    }
}
