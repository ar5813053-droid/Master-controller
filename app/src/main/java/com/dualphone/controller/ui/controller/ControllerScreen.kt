package com.dualphone.controller.ui.controller

import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dualphone.controller.MainActivity
import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.ui.components.ControllerButtonView
import com.dualphone.controller.ui.components.DPadView
import com.dualphone.controller.ui.components.JoystickPad
import com.dualphone.controller.ui.theme.AccentBlue
import com.dualphone.controller.ui.theme.Black
import com.dualphone.controller.ui.theme.DarkSurface
import com.dualphone.controller.ui.theme.TextPrimary
import com.dualphone.controller.ui.theme.TextSecondary

private val GlowWhite = Color(0x33FFFFFF)
private val OutlineSoft = Color(0x55FFFFFF)
private val FaceDark = Color(0xFF1A1A1A)

@Composable
fun ControllerScreen(
    onBack: () -> Unit,
    viewModel: ControllerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val main = context as? MainActivity
    val settings by viewModel.settings.collectAsState()
    val gamepad by viewModel.gamepadState.collectAsState()
    val connection by viewModel.connectionUi.collectAsState()
    val connectError by viewModel.connectError.collectAsState()
    val opacity = settings.buttonOpacity
    var pairingInput by remember { mutableStateOf("") }
    var showConnectPanel by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val previous = activity?.requestedOrientation
        if (settings.landscapeMode) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            previous?.let { activity?.requestedOrientation = it }
                ?: run { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            viewModel.releaseAll()
            main?.volumeKeyHandler = null
        }
    }

    // Capture volume keys only while mapping enabled and screen active
    DisposableEffect(settings.volumeUpTarget, settings.volumeDownTarget) {
        if (viewModel.volumeMappingEnabled()) {
            main?.volumeKeyHandler = { event ->
                val isUp = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
                val isDown = event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                if (!isUp && !isDown) return@volumeKeyHandler false
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            viewModel.onVolumeKey(isVolumeUp = isUp, pressed = true)
                        }
                        true
                    }
                    KeyEvent.ACTION_UP -> {
                        viewModel.onVolumeKey(isVolumeUp = isUp, pressed = false)
                        true
                    }
                    else -> false
                }
            }
        } else {
            main?.volumeKeyHandler = null
        }
        onDispose { main?.volumeKeyHandler = null }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // Soft vignette
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF151515), Color.Black),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.maxDimension * 0.75f
                )
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = if (connection.connected) "LIVE" else "LOCAL",
                style = MaterialTheme.typography.titleMedium,
                color = if (connection.connected) AccentBlue else TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { showConnectPanel = !showConnectPanel }) {
                Text(if (connection.connected) "Net" else "Connect", color = AccentBlue, fontSize = 12.sp)
            }
            InputPill(gamepadActive = gamepad.buttonsBitmask != 0 ||
                gamepad.leftStickX != 0f || gamepad.leftStickY != 0f ||
                gamepad.rightStickX != 0f || gamepad.rightStickY != 0f)
        }

        if (showConnectPanel) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface.copy(alpha = 0.95f))
                    .border(1.dp, OutlineSoft, RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Text(connection.statusMessage, color = TextSecondary, fontSize = 12.sp)
                if (connection.connected) {
                    Text(
                        "Sent ${connection.packetsSent} · ${"%.0f".format(connection.updateRateHz)} Hz",
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                    TextButton(onClick = { viewModel.disconnect() }) {
                        Text("Disconnect", color = AccentBlue)
                    }
                } else {
                    Text("Wi-Fi: IP:PORT:TOKEN  ·  BT: MAC:TOKEN", color = TextSecondary, fontSize = 11.sp)
                    BasicTextField(
                        value = pairingInput,
                        onValueChange = { pairingInput = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(AccentBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Black)
                            .padding(10.dp)
                    )
                    if (connectError != null) {
                        Text(connectError!!, color = Color(0xFFFF6B6B), fontSize = 11.sp)
                    }
                    Row {
                        TextButton(onClick = { viewModel.connect(pairingInput) }) {
                            Text("Connect", color = AccentBlue)
                        }
                        TextButton(onClick = { showConnectPanel = false }) {
                            Text("Close", color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Main gamepad layout – premium reference style
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 8.dp, start = 12.dp, end = 12.dp)
        ) {
            // LEFT cluster: shoulders, D-pad, LS
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    GlowShoulder("LT", opacity, onDown = { viewModel.onButtonDown(ControllerButton.L2, it) }, onUp = { viewModel.onButtonUp(ControllerButton.L2, it) })
                    Spacer(Modifier.width(10.dp))
                    GlowShoulder("LB", opacity, onDown = { viewModel.onButtonDown(ControllerButton.L1, it) }, onUp = { viewModel.onButtonUp(ControllerButton.L1, it) })
                }
                Spacer(Modifier.height(10.dp))
                DPadView(
                    size = 108.dp,
                    opacity = opacity,
                    onDown = viewModel::onButtonDown,
                    onUp = viewModel::onButtonUp
                )
                Spacer(Modifier.height(14.dp))
                GlowJoystick(
                    size = 128.dp,
                    opacity = opacity,
                    label = "LS",
                    onDown = viewModel::onLeftStickDown,
                    onMove = viewModel::onLeftStickMove,
                    onUp = viewModel::onLeftStickUp
                )
            }

            // CENTER: Back / Home / Start
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlowPill("BACK", opacity, onDown = { viewModel.onButtonDown(ControllerButton.SELECT, it) }, onUp = { viewModel.onButtonUp(ControllerButton.SELECT, it) })
                Spacer(Modifier.width(14.dp))
                GlowCircle("⌂", 36.dp, opacity, onDown = { viewModel.onButtonDown(ControllerButton.HOME, it) }, onUp = { viewModel.onButtonUp(ControllerButton.HOME, it) })
                Spacer(Modifier.width(14.dp))
                GlowPill("START", opacity, onDown = { viewModel.onButtonDown(ControllerButton.START, it) }, onUp = { viewModel.onButtonUp(ControllerButton.START, it) })
            }

            // RIGHT cluster: RT/RB, ABXY, RS
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    GlowShoulder("RB", opacity, onDown = { viewModel.onButtonDown(ControllerButton.R1, it) }, onUp = { viewModel.onButtonUp(ControllerButton.R1, it) })
                    Spacer(Modifier.width(10.dp))
                    GlowShoulder("RT", opacity, onDown = { viewModel.onButtonDown(ControllerButton.R2, it) }, onUp = { viewModel.onButtonUp(ControllerButton.R2, it) })
                }
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.size(150.dp)) {
                    GlowFace("Y", Modifier.align(Alignment.TopCenter), opacity,
                        onDown = { viewModel.onButtonDown(ControllerButton.Y, it) },
                        onUp = { viewModel.onButtonUp(ControllerButton.Y, it) })
                    GlowFace("X", Modifier.align(Alignment.CenterStart), opacity,
                        onDown = { viewModel.onButtonDown(ControllerButton.X, it) },
                        onUp = { viewModel.onButtonUp(ControllerButton.X, it) })
                    GlowFace("B", Modifier.align(Alignment.CenterEnd), opacity,
                        onDown = { viewModel.onButtonDown(ControllerButton.B, it) },
                        onUp = { viewModel.onButtonUp(ControllerButton.B, it) })
                    GlowFace("A", Modifier.align(Alignment.BottomCenter), opacity,
                        onDown = { viewModel.onButtonDown(ControllerButton.A, it) },
                        onUp = { viewModel.onButtonUp(ControllerButton.A, it) })
                }
                Spacer(Modifier.height(10.dp))
                GlowJoystick(
                    size = 128.dp,
                    opacity = opacity,
                    label = "RS",
                    onDown = viewModel::onRightStickDown,
                    onMove = viewModel::onRightStickMove,
                    onUp = viewModel::onRightStickUp
                )
            }
        }
    }
}

