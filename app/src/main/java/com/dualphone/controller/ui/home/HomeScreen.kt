package com.dualphone.controller.ui.home

import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dualphone.controller.core.ConnectionStatus
import com.dualphone.controller.core.DeviceRole
import com.dualphone.controller.ui.theme.AccentBlue
import com.dualphone.controller.ui.theme.ErrorRed
import com.dualphone.controller.ui.theme.AccentCyan
import com.dualphone.controller.ui.theme.Charcoal
import com.dualphone.controller.ui.theme.DarkSurface
import com.dualphone.controller.ui.theme.DarkSurfaceVariant
import com.dualphone.controller.ui.theme.TextPrimary
import com.dualphone.controller.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onNavigateController: () -> Unit,
    onNavigateGaming: () -> Unit,
    onNavigateEditor: () -> Unit,
    onNavigateSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showControllerConnectDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(AccentBlue, AccentCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "DualPhone Controller",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Milestone 1 – Foundation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Device Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                StatusRow(label = "Device", value = uiState.deviceName)
                StatusRow(
                    label = "Role",
                    value = when (uiState.deviceRole) {
                        DeviceRole.CONTROLLER -> "Controller"
                        DeviceRole.GAMING_RECEIVER -> "Gaming Receiver"
                        DeviceRole.NONE -> "None selected"
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Connection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.width(100.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (uiState.connectionStatus) {
                                    ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
                                    ConnectionStatus.CONNECTING -> Color(0xFFFF9800)
                                    ConnectionStatus.DISCONNECTED -> Color(0xFF9E9E9E)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (uiState.connectionStatus) {
                            ConnectionStatus.CONNECTED -> "Connected"
                            ConnectionStatus.CONNECTING -> "Connecting…"
                            ConnectionStatus.DISCONNECTED -> "Disconnected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Modes",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        ModeCard(
            title = "Controller Mode",
            subtitle = "Turn this phone into a landscape virtual gamepad",
            icon = Icons.Default.Gamepad,
            accent = AccentBlue,
            onClick = {
                viewModel.setRole(DeviceRole.CONTROLLER)
                showControllerConnectDialog = true
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        ModeCard(
            title = "Gaming Mode",
            subtitle = "Prepare this phone as the wireless input receiver",
            icon = Icons.Default.SportsEsports,
            accent = AccentCyan,
            onClick = {
                viewModel.setRole(DeviceRole.GAMING_RECEIVER)
                onNavigateGaming()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tools",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToolCard(
                title = "Layout Editor",
                icon = Icons.Default.Edit,
                modifier = Modifier.weight(1f),
                onClick = onNavigateEditor
            )
            ToolCard(
                title = "Settings",
                icon = Icons.Default.Settings,
                modifier = Modifier.weight(1f),
                onClick = onNavigateSettings
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Wireless features (Wi-Fi Hotspot, Wi-Fi Direct, Bluetooth RFCOMM) arrive in later milestones.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }

    if (showControllerConnectDialog) {
        ControllerConnectDialog(
            onDismiss = { showControllerConnectDialog = false },
            onContinueLocal = {
                showControllerConnectDialog = false
                onNavigateController()
            },
            onConnected = {
                showControllerConnectDialog = false
                onNavigateController()
            }
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}


@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun ControllerConnectDialog(
    onDismiss: () -> Unit,
    onContinueLocal: () -> Unit,
    onConnected: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as? com.dualphone.controller.DualPhoneApp
    var pairing by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Connect Controller", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Bluetooth RFCOMM (recommended for app-to-app)",
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "1) Dono phones Settings → Bluetooth me pair karein\n" +
                        "2) Gaming Mode me ▶ Bluetooth dabayein\n" +
                        "3) Jo token dikhe (jaise 6BF27991) yahan paste karein\n" +
                        "   Ya full: BT:AA:BB:CC:DD:EE:FF:TOKEN",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Wi-Fi (optional)",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Gaming Mode me Wi-Fi start karke IP:PORT:TOKEN paste karein",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pairing,
                    onValueChange = { pairing = it; error = null },
                    singleLine = true,
                    label = { Text("Token / BT:MAC:TOKEN / IP:PORT:TOKEN") },
                    placeholder = { Text("6BF27991") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pairing.isBlank()) {
                        error = "Token ya pairing string likhein"
                        return@TextButton
                    }
                    val cm = app?.connectionManager
                    if (cm == null) {
                        error = "App not ready"
                        return@TextButton
                    }
                    connecting = true
                    scope.launch {
                        val result = cm.connectAuto(pairing.trim())
                        connecting = false
                        if (result.isSuccess) onConnected()
                        else error = result.exceptionOrNull()?.message ?: "Connection failed"
                    }
                },
                enabled = !connecting
            ) {
                Text(if (connecting) "Connecting…" else "Connect & Open", color = AccentBlue)
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onContinueLocal) {
                    Text("Open Local (no network)", color = TextSecondary)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        },
        containerColor = DarkSurface
    )
}
