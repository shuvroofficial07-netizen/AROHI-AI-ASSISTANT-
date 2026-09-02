package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.service.DiagnosticStatusLevel
import com.example.device.batteryText
import com.example.device.orUnavailable
import com.example.device.ramUsedPercent
import com.example.device.storageUsedPercent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletSecondary
import com.example.ui.viewmodel.ArohiViewModel
import com.example.voice.SpeechState

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
    modifier: Modifier = Modifier
) {
    val speechState by viewModel.speechState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val isListening = speechState == SpeechState.LISTENING

    // Android 13+ requires POST_NOTIFICATIONS for the persistent foreground service notification
    val context = LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.startBackgroundOperatingService() }

    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_glow"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header with "A" Icon, "Arohi AI Assistant", "by Shù Vrô", and Scanner Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Circular Logo Badge with "A"
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

                // App Title & Author
                Column {
                    Text(
                        text = "AROHI AI ASSISTANT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "by Shù Vrô",
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        color = MagentaAccent
                    )
                }
            }

            // Scanner / Reticle Action Icon
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
                    contentDescription = "Scan",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val report by viewModel.diagnosticReport.collectAsState()
        val isBgActive = report.items.find { it.id == "background_service" }?.status == DiagnosticStatusLevel.READY

        // 2. Status & Background Assistant Real Switch Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Diagnostics Status Badge (Clickable to open deep diagnostics)
            val (statusColor, statusText) = when (report.overallStatus) {
                DiagnosticStatusLevel.READY -> Pair(EmeraldSuccess, "SYSTEM: READY")
                DiagnosticStatusLevel.LIMITED -> Pair(Color(0xFFF59E0B), "SYSTEM: LIMITED")
                DiagnosticStatusLevel.ERROR -> Pair(MagentaAccent, "SYSTEM: ERROR")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                    .clickable { onNavigateToDiagnostics() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = statusColor
                )
            }

            // Real Background Assistant Switch Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isBgActive) EmeraldSuccess.copy(alpha = 0.15f) else Color(0x1AFFFFFF))
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
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                val bgStateColor = if (isBgActive) EmeraldSuccess else Color(0xFFEF4444)
                val bgStateLabel = if (isBgActive) "BG: RUNNING" else "BG: STOPPED"
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(bgStateColor)
                )
                Text(
                    text = bgStateLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isBgActive) EmeraldSuccess else Color(0xFFEF4444)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Cyberpunk Anime AI Assistant Hero Avatar
        Box(
            modifier = Modifier
                .size(210.dp)
                .clickable {
                    if (isSpeaking) {
                        viewModel.silenceAssistant()
                    } else if (isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                }
                .testTag("home_avatar_image"),
            contentAlignment = Alignment.Center
        ) {
            // Glowing circular backdrop
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    VioletBright.copy(alpha = 0.4f * pulseGlow),
                                    CyanPrimary.copy(alpha = 0.25f * pulseGlow),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension / 1.5f
                        )
                    }
            )

            // Outer Neon Gradient Border Ring
            Box(
                modifier = Modifier
                    .size(190.dp)
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
                    .padding(3.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.arohi_avatar),
                    contentDescription = "Arohi AI Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Bengali Speech Bubble Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x1A251846),
                            Color(0x22131A33)
                        )
                    )
                )
                .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "আমি প্রস্তুত, বস! বলুন, কী করতে পারি আপনার জন্য?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Quick Actions Grid Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Tasks, System Dashboard, Vision, Notifications
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
                    title = "System",
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
                    title = "Notifs",
                    icon = Icons.Default.Notifications,
                    glowColor = EmeraldSuccess,
                    onClick = onNavigateToNotifications,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Status Overview Cards (Battery, Storage, RAM)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Battery Status Mini Card
            HomeStatusMiniCard(
                title = "Battery",
                value = telemetry.batteryText(),
                icon = Icons.Default.BatteryChargingFull,
                tint = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )

            // Storage Status Mini Card
            HomeStatusMiniCard(
                title = "Storage",
                value = telemetry.storageUsedPercent().orUnavailable(),
                icon = Icons.Default.Storage,
                tint = CyanPrimary,
                modifier = Modifier.weight(1f)
            )

            // RAM Status Mini Card
            HomeStatusMiniCard(
                title = "RAM",
                value = telemetry.ramUsedPercent().orUnavailable(),
                icon = Icons.Default.Memory,
                tint = VioletBright,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Interactive Voice Input Pill Bar (Bottom interactive voice activator)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x221E293B),
                            Color(0x330D1222)
                        )
                    )
                )
                .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(20.dp))
                .clickable {
                    if (isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp)
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
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isListening) MagentaAccent.copy(alpha = 0.25f) else CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (isListening) MagentaAccent else CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = if (isListening) "বলুন, আমি শুনছি..." else "Tap to speak with Arohi...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isListening) MagentaAccent else TextSecondary
                    )
                }

                // Mini sound wave indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(10.dp, 16.dp, 12.dp, 20.dp, 8.dp).forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(if (isListening || isSpeaking) h else 6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CyanPrimary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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
    modifier: Modifier = Modifier
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

