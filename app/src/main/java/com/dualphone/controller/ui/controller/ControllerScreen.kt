package com.dualphone.controller.ui.controller

import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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

/** Soft white glow matching the reference overlay controller. */
private val Glow = Color(0x44FFFFFF)
private val Outline = Color(0x66FFFFFF)
private val Face = Color(0xFF0A0A0A)

/**
 * Exact relative layout from the supplied Sleeping Dogs / touch-overlay reference.
 * Coordinates are fractions of the safe controller area (0–1).
 */
private object Ref {
    // BACK / START
    const val BACK_X = 0.26f; const val BACK_Y = 0.06f
    const val START_X = 0.74f; const val START_Y = 0.06f
    // Shoulders
    const val LT_X = 0.12f; const val LT_Y = 0.20f
    const val LB_X = 0.18f; const val LB_Y = 0.28f
    const val RT_X = 0.88f; const val RT_Y = 0.20f
    const val RB_X = 0.82f; const val RB_Y = 0.28f
    // Stick labels
    const val LS_X = 0.08f; const val LS_Y = 0.40f
    const val RS_X = 0.92f; const val RS_Y = 0.40f
    // Analog sticks
    const val L_STICK_X = 0.14f; const val L_STICK_Y = 0.58f
    const val R_STICK_X = 0.86f; const val R_STICK_Y = 0.42f
    // D-pad
    const val DPAD_X = 0.34f; const val DPAD_Y = 0.62f
    // Face diamond
    const val Y_X = 0.81f; const val Y_Y = 0.48f
    const val X_X = 0.73f; const val X_Y = 0.58f
    const val B_X = 0.89f; const val B_Y = 0.58f
    const val A_X = 0.81f; const val A_Y = 0.68f
}

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
    val opacity = settings.buttonOpacity.coerceIn(0.5f, 1f)
    var pairingInput by remember { mutableStateOf("") }
    var showConnectPanel by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            previous?.let { activity?.requestedOrientation = it }
                ?: run { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            viewModel.releaseAll()
            main?.volumeKeyHandler = null
        }
    }

    DisposableEffect(settings.volumeUpTarget, settings.volumeDownTarget) {
        if (viewModel.volumeMappingEnabled()) {
            main?.volumeKeyHandler = handler@{ event ->
                val isUp = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
                val isDown = event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                if (!isUp && !isDown) return@handler false
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) viewModel.onVolumeKey(isUp, true)
                        true
                    }
                    KeyEvent.ACTION_UP -> {
                        viewModel.onVolumeKey(isUp, false)
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
        Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // Minimal top chrome
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextSecondary)
            }
            Text(
                if (connection.connected) "LIVE" else "LOCAL",
                color = if (connection.connected) AccentBlue else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showConnectPanel = !showConnectPanel }) {
                Text("Connect", color = AccentBlue, fontSize = 11.sp)
            }
        }

        if (showConnectPanel) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface.copy(alpha = 0.96f))
                    .border(1.dp, Outline, RoundedCornerShape(10.dp))
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Text(connection.statusMessage, color = TextSecondary, fontSize = 12.sp)
                if (connection.connected) {
                    Text(
                        "pkts ${connection.packetsSent} · ${"%.0f".format(connection.updateRateHz)} Hz",
                        color = TextPrimary, fontSize = 11.sp
                    )
                    TextButton(onClick = { viewModel.disconnect() }) {
                        Text("Disconnect", color = AccentBlue)
                    }
                } else {
                    Text("IP:PORT:TOKEN  or  BT:MAC:TOKEN", color = TextSecondary, fontSize = 11.sp)
                    BasicTextField(
                        value = pairingInput,
                        onValueChange = { pairingInput = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(AccentBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Black)
                            .padding(8.dp)
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

        // Reference-faithful layout
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 4.dp)
        ) {
            val W = maxWidth
            val H = maxHeight
            val density = LocalDensity.current

            fun px(fx: Float, fy: Float, sizeDp: Dp): Modifier {
                val s = sizeDp
                return Modifier
                    .offset(
                        x = W * fx - s / 2,
                        y = H * fy - s / 2
                    )
                    .size(s)
            }

            // BACK / START
            RefRect("BACK", ControllerButton.SELECT, px(Ref.BACK_X, Ref.BACK_Y, 56.dp), opacity, viewModel)
            RefRect("START", ControllerButton.START, px(Ref.START_X, Ref.START_Y, 56.dp), opacity, viewModel)

            // Shoulders LT LB RT RB
            RefShoulder("LT", ControllerButton.L2, px(Ref.LT_X, Ref.LT_Y, 48.dp), -18f, opacity, viewModel)
            RefShoulder("LB", ControllerButton.L1, px(Ref.LB_X, Ref.LB_Y, 48.dp), -12f, opacity, viewModel)
            RefShoulder("RT", ControllerButton.R2, px(Ref.RT_X, Ref.RT_Y, 48.dp), 18f, opacity, viewModel)
            RefShoulder("RB", ControllerButton.R1, px(Ref.RB_X, Ref.RB_Y, 48.dp), 12f, opacity, viewModel)

            // LS / RS indicator dots
            RefDot("LS", px(Ref.LS_X, Ref.LS_Y, 36.dp), opacity)
            RefDot("RS", px(Ref.RS_X, Ref.RS_Y, 36.dp), opacity)

            // Left stick
            Box(px(Ref.L_STICK_X, Ref.L_STICK_Y, 120.dp), contentAlignment = Alignment.Center) {
                JoystickPad(
                    size = 120.dp,
                    opacity = opacity,
                    onDown = viewModel::onLeftStickDown,
                    onMove = viewModel::onLeftStickMove,
                    onUp = viewModel::onLeftStickUp
                )
            }

            // Right stick (reference places it mid-right, above ABXY cluster slightly)
            Box(px(Ref.R_STICK_X, Ref.R_STICK_Y, 100.dp), contentAlignment = Alignment.Center) {
                JoystickPad(
                    size = 100.dp,
                    opacity = opacity,
                    onDown = viewModel::onRightStickDown,
                    onMove = viewModel::onRightStickMove,
                    onUp = viewModel::onRightStickUp
                )
            }

            // D-pad
            Box(px(Ref.DPAD_X, Ref.DPAD_Y, 100.dp)) {
                DPadView(
                    size = 100.dp,
                    opacity = opacity,
                    onDown = viewModel::onButtonDown,
                    onUp = viewModel::onButtonUp
                )
            }

            // ABXY diamond
            RefCircle("Y", ControllerButton.Y, px(Ref.Y_X, Ref.Y_Y, 48.dp), opacity, viewModel)
            RefCircle("X", ControllerButton.X, px(Ref.X_X, Ref.X_Y, 48.dp), opacity, viewModel)
            RefCircle("B", ControllerButton.B, px(Ref.B_X, Ref.B_Y, 48.dp), opacity, viewModel)
            RefCircle("A", ControllerButton.A, px(Ref.A_X, Ref.A_Y, 48.dp), opacity, viewModel)
        }
    }
}

