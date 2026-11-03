package com.dualphone.controller.ui.gaming

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dualphone.controller.ui.theme.AccentBlue
import com.dualphone.controller.ui.theme.AccentCyan
import com.dualphone.controller.ui.theme.Charcoal
import com.dualphone.controller.ui.theme.DarkSurface
import com.dualphone.controller.ui.theme.SuccessGreen
import com.dualphone.controller.ui.theme.TextPrimary
import com.dualphone.controller.ui.theme.TextSecondary

@Composable
fun GamingModeScreen(
    onBack: () -> Unit,
    viewModel: GamingViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val diag = ui.diagnostics

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "Gaming Mode",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Receiver Status", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                StatusLine("Device", android.os.Build.MODEL)
                StatusLine("Role", "Gaming Receiver")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Connection", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(120.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (diag.isConnected) SuccessGreen else TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (diag.isConnected) "Connected" else if (ui.isListening) "Listening" else "Stopped",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                StatusLine("Type", diag.connectionType.ifBlank { "—" })
                StatusLine("Local IP", diag.localIp.ifBlank { "—" })
                StatusLine("TCP / UDP", if (diag.tcpPort > 0) "${diag.tcpPort} / ${diag.udpPort}" else "—")
                if (diag.isConnected) {
                    StatusLine("Remote", diag.remoteDeviceName.ifBlank { diag.remoteAddress })
                    StatusLine("Session", diag.sessionId.toString())
                }
                StatusLine("Status", diag.statusMessage)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (ui.isListening || diag.isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Live Diagnostics", style = MaterialTheme.typography.titleMedium, color = AccentCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusLine("Packets received", diag.packetsReceived.toString())
                    StatusLine("Loss estimate", diag.packetsLostEstimate.toString())
                    StatusLine("Update rate", "%.0f Hz".format(diag.updateRateHz))
                    StatusLine(
                        "Est. RTT",
                        diag.estimatedRttMs?.let { "%.0f ms".format(it) } ?: "Not measured (one-way only)"
                    )
                    StatusLine(
                        "Last packet",
                        if (diag.lastPacketTimestampMs > 0) "${diag.lastPacketTimestampMs}" else "—"
                    )
                    if (diag.isConnected) {
                        val s = ui.lastInput
                        Text(
                            "Input L(%.2f,%.2f) R(%.2f,%.2f) mask=0x%X".format(
                                s.leftStickX, s.leftStickY, s.rightStickX, s.rightStickY, s.buttonsBitmask
                            ),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row {
            Button(
                onClick = { viewModel.startWifiReceiver() },
                enabled = !ui.isListening,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Wi-Fi")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.startBluetoothReceiver() },
                enabled = !ui.isListening && ui.bluetoothAvailable,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bluetooth")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { viewModel.stopReceiver() },
                enabled = ui.isListening,
                modifier = Modifier.weight(0.8f)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stop")
            }
        }

        if (ui.bluetoothToken.isNotBlank() && ui.activeTransport.name.contains("BLUETOOTH")) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "BT pairing token: ${ui.bluetoothToken}",
                color = AccentCyan,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Controller enters: BT:MAC:TOKEN (bonded device)",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (ui.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(ui.error!!, color = MaterialTheme.colorScheme.error)
        }

        if (ui.isListening && ui.pairingString.isNotBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Pairing", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Scan or enter on Controller phone",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ui.qrBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Pairing QR",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        text = ui.pairingString,
                        color = AccentCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Format: IP:TCP_PORT:TOKEN",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Connection options", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))

        OptionCard(
            icon = Icons.Default.WifiTethering,
            title = "Wi-Fi Hotspot / LAN",
            subtitle = "Start receiver, then connect the controller to the same network (or this phone's hotspot). Enter the pairing string on the controller."
        )
        Spacer(modifier = Modifier.height(8.dp))
        OptionCard(
            icon = Icons.Default.Wifi,
            title = "Wi-Fi Direct",
            subtitle = if (ui.wifiDirectAvailable) {
                "Available – ${ui.wifiDirectStatus.ifBlank { "ready" }}"
            } else {
                "Hardware/permission unavailable – use hotspot or existing Wi-Fi instead"
            },
            actionLabel = if (ui.wifiDirectAvailable) "Discover peers" else null,
            onAction = { viewModel.startWifiDirectDiscovery() }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OptionCard(
            icon = Icons.Default.Bluetooth,
            title = "Bluetooth RFCOMM",
            subtitle = if (ui.bluetoothAvailable) {
                "Available – start Bluetooth receiver, then connect from a bonded controller device with token"
            } else {
                "Bluetooth disabled or unavailable on this device"
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "UDP carries live input after TCP session authentication. " +
                "Disconnect always sends RELEASE_ALL so buttons never stick.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(120.dp))
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AccentBlue.copy(alpha = 0.8f), modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(onClick = onAction) { Text(actionLabel) }
                }
            }
        }
    }
}
