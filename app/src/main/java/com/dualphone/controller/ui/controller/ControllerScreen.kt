package com.dualphone.controller.ui.controller

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dualphone.controller.core.ControllerButton
import com.dualphone.controller.ui.components.ControllerButtonView
import com.dualphone.controller.ui.components.DPadView
import com.dualphone.controller.ui.components.JoystickPad
import com.dualphone.controller.ui.theme.AccentBlue
import com.dualphone.controller.ui.theme.Black
import com.dualphone.controller.ui.theme.Charcoal
import com.dualphone.controller.ui.theme.DarkSurface
import com.dualphone.controller.ui.theme.TextPrimary
import com.dualphone.controller.ui.theme.TextSecondary

@Composable
fun ControllerScreen(
    onBack: () -> Unit,
    viewModel: ControllerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val settings by viewModel.settings.collectAsState()
    val gamepad by viewModel.gamepadState.collectAsState()
    val opacity = settings.buttonOpacity

    // Force landscape while in controller mode
    DisposableEffect(Unit) {
        val previous = activity?.requestedOrientation
        if (settings.landscapeMode) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            previous?.let { activity?.requestedOrientation = it }
                ?: run { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            viewModel.releaseAll()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Controller Mode – Local Test",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            // Live input indicator
            InputTestIndicator(gamepad = gamepad)
        }

        // Main controller layout – fixed relative positions for landscape
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            // Left side: D-Pad + Left Stick
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DPadView(
                    size = 110.dp,
                    opacity = opacity,
                    onDown = viewModel::onButtonDown,
                    onUp = viewModel::onButtonUp
                )
                Spacer(modifier = Modifier.height(16.dp))
                JoystickPad(
                    size = 130.dp,
                    opacity = opacity,
                    onDown = viewModel::onLeftStickDown,
                    onMove = viewModel::onLeftStickMove,
                    onUp = viewModel::onLeftStickUp
                )
            }

            // Center: Select / Start / Home
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControllerButtonView(
                    button = ControllerButton.SELECT,
                    label = "SEL",
                    size = 44.dp,
                    opacity = opacity,
                    isRound = false,
                    onDown = viewModel::onButtonDown,
                    onUp = viewModel::onButtonUp
                )
                Spacer(modifier = Modifier.width(12.dp))
                ControllerButtonView(
                    button = ControllerButton.HOME,
                    label = "⌂",
                    size = 40.dp,
                    opacity = opacity,
                    isRound = true,
                    onDown = viewModel::onButtonDown,
                    onUp = viewModel::onButtonUp
                )
                Spacer(modifier = Modifier.width(12.dp))
                ControllerButtonView(
                    button = ControllerButton.START,
                    label = "STR",
                    size = 44.dp,
                    opacity = opacity,
                    isRound = false,
                    onDown = viewModel::onButtonDown,
                    onUp = viewModel::onButtonUp
                )
            }

            // Right side: Face buttons + Right Stick + Shoulders
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shoulders
                Row {
                    ControllerButtonView(
                        button = ControllerButton.L1,
                        label = "L1",
                        size = 48.dp,
                        opacity = opacity,
                        isRound = false,
                        onDown = viewModel::onButtonDown,
                        onUp = viewModel::onButtonUp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ControllerButtonView(
                        button = ControllerButton.L2,
                        label = "L2",
                        size = 48.dp,
                        opacity = opacity,
                        isRound = false,
                        onDown = viewModel::onButtonDown,
                        onUp = viewModel::onButtonUp
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    ControllerButtonView(
                        button = ControllerButton.R1,
                        label = "R1",
                        size = 48.dp,
                        opacity = opacity,
                        isRound = false,
                        onDown = viewModel::onButtonDown,
                        onUp = viewModel::onButtonUp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ControllerButtonView(
                        button = ControllerButton.R2,
                        label = "R2",
                        size = 48.dp,
                        opacity = opacity,
                        isRound = false,
                        onDown = viewModel::onButtonDown,
                        onUp = viewModel::onButtonUp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Face buttons diamond
                Box(modifier = Modifier.size(140.dp)) {
                    ControllerButtonView(
                        button = ControllerButton.Y,
                        label = "Y",
                        size = 48.dp,
                        opacity = opacity,
                        modifier = Modifier.align(Alignment.TopCenter),
                        onDown = viewModel::onButtonDown,
                        onUp = viewModel::onButtonUp
                    )
                    ControllerButtonView(
                        button = ControllerButton.X,
                        label = "X",
                        size = 48.dp,
                        opacity = opacity,
                        modifier = Modifier.align(Alignment.CenterStart),
                        onDown = viewModel::onButtonDown,
                        onUp = viewModel::onButtonUp
                    )
                    ControllerButtonView(
                        button = ControllerButton.B,
                        label = "B",
                        size = 48.dp,
                        opacity = opacity,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onDown = viewModel::onButtonDown,
                        onUp = viewModel::onButtonUp
                    )
                    ControllerButtonView(
                        button = ControllerButton.A,
                        label = "A",
                        size = 48.dp,
                        opacity = opacity,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onDown = viewModel::onButtonDown,
                        onUp = viewModel::onButtonUp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                JoystickPad(
                    size = 130.dp,
                    opacity = opacity,
                    onDown = viewModel::onRightStickDown,
                    onMove = viewModel::onRightStickMove,
                    onUp = viewModel::onRightStickUp
                )
            }
        }
    }
}

@Composable
private fun InputTestIndicator(gamepad: com.dualphone.controller.core.GamepadState) {
    val active = gamepad.buttonsBitmask != 0 ||
            gamepad.leftStickX != 0f || gamepad.leftStickY != 0f ||
            gamepad.rightStickX != 0f || gamepad.rightStickY != 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (active) AccentBlue else TextSecondary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (active) "Input active" else "No input",
            color = TextSecondary,
            fontSize = 12.sp
        )
        if (active) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "L(${"%.2f".format(gamepad.leftStickX)},${"%.2f".format(gamepad.leftStickY)}) " +
                        "R(${"%.2f".format(gamepad.rightStickX)},${"%.2f".format(gamepad.rightStickY)})",
                color = TextPrimary,
                fontSize = 11.sp
            )
        }
    }
}
