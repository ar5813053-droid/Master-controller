package com.dualphone.controller.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dualphone.controller.core.ControlPosition
import com.dualphone.controller.core.ControlType
import com.dualphone.controller.ui.theme.AccentBlue
import com.dualphone.controller.ui.theme.AccentCyan
import com.dualphone.controller.ui.theme.ButtonFace
import com.dualphone.controller.ui.theme.ButtonOutline
import com.dualphone.controller.ui.theme.Charcoal
import com.dualphone.controller.ui.theme.DarkSurface
import com.dualphone.controller.ui.theme.TextPrimary
import com.dualphone.controller.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun LayoutEditorScreen(
    onBack: () -> Unit,
    viewModel: LayoutEditorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val layout = uiState.layout

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal)
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "Layout Editor",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.resetToDefault() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = TextSecondary)
            }
            IconButton(onClick = { viewModel.saveLayout() }) {
                Icon(Icons.Default.Save, contentDescription = "Save", tint = AccentBlue)
            }
        }

        if (uiState.message != null) {
            Text(
                text = uiState.message!!,
                color = AccentCyan,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Canvas
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, ButtonOutline, RoundedCornerShape(12.dp))
        ) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()
            val density = LocalDensity.current

            if (layout != null) {
                layout.controls.forEach { control ->
                    EditableControl(
                        control = control,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        isSelected = control.controlId == uiState.selectedControlId,
                        onSelect = { viewModel.selectControl(control.controlId) },
                        onMove = { dx, dy ->
                            val newX = control.x + dx / canvasWidth
                            val newY = control.y + dy / canvasHeight
                            viewModel.moveControl(control.controlId, newX, newY)
                        }
                    )
                }
            } else {
                Text(
                    text = "Loading layout…",
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Bottom controls for selected item
        val selected = layout?.controls?.find { it.controlId == uiState.selectedControlId }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(16.dp)
        ) {
            if (selected != null) {
                Text(
                    text = "Selected: ${selected.controlType.name}",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Size", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("W", color = TextSecondary, modifier = Modifier.width(20.dp))
                    Slider(
                        value = selected.width,
                        onValueChange = {
                            viewModel.resizeControl(selected.controlId, it, selected.height)
                        },
                        valueRange = 0.05f..0.35f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("H", color = TextSecondary, modifier = Modifier.width(20.dp))
                    Slider(
                        value = selected.height,
                        onValueChange = {
                            viewModel.resizeControl(selected.controlId, selected.width, it)
                        },
                        valueRange = 0.05f..0.35f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Opacity", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = selected.opacity,
                    onValueChange = { viewModel.setControlOpacity(selected.controlId, it) },
                    valueRange = 0.2f..1f
                )
            } else {
                Text(
                    text = "Tap a control to select · Drag to reposition",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = { viewModel.loadDefault() },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonFace)
                    ) {
                        Text("Load Default")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.saveLayout() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Save Layout")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableControl(
    control: ControlPosition,
    canvasWidth: Float,
    canvasHeight: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMove: (dx: Float, dy: Float) -> Unit
) {
    val xPx = control.x * canvasWidth
    val yPx = control.y * canvasHeight
    val wPx = control.width * canvasWidth
    val hPx = control.height * canvasHeight

    Box(
        modifier = Modifier
            .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
            .size(
                width = with(LocalDensity.current) { wPx.toDp() },
                height = with(LocalDensity.current) { hPx.toDp() }
            )
            .clip(RoundedCornerShape(8.dp))
            .background(ButtonFace.copy(alpha = control.opacity * 0.85f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AccentBlue else ButtonOutline,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(control.controlId) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onMove(dragAmount.x, dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shortLabel(control.controlType),
            color = TextPrimary.copy(alpha = control.opacity),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun shortLabel(type: ControlType): String = when (type) {
    ControlType.LEFT_STICK -> "L-Stick"
    ControlType.RIGHT_STICK -> "R-Stick"
    ControlType.DPAD -> "D-Pad"
    ControlType.BUTTON_A -> "A"
    ControlType.BUTTON_B -> "B"
    ControlType.BUTTON_X -> "X"
    ControlType.BUTTON_Y -> "Y"
    ControlType.BUTTON_L1 -> "L1"
    ControlType.BUTTON_L2 -> "L2"
    ControlType.BUTTON_R1 -> "R1"
    ControlType.BUTTON_R2 -> "R2"
    ControlType.BUTTON_START -> "Start"
    ControlType.BUTTON_SELECT -> "Select"
    ControlType.BUTTON_HOME -> "Home"
}
