package com.dualphone.controller.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dualphone.controller.ui.theme.AccentBlue
import com.dualphone.controller.ui.theme.ButtonFace
import com.dualphone.controller.ui.theme.Charcoal
import com.dualphone.controller.ui.theme.TextPrimary
import com.dualphone.controller.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Haptic
        SettingRow(
            title = "Haptic Feedback",
            subtitle = "Vibrate on button press"
        ) {
            Switch(
                checked = settings.hapticEnabled,
                onCheckedChange = viewModel::setHaptic,
                colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue)
            )
        }

        HorizontalDivider(color = ButtonFace, modifier = Modifier.padding(vertical = 8.dp))

        // Deadzone
        Text("Joystick Deadzone", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "%.0f%%".format(settings.joystickDeadzone * 100),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = settings.joystickDeadzone,
            onValueChange = viewModel::setDeadzone,
            valueRange = 0f..0.4f,
            steps = 7
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sensitivity
        Text("Joystick Sensitivity", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "%.0f%%".format(settings.joystickSensitivity * 100),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = settings.joystickSensitivity,
            onValueChange = viewModel::setSensitivity,
            valueRange = 0.5f..2.0f,
            steps = 5
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Button opacity
        Text("Button Opacity", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "%.0f%%".format(settings.buttonOpacity * 100),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = settings.buttonOpacity,
            onValueChange = viewModel::setButtonOpacity,
            valueRange = 0.3f..1f
        )

        HorizontalDivider(color = ButtonFace, modifier = Modifier.padding(vertical = 12.dp))

        // Landscape
        SettingRow(
            title = "Force Landscape Controller",
            subtitle = "Lock orientation in Controller Mode"
        ) {
            Switch(
                checked = settings.landscapeMode,
                onCheckedChange = viewModel::setLandscapeMode,
                colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::resetDefaults,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonFace),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Restore Default Settings")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "About",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "DualPhone Controller\nMilestone 1 – Foundation & UI\n\nPersonal-use dual-phone gamepad system.\nNo accounts, ads or cloud services.\n\nVersion 1.0.0-milestone1",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        trailing()
    }
}
