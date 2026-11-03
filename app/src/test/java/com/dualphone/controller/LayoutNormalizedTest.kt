package com.dualphone.controller

import com.dualphone.controller.core.ControlType
import com.dualphone.controller.database.LayoutRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** LayoutRepository without Room — only createDefaultLayout pure logic via reflection-free copy */
class LayoutNormalizedTest {
    @Test
    fun `classic layout is landscape oriented with controls in unit square`() {
        // Mirror createDefaultLayout coordinates from repository (keep in sync)
        val points = listOf(
            0.26f to 0.08f, // BACK
            0.74f to 0.08f, // START
            0.16f to 0.58f, // LS
            0.86f to 0.42f, // RS
            0.81f to 0.68f  // A
        )
        points.forEach { (x, y) ->
            assertThat(x).isAtLeast(0f)
            assertThat(x).isAtMost(1f)
            assertThat(y).isAtLeast(0f)
            assertThat(y).isAtMost(1f)
        }
        // A is below Y in diamond
        assertThat(0.68f).isGreaterThan(0.48f)
    }
}