@Composable
private fun InputPill(gamepadActive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, if (gamepadActive) AccentBlue else OutlineSoft, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (gamepadActive) AccentBlue else TextSecondary)
        )
        Spacer(Modifier.width(6.dp))
        Text(if (gamepadActive) "Input" else "Idle", color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun GlowJoystick(
    size: androidx.compose.ui.unit.Dp,
    opacity: Float,
    label: String,
    onDown: (Int) -> Unit,
    onMove: (Int, Float, Float) -> Unit,
    onUp: (Int) -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        // outer glow ring
        Box(
            Modifier
                .size(size + 8.dp)
                .border(1.5.dp, GlowWhite.copy(alpha = opacity * 0.5f), CircleShape)
        )
        JoystickPad(
            size = size,
            opacity = opacity,
            onDown = onDown,
            onMove = onMove,
            onUp = onUp
        )
        Text(
            label,
            color = TextSecondary.copy(alpha = 0.5f * opacity),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
        )
    }
}

@Composable
private fun GlowShoulder(
    label: String,
    opacity: Float,
    onDown: (Int) -> Unit,
    onUp: (Int) -> Unit
) {
    ControllerButtonView(
        button = when (label) {
            "LT" -> ControllerButton.L2
            "LB" -> ControllerButton.L1
            "RT" -> ControllerButton.R2
            else -> ControllerButton.R1
        },
        label = label,
        size = 52.dp,
        opacity = opacity,
        isRound = false,
        onDown = { _, id -> onDown(id) },
        onUp = { _, id -> onUp(id) }
    )
}

@Composable
private fun GlowFace(
    label: String,
    modifier: Modifier,
    opacity: Float,
    onDown: (Int) -> Unit,
    onUp: (Int) -> Unit
) {
    val btn = when (label) {
        "A" -> ControllerButton.A
        "B" -> ControllerButton.B
        "X" -> ControllerButton.X
        else -> ControllerButton.Y
    }
    Box(
        modifier = modifier
            .size(52.dp)
            .shadow(8.dp, CircleShape, ambientColor = Color.White.copy(alpha = 0.15f), spotColor = Color.White.copy(alpha = 0.2f))
    ) {
        ControllerButtonView(
            button = btn,
            label = label,
            size = 50.dp,
            opacity = opacity,
            isRound = true,
            onDown = { _, id -> onDown(id) },
            onUp = { _, id -> onUp(id) }
        )
    }
}

@Composable
private fun GlowPill(
    label: String,
    opacity: Float,
    onDown: (Int) -> Unit,
    onUp: (Int) -> Unit
) {
    val btn = if (label == "BACK") ControllerButton.SELECT else ControllerButton.START
    ControllerButtonView(
        button = btn,
        label = label,
        size = 44.dp,
        opacity = opacity,
        isRound = false,
        onDown = { _, id -> onDown(id) },
        onUp = { _, id -> onUp(id) }
    )
}

@Composable
private fun GlowCircle(
    label: String,
    size: androidx.compose.ui.unit.Dp,
    opacity: Float,
    onDown: (Int) -> Unit,
    onUp: (Int) -> Unit
) {
    ControllerButtonView(
        button = ControllerButton.HOME,
        label = label,
        size = size,
        opacity = opacity,
        isRound = true,
        onDown = { _, id -> onDown(id) },
        onUp = { _, id -> onUp(id) }
    )
}
