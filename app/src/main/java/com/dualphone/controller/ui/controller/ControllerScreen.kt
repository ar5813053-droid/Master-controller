package com.dualphone.controller.ui.controller

import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dualphone.controller.MainActivity
import com.dualphone.controller.ui.components.ControllerCanvas
import com.dualphone.controller.ui.theme.AccentBlue
import com.dualphone.controller.ui.theme.Black
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
    val main = context as? MainActivity
    val settings by viewModel.settings.collectAsState()
    val connection by viewModel.connectionUi.collectAsState()
    val connectError by viewModel.connectError.collectAsState()
    val layout by viewModel.layout.collectAsState()
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

    Box(Modifier.fillMaxSize().background(Black)) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp).align(Alignment.TopStart),
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
                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
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

        // 16:9 landscape canvas — same renderer as Layout Editor
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 32.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Black)
            ) {
                ControllerCanvas(
                    layout = layout,
                    opacity = opacity,
                    interactive = true,
                    onLeftStickDown = viewModel::onLeftStickDown,
                    onLeftStickMove = viewModel::onLeftStickMove,
                    onLeftStickUp = viewModel::onLeftStickUp,
                    onRightStickDown = viewModel::onRightStickDown,
                    onRightStickMove = viewModel::onRightStickMove,
                    onRightStickUp = viewModel::onRightStickUp,
                    onButtonDown = viewModel::onButtonDown,
                    onButtonUp = viewModel::onButtonUp
                )
            }
        }
    }
}
