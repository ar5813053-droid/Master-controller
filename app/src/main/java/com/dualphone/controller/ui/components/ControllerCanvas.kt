package com.dualphone.controller.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dualphone.controller.core.ControlPosition
import com.dualphone.controller.core.ControlType
import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.core.ControllerLayout

private val Glow = Color(0x44FFFFFF)
private val Outline = Color(0x66FFFFFF)

/**
 * Shared landscape controller renderer used by Controller Mode and Layout Editor.
 * Positions are normalized (0–1) against a 16:9 design canvas.
 */
@Composable
fun ControllerCanvas(
    layout: ControllerLayout,
    opacity: Float,
    interactive: Boolean,
    selectedId: String? = null,
    onSelect: ((String) -> Unit)? = null,
    onLeftStickDown: ((Int) -> Unit)? = null,
    onLeftStickMove: ((Int, Float, Float) -> Unit)? = null,
    onLeftStickUp: ((Int) -> Unit)? = null,
    onRightStickDown: ((Int) -> Unit)? = null,
    onRightStickMove: ((Int, Float, Float) -> Unit)? = null,
    onRightStickUp: ((Int) -> Unit)? = null,
    onButtonDown: ((ControllerButton, Int) -> Unit)? = null,
    onButtonUp: ((ControllerButton, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val W = maxWidth
        val H = maxHeight
        layout.controls.forEach { c ->
            val w = W * c.width
            val h = H * c.height
            val x = W * c.x - w / 2
            val y = H * c.y - h / 2
            val sizeDp = minOf(w, h)
            val selected = c.controlId == selectedId
            Box(
                Modifier
                    .offset(x, y)
                    .size(w, h)
                    .then(
                        if (selected) Modifier.border(2.dp, Color(0xFF4FC3F7))
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (c.controlType) {
                    ControlType.LEFT_STICK -> {
                        if (interactive) {
                            JoystickPad(
                                size = sizeDp,
                                opacity = opacity * c.opacity,
                                onDown = { onLeftStickDown?.invoke(it) },
                                onMove = { id, nx, ny -> onLeftStickMove?.invoke(id, nx, ny) },
                                onUp = { onLeftStickUp?.invoke(it) }
                            )
                        } else {
                            JoystickPad(
                                size = sizeDp,
                                opacity = opacity * c.opacity,
                                onDown = {},
                                onMove = { _, _, _ -> },
                                onUp = {}
                            )
                        }
                    }
                    ControlType.RIGHT_STICK -> {
                        if (interactive) {
                            JoystickPad(
                                size = sizeDp,
                                opacity = opacity * c.opacity,
                                onDown = { onRightStickDown?.invoke(it) },
                                onMove = { id, nx, ny -> onRightStickMove?.invoke(id, nx, ny) },
                                onUp = { onRightStickUp?.invoke(it) }
                            )
                        } else {
                            JoystickPad(
                                size = sizeDp,
                                opacity = opacity * c.opacity,
                                onDown = {},
                                onMove = { _, _, _ -> },
                                onUp = {}
                            )
                        }
                    }
                    ControlType.DPAD -> {
                        DPadView(
                            size = sizeDp,
                            opacity = opacity * c.opacity,
                            onDown = { b, id -> if (interactive) onButtonDown?.invoke(b, id) },
                            onUp = { b, id -> if (interactive) onButtonUp?.invoke(b, id) }
                        )
                    }
                    else -> {
                        val (btn, label, round) = mapButton(c.controlType)
                        Box(
                            Modifier
                                .size(sizeDp)
                                .shadow(4.dp, if (round) CircleShape else CircleShape, ambientColor = Glow, spotColor = Glow)
                                .border(
                                    1.dp,
                                    if (selected) Color(0xFF4FC3F7) else Outline.copy(alpha = opacity),
                                    if (round) CircleShape else CircleShape
                                )
                        ) {
                            ControllerButtonView(
                                button = btn,
                                label = label,
                                size = sizeDp * 0.95f,
                                opacity = opacity * c.opacity,
                                isRound = round,
                                onDown = { b, id -> if (interactive) onButtonDown?.invoke(b, id) },
                                onUp = { b, id -> if (interactive) onButtonUp?.invoke(b, id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun mapButton(type: ControlType): Triple<ControllerButton, String, Boolean> = when (type) {
    ControlType.BUTTON_A -> Triple(ControllerButton.A, "A", true)
    ControlType.BUTTON_B -> Triple(ControllerButton.B, "B", true)
    ControlType.BUTTON_X -> Triple(ControllerButton.X, "X", true)
    ControlType.BUTTON_Y -> Triple(ControllerButton.Y, "Y", true)
    ControlType.BUTTON_L1 -> Triple(ControllerButton.L1, "LB", false)
    ControlType.BUTTON_L2 -> Triple(ControllerButton.L2, "LT", false)
    ControlType.BUTTON_R1 -> Triple(ControllerButton.R1, "RB", false)
    ControlType.BUTTON_R2 -> Triple(ControllerButton.R2, "RT", false)
    ControlType.BUTTON_START -> Triple(ControllerButton.START, "START", false)
    ControlType.BUTTON_SELECT -> Triple(ControllerButton.SELECT, "BACK", false)
    ControlType.BUTTON_HOME -> Triple(ControllerButton.HOME, "⌂", true)
    else -> Triple(ControllerButton.A, "?", true)
}
