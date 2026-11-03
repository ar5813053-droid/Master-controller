package com.dualphone.controller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.ui.theme.AccentBlue
import com.dualphone.controller.ui.theme.ButtonFace
import com.dualphone.controller.ui.theme.ButtonOutline
import com.dualphone.controller.ui.theme.ButtonPressed
import com.dualphone.controller.ui.theme.TextPrimary

@Composable
fun ControllerButtonView(
    button: ControllerButton,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    opacity: Float = 1f,
    isRound: Boolean = true,
    onDown: (ControllerButton, Int) -> Unit,
    onUp: (ControllerButton, Int) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val shape = if (isRound) CircleShape else RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                (if (pressed) ButtonPressed else ButtonFace).copy(alpha = opacity)
            )
            .border(
                width = 1.5.dp,
                color = (if (pressed) AccentBlue else ButtonOutline).copy(alpha = opacity),
                shape = shape
            )
            .pointerInput(button) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val id = down.id.value.toInt()
                    pressed = true
                    onDown(button, id)
                    down.consume()

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id.value.toInt() == id }
                        if (change == null || change.changedToUp()) break
                        change.consume()
                    } while (true)

                    pressed = false
                    onUp(button, id)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextPrimary.copy(alpha = opacity),
            fontSize = (size.value * 0.32f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}
