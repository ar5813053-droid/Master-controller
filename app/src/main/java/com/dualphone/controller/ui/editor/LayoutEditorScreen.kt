package com.dualphone.controller.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dualphone.controller.ui.components.ControllerCanvas
import com.dualphone.controller.ui.theme.AccentBlue
import com.dualphone.controller.ui.theme.Black
import com.dualphone.controller.ui.theme.ButtonFace
import com.dualphone.controller.ui.theme.Charcoal
import com.dualphone.controller.ui.theme.TextPrimary
import com.dualphone.controller.ui.theme.TextSecondary
import kotlin.math.hypot

@Composable
fun LayoutEditorScreen(
    onBack: () -> Unit,
    viewModel: LayoutEditorViewModel = viewModel()
) {
    val layout by viewModel.layout.collectAsState()
    val selectedId by viewModel.selectedId.collectAsState()
    val selected = layout.controls.find { it.controlId == selectedId }

    Column(
        Modifier
            .fillMaxSize()
            .background(Charcoal)
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
            }
            Text(
                "Layout Editor",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "Landscape 16:9 — tap a control on the preview to select, then drag to move",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Black)
                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp))
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                ControllerCanvas(
                    layout = layout,
                    opacity = layout.opacity,
                    interactive = false,
                    selectedId = selectedId,
                    modifier = Modifier.fillMaxSize()
                )
                // Tap = select nearest; Drag = move selected
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(layout, selectedId) {
                            detectTapGestures { offset ->
                                val nx = offset.x / size.width
                                val ny = offset.y / size.height
                                val nearest = layout.controls.minByOrNull { c ->
                                    hypot((c.x - nx).toDouble(), (c.y - ny).toDouble())
                                }
                                if (nearest != null) viewModel.select(nearest.controlId)
                            }
                        }
                        .pointerInput(selectedId, layout) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                if (selectedId == null) {
                                    val nx = change.position.x / size.width
                                    val ny = change.position.y / size.height
                                    val nearest = layout.controls.minByOrNull { c ->
                                        hypot((c.x - nx).toDouble(), (c.y - ny).toDouble())
                                    }
                                    if (nearest != null) viewModel.select(nearest.controlId)
                                }
                                viewModel.nudgeSelected(
                                    drag.x / size.width,
                                    drag.y / size.height
                                )
                            }
                        }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        if (selected != null) {
            Text(
                "Selected: ${selected.controlId}  x=${"%.2f".format(selected.x)} y=${"%.2f".format(selected.y)}  w=${"%.2f".format(selected.width)}",
                color = TextPrimary,
                fontSize = 12.sp
            )
            Text("Size", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = selected.width,
                onValueChange = { viewModel.resizeSelected(it) },
                valueRange = 0.05f..0.35f
            )
            Text("Opacity", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = selected.opacity,
                onValueChange = { viewModel.setSelectedOpacity(it) },
                valueRange = 0.3f..1f
            )
        } else {
            Text("Tap any control on the preview to select it", color = TextSecondary, fontSize = 12.sp)
        }

        Spacer(Modifier.height(8.dp))
        layout.controls.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { c ->
                    Button(
                        onClick = { viewModel.select(c.controlId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (c.controlId == selectedId) AccentBlue else ButtonFace
                        ),
                        modifier = Modifier.weight(1f).padding(2.dp)
                    ) {
                        Text(c.controlId.removePrefix("btn_").removePrefix("stick_"), color = TextPrimary, fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = { viewModel.save() },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.weight(1f)
            ) { Text("Save", color = TextPrimary, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { viewModel.resetToDefault() },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonFace),
                modifier = Modifier.weight(1f)
            ) { Text("Reset Classic", color = TextPrimary) }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.loadPresetDualStick() },
            colors = ButtonDefaults.buttonColors(containerColor = ButtonFace),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Preset: Dual Joystick", color = TextPrimary) }
    }
}
