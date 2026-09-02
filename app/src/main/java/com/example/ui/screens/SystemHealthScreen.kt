package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.DiagnosticCategory
import com.example.service.DiagnosticItem
import com.example.service.DiagnosticStatusLevel
import com.example.ui.components.GlassCard
import com.example.ui.theme.ArohiDarkSurface
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel

@Composable
fun SystemHealthScreen(
    viewModel: ArohiViewModel,
    onNavigateToVision: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report by viewModel.diagnosticReport.collectAsState()
    val isChecking by viewModel.isDiagnosticsChecking.collectAsState()
    var selectedCategory by remember { mutableStateOf<DiagnosticCategory?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "spin_refresh")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "REAL-TIME DIAGNOSTICS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    color = CyanPrimary
                )
                Text(
                    text = "সিস্টেম ডায়াগনস্টিকস ও হেলথ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            IconButton(
                onClick = { viewModel.runFullDiagnostics() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, Color(0x22FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Diagnostics",
                    tint = if (isChecking) CyanPrimary else Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .then(if (isChecking) Modifier.rotate(rotationAngle) else Modifier)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Overall Health Status Hero Banner
        OverallHealthHeroCard(
            report = report,
            isChecking = isChecking,
            onRunDiagnostics = { viewModel.runFullDiagnostics() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Category Filter Chips
        val categories = listOf(
            null to "ALL (সবগুলো)",
            DiagnosticCategory.AI_CORE to "AI ENGINE",
            DiagnosticCategory.BACKGROUND_LAYER to "BACKGROUND LAYER",
            DiagnosticCategory.HARDWARE to "HARDWARE & VISION",
            DiagnosticCategory.PERMISSIONS_ACCESS to "PERMISSIONS"
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { (cat, label) ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) CyanPrimary.copy(alpha = 0.2f) else Color(0x14FFFFFF))
                        .border(
                            1.dp,
                            if (isSelected) CyanPrimary else Color(0x1AFFFFFF),
                            RoundedCornerShape(999.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) CyanPrimary else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Filtered Subsystem Items
        val filteredItems = report.items.filter { item ->
            selectedCategory == null || item.category == selectedCategory
        }

        filteredItems.forEach { item ->
            DiagnosticDetailCard(
                item = item,
                onAction = {
                    when (item.id) {
                        "gemini_ai" -> viewModel.checkGeminiConnection(viewModel.apiKeyFlow.value)
                        "network_connectivity" -> {
                            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        }
                        "tts_engine" -> {
                            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                } catch (ex: Exception) { /* no settings available */ }
                            }
                        }
                        "storage" -> {
                            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                } catch (ex: Exception) { }
                            }
                        }
                        "background_service" -> {
                            if (item.status == DiagnosticStatusLevel.READY) {
                                viewModel.stopBackgroundOperatingService()
                            } else {
                                viewModel.startBackgroundOperatingService()
                            }
                        }
                        "camera_vision" -> {
                            if (item.status == DiagnosticStatusLevel.READY) {
                                onNavigateToVision()
                            } else {
                                openAppPermissionSettings(context)
                            }
                        }
                        "accessibility" -> {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        }
                        "notification_listener" -> {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        }
                        "microphone", "contacts" -> {
                            openAppPermissionSettings(context)
                        }
                        "battery_opt" -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    openAppPermissionSettings(context)
                                }
                            }
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun OverallHealthHeroCard(
    report: com.example.service.DiagnosticReport,
    isChecking: Boolean,
    onRunDiagnostics: () -> Unit
) {
    val (statusColor, statusTitle, statusDesc) = when (report.overallStatus) {
        DiagnosticStatusLevel.READY -> Triple(
            EmeraldSuccess,
            "ALL SYSTEMS READY",
            "সবগুলো সাবসিস্টেম ও AI ক্লাউড কানেকশন সক্রিয় রয়েছে।"
        )
        DiagnosticStatusLevel.LIMITED -> Triple(
            Color(0xFFF59E0B),
            "SYSTEM STATUS: LIMITED",
            "কিছু সার্ভিস বা পারমিশন সীমিত রয়েছে (লোকাল ইঞ্জিন সক্রিয়)।"
        )
        DiagnosticStatusLevel.ERROR -> Triple(
            MagentaAccent,
            "SYSTEM STATUS: ERROR",
            "এক বা একাধিক গুরুত্বপূর্ণ সাবসিস্টেমে ত্রুটি পরিলক্ষিত হয়েছে।"
        )
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = statusColor.copy(alpha = 0.4f),
        glowBorder = true
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = statusTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor
                    )
                }

                StatusBadge(status = report.overallStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusDesc,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subsystem Status Count Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountChip(
                    label = "READY",
                    count = report.readyCount,
                    color = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
                CountChip(
                    label = "LIMITED",
                    count = report.limitedCount,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                CountChip(
                    label = "ERROR",
                    count = report.errorCount,
                    color = MagentaAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Run Diagnostics Button
            OutlinedButton(
                onClick = onRunDiagnostics,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CyanPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f))
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = CyanPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ডায়াগনস্টিকস পরীক্ষা চলছে...",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CyanPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RUN FULL DIAGNOSTICS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun CountChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(vertical = 5.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: $count",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
fun DiagnosticDetailCard(
    item: DiagnosticItem,
    onAction: () -> Unit
) {
    val icon = when (item.id) {
        "gemini_ai" -> if (item.status == DiagnosticStatusLevel.READY) Icons.Default.CloudDone else Icons.Default.CloudOff
        "background_service" -> Icons.Default.Security
        "camera_vision" -> Icons.Default.CameraAlt
        "accessibility" -> Icons.Default.Accessibility
        "notification_listener" -> Icons.Default.NotificationsActive
        "microphone" -> Icons.Default.Mic
        "contacts" -> Icons.Default.ContactPhone
        "battery_opt" -> Icons.Default.BatteryAlert
        else -> Icons.Default.Security
    }

    val borderColor = when (item.status) {
        DiagnosticStatusLevel.READY -> Color(0x1FFFFFFF)
        DiagnosticStatusLevel.LIMITED -> Color(0x33F59E0B)
        DiagnosticStatusLevel.ERROR -> MagentaAccent.copy(alpha = 0.4f)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = borderColor
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x14FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = item.name,
                            tint = when (item.status) {
                                DiagnosticStatusLevel.READY -> EmeraldSuccess
                                DiagnosticStatusLevel.LIMITED -> Color(0xFFF59E0B)
                                DiagnosticStatusLevel.ERROR -> MagentaAccent
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = item.summary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (item.status) {
                                DiagnosticStatusLevel.READY -> EmeraldSuccess
                                DiagnosticStatusLevel.LIMITED -> Color(0xFFF59E0B)
                                DiagnosticStatusLevel.ERROR -> MagentaAccent
                            }
                        )
                    }
                }

                StatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.details,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            if (item.latencyMs != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Latency: ${item.latencyMs} ms",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyanPrimary
                )
            }

            if (item.actionText != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onAction,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = when (item.status) {
                                DiagnosticStatusLevel.READY -> CyanPrimary
                                DiagnosticStatusLevel.LIMITED -> Color(0xFFF59E0B)
                                DiagnosticStatusLevel.ERROR -> MagentaAccent
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (item.status) {
                                DiagnosticStatusLevel.READY -> CyanPrimary.copy(alpha = 0.4f)
                                DiagnosticStatusLevel.LIMITED -> Color(0x66F59E0B)
                                DiagnosticStatusLevel.ERROR -> MagentaAccent.copy(alpha = 0.5f)
                            }
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = item.actionText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: DiagnosticStatusLevel) {
    val (bgColor, textColor, label, icon) = when (status) {
        DiagnosticStatusLevel.READY -> Quadruple(
            EmeraldSuccess.copy(alpha = 0.15f),
            EmeraldSuccess,
            "READY",
            Icons.Default.CheckCircle
        )
        DiagnosticStatusLevel.LIMITED -> Quadruple(
            Color(0xFFF59E0B).copy(alpha = 0.15f),
            Color(0xFFF59E0B),
            "LIMITED",
            Icons.Default.WarningAmber
        )
        DiagnosticStatusLevel.ERROR -> Quadruple(
            MagentaAccent.copy(alpha = 0.15f),
            MagentaAccent,
            "ERROR",
            Icons.Default.ErrorOutline
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun openAppPermissionSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {}
}