@Composable
private fun RefRect(
    label: String,
    button: ControllerButton,
    modifier: Modifier,
    opacity: Float,
    vm: ControllerViewModel
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        ControllerButtonView(
            button = button,
            label = label,
            size = 48.dp,
            opacity = opacity,
            isRound = false,
            onDown = vm::onButtonDown,
            onUp = vm::onButtonUp
        )
    }
}

@Composable
private fun RefCircle(
    label: String,
    button: ControllerButton,
    modifier: Modifier,
    opacity: Float,
    vm: ControllerViewModel
) {
    Box(
        modifier
            .shadow(6.dp, CircleShape, ambientColor = Glow, spotColor = Glow)
            .border(1.dp, Outline.copy(alpha = opacity), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        ControllerButtonView(
            button = button,
            label = label,
            size = 46.dp,
            opacity = opacity,
            isRound = true,
            onDown = vm::onButtonDown,
            onUp = vm::onButtonUp
        )
    }
}

@Composable
private fun RefShoulder(
    label: String,
    button: ControllerButton,
    modifier: Modifier,
    rotation: Float,
    opacity: Float,
    vm: ControllerViewModel
) {
    Box(
        modifier
            .rotate(rotation)
            .shadow(4.dp, RoundedCornerShape(10.dp), ambientColor = Glow, spotColor = Glow)
            .clip(RoundedCornerShape(10.dp))
            .background(Face.copy(alpha = opacity))
            .border(1.dp, Outline.copy(alpha = opacity), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        ControllerButtonView(
            button = button,
            label = label,
            size = 44.dp,
            opacity = opacity,
            isRound = false,
            onDown = vm::onButtonDown,
            onUp = vm::onButtonUp
        )
    }
}

@Composable
private fun RefDot(label: String, modifier: Modifier, opacity: Float) {
    Box(
        modifier
            .border(1.dp, Outline.copy(alpha = opacity * 0.7f), CircleShape)
            .background(Face.copy(alpha = opacity * 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextSecondary.copy(alpha = opacity * 0.8f), fontSize = 10.sp)
    }
}
