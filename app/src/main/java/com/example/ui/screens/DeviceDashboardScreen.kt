package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.device.batteryText
import com.example.device.chargingText
import com.example.device.isMediaVolumeReadable
import com.example.device.mediaVolumeText
import com.example.device.networkText
import com.example.device.orUnavailable
import com.example.device.ramDetailText
import com.example.device.ramUsedPercent
import com.example.device.storageDetailText
import com.example.device.storageUsedPercent
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel
import java.util.Locale

@Composable
fun DeviceDashboardScreen(
    viewModel: ArohiViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header with Back Arrow and "System Dashboard"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, Color(0x22FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "System Dashboard",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Telemetry Metric Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Battery & Storage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Battery Card
                DashboardMetricCard(
                    title = "Battery",
                    value = telemetry.batteryText(),
                    subtitle = telemetry.chargingText(),
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = EmeraldSuccess,
                    hasLeadingDot = true,
                    modifier = Modifier.weight(1f)
                )

                // Storage Card
                DashboardMetricCard(
                    title = "Storage",
                    value = telemetry.storageUsedPercent().orUnavailable(),
                    subtitle = telemetry.storageDetailText(),
                    icon = Icons.Default.Storage,
                    accentColor = CyanPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: RAM & Network
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // RAM Card
                DashboardMetricCard(
                    title = "RAM",
                    value = telemetry.ramUsedPercent().orUnavailable(),
                    subtitle = telemetry.ramDetailText(),
                    icon = Icons.Default.Memory,
                    accentColor = VioletBright,
                    modifier = Modifier.weight(1f)
                )

                // Network Card
                DashboardMetricCard(
                    title = "Network",
                    value = telemetry.networkText(),
                    subtitle = if (telemetry.isConnected) "Connected" else "Offline",
                    icon = Icons.Default.Wifi,
                    accentColor = CyanPrimary,
                    hasLeadingDot = true,
                    dotColor = if (telemetry.isConnected) EmeraldSuccess else MagentaAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // "Real-time Info" Section Header
        Text(
            text = "Real-time Info",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Real-time Info List Rows in Glass Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RealtimeInfoRow(
                    icon = Icons.Default.SettingsSuggest,
                    iconTint = EmeraldSuccess,
                    title = "CPU Usage",
                    value = "38%"
                )

                RealtimeInfoRow(
                    icon = Icons.Default.Thermostat,
                    iconTint = AmberWarning,
                    title = "Temperature",
                    value = "34°C"
                )

                RealtimeInfoRow(
                    icon = Icons.Default.PhoneAndroid,
                    iconTint = CyanPrimary,
                    title = "Screen On",
                    value = "2h 15m"
                )

                RealtimeInfoRow(
                    icon = Icons.Default.AccessTime,
                    iconTint = VioletBright,
                    title = "Uptime",
                    value = "5h 45m"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Hardware Controls: Flashlight & Volume
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FlashlightOn,
                            contentDescription = null,
                            tint = if (telemetry.isFlashlightOn) EmeraldSuccess else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Flashlight / Torch",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Switch(
                        checked = telemetry.isFlashlightOn,
                        onCheckedChange = { viewModel.toggleFlashlight() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldSuccess,
                            checkedTrackColor = EmeraldSuccess.copy(alpha = 0.3f),
                            uncheckedTrackColor = Color(0x1AFFFFFF)
                        ),
                        modifier = Modifier.testTag("dashboard_flashlight_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Media Volume (${telemetry.mediaVolumeText()})",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Slider(
                    value = telemetry.mediaVolumePercent.coerceIn(0, 100).toFloat(),
                    onValueChange = { viewModel.setMediaVolume(it.toInt()) },
                    enabled = telemetry.isMediaVolumeReadable(),
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary,
                        inactiveTrackColor = Color(0x1AFFFFFF)
                    ),
                    modifier = Modifier.testTag("dashboard_volume_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    hasLeadingDot: Boolean = false,
    dotColor: Color = EmeraldSuccess,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x141E293B),
                        Color(0x220D1222)
                    )
                )
            )
            .border(1.dp, Color(0x228B5CF6), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            // Big Value
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )

            // Subtitle with optional dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (hasLeadingDot) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (hasLeadingDot) dotColor else TextMuted
                )
            }
        }
    }
}

@Composable
fun RealtimeInfoRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
    }
}

