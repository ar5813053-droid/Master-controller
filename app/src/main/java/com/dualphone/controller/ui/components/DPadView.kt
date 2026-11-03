package com.dualphone.controller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.input.pointer.changedToUp
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
import com.dualphone.controller.ui.theme.DpadBg
import com.dualphone.controller.ui.theme.TextPrimary

@Composable
fun DPadView(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    opacity: Float = 1f,
    onDown: (ControllerButton, Int) -> Unit,
    onUp: (ControllerButton, Int) -> Unit
) {
    val arm = size * 0.32f
    val center = size * 0.36f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(DpadBg.copy(alpha = opacity * 0.7f))
    ) {
        // Up
        DPadArm(
            label = "▲",
            button = ControllerButton.DPAD_UP,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .size(arm * 1.1f, arm),
            opacity = opacity,
            onDown = onDown,
            onUp = onUp
        )
        // Down
        DPadArm(
            label = "▼",
            button = ControllerButton.DPAD_DOWN,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-4).dp)
                .size(arm * 1.1f, arm),
            opacity = opacity,
            onDown = onDown,
            onUp = onUp
        )
        // Left
        DPadArm(
            label = "◀",
            button = ControllerButton.DPAD_LEFT,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 4.dp)
                .size(arm, arm * 1.1f),
            opacity = opacity,
            onDown = onDown,
            onUp = onUp
        )
        // Right
        DPadArm(
            label = "▶",
            button = ControllerButton.DPAD_RIGHT,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-4).dp)
                .size(arm, arm * 1.1f),
            opacity = opacity,
            onDown = onDown,
            onUp = onUp
        )
        // Center hub
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(center)
                .clip(RoundedCornerShape(6.dp))
                .background(ButtonFace.copy(alpha = opacity * 0.6f))
                .border(1.dp, ButtonOutline.copy(alpha = opacity * 0.5f), RoundedCornerShape(6.dp))
        )
    }
}

@Composable
private fun DPadArm(
    label: String,
    button: ControllerButton,
    modifier: Modifier,
    opacity: Float,
    onDown: (ControllerButton, Int) -> Unit,
    onUp: (ControllerButton, Int) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background((if (pressed) ButtonPressed else ButtonFace).copy(alpha = opacity))
            .border(
                1.dp,
                (if (pressed) AccentBlue else ButtonOutline).copy(alpha = opacity),
                RoundedCornerShape(6.dp)
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
