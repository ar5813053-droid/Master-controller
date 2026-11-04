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
    val hid by viewModel.hidUi.collectAsState()
    val opacity = settings.buttonOpacity.coerceIn(0.55f, 1f)
    var pairingInput by remember { mutableStateOf("") }
    // Start open so Connect is never "missing"
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
        // Top bar — Connect always visible
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
            }
            Text(
                if (connection.connected) "● LIVE" else "○ LOCAL",
                color = if (connection.connected) AccentBlue else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { showConnectPanel = !showConnectPanel },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showConnectPanel) AccentBlue.copy(alpha = 0.25f) else DarkSurface)
            ) {
                Text(
                    if (connection.connected) "Net / Disconnect" else "Connect",
                    color = AccentBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (showConnectPanel) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface.copy(alpha = 0.97f))
                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                    .padding(14.dp)
                    .fillMaxWidth(0.92f)
            ) {
                Text("Bluetooth HID Gamepad (primary)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Status: ${hid.status} — ${hid.message}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                if (hid.hostName != null) {
                    Text("Host: ${hid.hostName}", color = AccentBlue, fontSize = 11.sp)
                }
                Text("Reports sent: ${hid.reportsSent}", color = TextSecondary, fontSize = 11.sp)
                Row {
                    TextButton(onClick = { viewModel.startHidGamepad() }) {
                        Text("Start HID", color = AccentBlue, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { viewModel.stopHidGamepad() }) {
                        Text("Stop HID", color = TextSecondary)
                    }
                }
                Text(
                    "1) Start HID  2) Gaming phone: Settings → Bluetooth → pair “DualPhone Gamepad”  3) Open a gamepad-compatible game",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (connection.connected) connection.statusMessage else "Optional Wi-Fi diagnostic: IP:TCP_PORT:TOKEN",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (connection.connected) {
                    Text(
                        "pkts ${connection.packetsSent} · ${"%.0f".format(connection.updateRateHz)} Hz",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Row {
                        TextButton(onClick = { viewModel.disconnect() }) {
                            Text("Disconnect", color = AccentBlue, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { showConnectPanel = false }) {
                            Text("Hide", color = TextSecondary)
                        }
                    }
                } else {
                    BasicTextField(
                        value = pairingInput,
                        onValueChange = { pairingInput = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(AccentBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Black)
                            .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                    if (connectError != null) {
                        Text(connectError!!, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    }
                    Row {
                        TextButton(onClick = { viewModel.connect(pairingInput) }) {
                            Text("Connect", color = AccentBlue, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { showConnectPanel = false }) {
                            Text("Hide panel", color = TextSecondary)
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 44.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
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
