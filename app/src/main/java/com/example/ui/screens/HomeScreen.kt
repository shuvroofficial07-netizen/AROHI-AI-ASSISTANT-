package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.engine.ArohiEmotion
import com.example.service.DiagnosticStatusLevel
import com.example.ui.components.GlowingArohiAvatar
import com.example.ui.components.VoiceWaveform
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel
import com.example.voice.SpeechState

/**
 * MAIN HOME — next-generation AROHI operating layer.
 * Every status, meter and animation on this screen is driven by REAL
 * application state: diagnostics, telemetry, mic RMS, TTS and the brain.
 */
@Composable
fun HomeScreen(
    viewModel: ArohiViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToVision: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSmartTasks: () -> Unit = {},
    onNavigateToRoutines: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToApps: () -> Unit = {},
    onNavigateToCalls: () -> Unit = {},
    onNavigateToBrain: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val speechState by viewModel.speechState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val emotion by viewModel.emotion.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val report by viewModel.diagnosticReport.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val unreadCount by viewModel.unreadNotifCount.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val languageCode by viewModel.languageCodeFlow.collectAsState()

    val isListening = speechState == SpeechState.LISTENING

    // Android 13+ requires POST_NOTIFICATIONS for the persistent foreground service notification
    val context = LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.startBackgroundOperatingService() }

    val isBgActive = diagnostics.isBackgroundServiceActive

    // REAL system status — never faked
    val (systemStatusColor, systemStatusText) = when (report.overallStatus) {
        DiagnosticStatusLevel.READY -> EmeraldSuccess to "ONLINE"
        DiagnosticStatusLevel.LIMITED -> Color(0xFFF59E0B) to "LIMITED"
        DiagnosticStatusLevel.ERROR -> MagentaAccent to "OFFLINE"
    }

    // The avatar follows the real assistant state, not random animation
    val avatarEmotion = when {
        isListening -> ArohiEmotion.LISTENING
        isProcessing -> ArohiEmotion.THINKING
        isSpeaking -> ArohiEmotion.SPEAKING
        else -> emotion
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header: branding + real system status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onNavigateToAbout)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    CyanPrimary,
                                    VioletBright,
                                    MagentaAccent,
                                    CyanPrimary
                                )
                            )
                        )
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF070A14)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = "AROHI AI ASSISTANT",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "by Shù Vrô",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        color = MagentaAccent
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Real ONLINE / LIMITED / OFFLINE pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(systemStatusColor.copy(alpha = 0.13f))
                        .border(1.dp, systemStatusColor.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                        .clickable { onNavigateToDiagnostics() }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .drawBehind {
                                drawCircle(
                                    color = systemStatusColor.copy(alpha = 0.45f),
                                    radius = size.minDimension * 1.4f
                                )
                            }
                            .clip(CircleShape)
                            .background(systemStatusColor)
                    )
                    Text(
                        text = systemStatusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = systemStatusColor
                    )
                }

                // Vision shortcut
                IconButton(
                    onClick = onNavigateToVision,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Arohi Vision",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Central AROHI AI Core — animation follows the REAL state
        Box(
            modifier = Modifier
                .size(200.dp)
                .testTag("home_avatar_image"),
            contentAlignment = Alignment.Center
        ) {
            GlowingArohiAvatar(
                emotion = avatarEmotion,
                speechState = speechState,
                rmsLevel = rmsLevel,
                isSpeaking = isSpeaking,
                size = 200.dp,
                onClick = {
                    if (isSpeaking) {
                        viewModel.silenceAssistant()
                    } else if (isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Real assistant state label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(avatarEmotion.glowColor.copy(alpha = 0.12f))
                .border(1.dp, avatarEmotion.glowColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                .clickable { onNavigateToBrain() }
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(avatarEmotion.glowColor)
            )
            Text(
                text = avatarEmotion.bengaliLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = avatarEmotion.glowColor
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Real-time voice interface: waveform driven by actual mic RMS
        VoiceWaveform(
            isActive = isListening || isSpeaking,
            rmsLevel = if (isListening) rmsLevel else if (isSpeaking) 0.55f else 0f
        )

        // Mic state + language line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isListening) "🎙 Listening…" else "🎙 Microphone: ${if (diagnostics.hasMicPermission) "ready" else "permission required"}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isListening) EmeraldSuccess else TextMuted
            )
            Text(
                text = "Lang: $languageCode",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. TALK TO AROHI — the real voice pipeline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            CyanPrimary.copy(alpha = 0.28f),
                            VioletBright.copy(alpha = 0.28f),
                            MagentaAccent.copy(alpha = 0.28f)
                        )
                    )
                )
                .border(
                    1.dp,
                    if (isListening) EmeraldSuccess else Color(0x338B5CF6),
                    RoundedCornerShape(22.dp)
                )
                .clickable {
                    if (isListening) viewModel.stopListening() else viewModel.startListening()
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Talk",
                    tint = if (isListening) EmeraldSuccess else Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isListening) "LISTENING — TAP TO STOP" else "TALK TO AROHI",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. BACKGROUND ASSISTANT — real subsystem states
        val bgStatusColor = when {
            isBgActive -> EmeraldSuccess
            diagnostics.isNotificationListenerActive || diagnostics.isAccessibilityActive -> Color(0xFFF59E0B)
            else -> MagentaAccent
        }
        val bgStatusLabel = when {
            isBgActive -> "RUNNING"
            diagnostics.isNotificationListenerActive || diagnostics.isAccessibilityActive -> "LIMITED"
            else -> "STOPPED"
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x181E293B), Color(0x220D1222))
                    )
                )
                .border(1.dp, bgStatusColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BACKGROUND ASSISTANT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = bgStatusLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = bgStatusColor
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isBgActive) EmeraldSuccess.copy(alpha = 0.16f) else Color(0x1AFFFFFF))
                        .border(
                            1.dp,
                            if (isBgActive) EmeraldSuccess.copy(alpha = 0.5f) else Color(0x22FFFFFF),
                            RoundedCornerShape(999.dp)
                        )
                        .clickable {
                            if (isBgActive) {
                                viewModel.stopBackgroundOperatingService()
                            } else if (Build.VERSION.SDK_INT < 33 ||
                                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.startBackgroundOperatingService()
                            } else {
                                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (isBgActive) "TURN OFF" else "TURN ON",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isBgActive) EmeraldSuccess else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SubsystemRow("Voice engine", diagnostics.hasMicPermission)
            SubsystemRow("Notification listener", diagnostics.isNotificationListenerActive)
            SubsystemRow("Gemini connection", diagnostics.isGeminiConnected)
            SubsystemRow("Background service", isBgActive)
            SubsystemRow("Accessibility", diagnostics.isAccessibilityActive)
            SubsystemRow("Overlay indicator", com.example.service.ArohiOverlayService.canDrawOverlays(context))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Quick actions grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionGridCard(
                    title = "Tasks",
                    icon = Icons.Default.Checklist,
                    glowColor = MagentaAccent,
                    onClick = onNavigateToSmartTasks,
                    modifier = Modifier.weight(1f)
                )
                ActionGridCard(
                    title = "Device",
                    icon = Icons.Default.PhoneAndroid,
                    glowColor = CyanPrimary,
                    onClick = onNavigateToDashboard,
                    modifier = Modifier.weight(1f)
                )
                ActionGridCard(
                    title = "Vision",
                    icon = Icons.Default.RemoveRedEye,
                    glowColor = VioletBright,
                    onClick = onNavigateToVision,
                    modifier = Modifier.weight(1f)
                )
                ActionGridCard(
                    title = "Inbox",
                    icon = Icons.Default.Notifications,
                    glowColor = EmeraldSuccess,
                    onClick = onNavigateToNotifications,
                    badgeCount = unreadCount,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionGridCard(
                    title = "Apps",
                    icon = Icons.Default.Apps,
                    glowColor = VioletBright,
                    onClick = onNavigateToApps,
                    modifier = Modifier.weight(1f)
                )
                ActionGridCard(
                    title = "Calls",
                    icon = Icons.Default.Call,
                    glowColor = EmeraldSuccess,
                    onClick = onNavigateToCalls,
                    modifier = Modifier.weight(1f)
                )
                ActionGridCard(
                    title = "Brain",
                    icon = Icons.Default.Psychology,
                    glowColor = CyanPrimary,
                    onClick = onNavigateToBrain,
                    modifier = Modifier.weight(1f)
                )
                ActionGridCard(
                    title = "Memory",
                    icon = Icons.Default.Memory,
                    glowColor = MagentaAccent,
                    onClick = onNavigateToMemory,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. Live telemetry mini cards (real device readings)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeStatusMiniCard(
                title = "Battery",
                value = "${telemetry.batteryPercent}%",
                icon = Icons.Default.BatteryChargingFull,
                tint = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )

            val totalStorage = telemetry.totalStorageGb.toInt().coerceAtLeast(1)
            val storagePct = if (telemetry.totalStorageGb > 0) {
                (((telemetry.totalStorageGb - telemetry.freeStorageGb) / telemetry.totalStorageGb) * 100).toInt()
            } else 0
            HomeStatusMiniCard(
                title = "Storage",
                value = "$storagePct%",
                icon = Icons.Default.Storage,
                tint = CyanPrimary,
                modifier = Modifier.weight(1f)
            )

            val totalRam = telemetry.totalRamMb.coerceAtLeast(1)
            val ramPct = if (telemetry.totalRamMb > 0) {
                (((telemetry.totalRamMb - telemetry.freeRamMb).toFloat() / totalRam) * 100).toInt()
            } else 0
            HomeStatusMiniCard(
                title = "RAM",
                value = "$ramPct%",
                icon = Icons.Default.Memory,
                tint = VioletBright,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 8. Routine quick trigger + settings strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeLinkChip("Routines", onClick = onNavigateToRoutines, modifier = Modifier.weight(1f))
            HomeLinkChip("Settings", onClick = onNavigateToSettings, modifier = Modifier.weight(1f))
            HomeLinkChip("Diagnostics", onClick = onNavigateToDiagnostics, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SubsystemRow(label: String, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (active) EmeraldSuccess else MagentaAccent)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (active) "READY" else "LIMITED",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (active) EmeraldSuccess else TextMuted
            )
        }
    }
}

@Composable
private fun HomeLinkChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x0DFFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = CyanPrimary
        )
    }
}

@Composable
fun HomeStatusMiniCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x141E293B),
                        Color(0x220D1222)
                    )
                )
            )
            .border(1.dp, Color(0x1A8B5CF6), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }
    }
}

@Composable
fun ActionGridCard(
    title: String,
    icon: ImageVector,
    glowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x18182440),
                        Color(0x220D1222)
                    )
                )
            )
            .border(1.dp, Color(0x228B5CF6), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(glowColor.copy(alpha = 0.18f))
                        .border(1.dp, glowColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = glowColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MagentaAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 9) "9+" else "$badgeCount",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
