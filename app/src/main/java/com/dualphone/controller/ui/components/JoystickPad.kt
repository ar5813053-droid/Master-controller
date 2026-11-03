package com.dualphone.controller.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dualphone.controller.ui.theme.JoystickBase
import com.dualphone.controller.ui.theme.JoystickKnob
import com.dualphone.controller.ui.theme.JoystickKnobActive
import kotlin.math.sqrt

@Composable
fun JoystickPad(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    opacity: Float = 1f,
    onDown: (pointerId: Int) -> Unit,
    onMove: (pointerId: Int, normalizedX: Float, normalizedY: Float) -> Unit,
    onUp: (pointerId: Int) -> Unit
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val radius = sizePx / 2f
    val knobRadius = radius * 0.38f
    val maxTravel = radius - knobRadius

    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var active by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val id = down.id.value.toInt()
                    active = true
                    onDown(id)
                    down.consume()

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id.value.toInt() == id }
                        if (change == null || change.changedToUp()) {
                            break
                        }
                        val local = change.position
                        val dx = local.x - radius
                        val dy = local.y - radius
                        val dist = sqrt(dx * dx + dy * dy)
                        val clamped = if (dist > maxTravel && dist > 0f) {
                            val scale = maxTravel / dist
                            Offset(dx * scale, dy * scale)
                        } else {
                            Offset(dx, dy)
                        }
                        knobOffset = clamped
                        val nx = (clamped.x / maxTravel).coerceIn(-1f, 1f)
                        val ny = (clamped.y / maxTravel).coerceIn(-1f, 1f)
                        onMove(id, nx, ny)
                        change.consume()
                    } while (true)

                    knobOffset = Offset.Zero
                    active = false
                    onUp(id)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Base
            drawCircle(
                color = JoystickBase.copy(alpha = opacity * 0.9f),
                radius = radius * 0.95f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f * opacity),
                radius = radius * 0.95f,
                style = Stroke(width = 2f)
            )
            // Travel ring
            drawCircle(
                color = Color.White.copy(alpha = 0.06f * opacity),
                radius = maxTravel,
                style = Stroke(width = 1.5f)
            )
            // Knob
            val knobColor = if (active) JoystickKnobActive else JoystickKnob
            drawCircle(
                color = knobColor.copy(alpha = opacity),
                radius = knobRadius,
                center = Offset(radius + knobOffset.x, radius + knobOffset.y)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.15f * opacity),
                radius = knobRadius,
                center = Offset(radius + knobOffset.x, radius + knobOffset.y),
                style = Stroke(width = 2f)
            )
        }
    }
}
