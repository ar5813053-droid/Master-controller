package com.dualphone.controller.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dualphone.controller.ui.theme.JoystickBase
import com.dualphone.controller.ui.theme.JoystickKnob
import com.dualphone.controller.ui.theme.JoystickKnobActive
import com.dualphone.controller.ui.theme.SoftGlow
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
    val knobRadius = radius * 0.36f
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
                        if (change == null || change.changedToUp()) break
                        val local = change.position
                        val dx = local.x - radius
                        val dy = local.y - radius
                        val mag = sqrt(dx * dx + dy * dy)
                        val clamped = if (mag > maxTravel && mag > 0f) {
                            Offset(dx / mag * maxTravel, dy / mag * maxTravel)
                        } else Offset(dx, dy)
                        knobOffset = clamped
                        onMove(
                            id,
                            (clamped.x / maxTravel).coerceIn(-1f, 1f),
                            (clamped.y / maxTravel).coerceIn(-1f, 1f)
                        )
                        change.consume()
                    } while (true)
                    knobOffset = Offset.Zero
                    active = false
                    onUp(id)
                    onMove(id, 0f, 0f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            val kR = r * 0.36f
            drawCircle(
                color = SoftGlow.copy(alpha = 0.25f * opacity),
                radius = r * 0.98f,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = JoystickBase.copy(alpha = opacity),
                radius = r * 0.92f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f * opacity),
                radius = r * 0.92f,
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f * opacity),
                radius = r * 0.55f,
                style = Stroke(width = 1.5f)
            )
            val knobColor = if (active) JoystickKnobActive else JoystickKnob
            val cx = this.size.width / 2f + knobOffset.x
            val cy = this.size.height / 2f + knobOffset.y
            drawCircle(
                color = SoftGlow.copy(alpha = 0.2f * opacity),
                radius = kR * 1.15f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = knobColor.copy(alpha = opacity),
                radius = kR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.15f * opacity),
                radius = kR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
        }
    }
}
