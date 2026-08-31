package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ArohiViewModel

/**
 * AROHI PERMISSIONS — every row shows the REAL state reported by Android and
 * opens the REAL system permission screen. Nothing is faked or bypassed.
 */
@Composable
fun PermissionsScreen(
    viewModel: ArohiViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val diagnostics by viewModel.diagnostics.collectAsState()
    var refreshTick by remember { mutableStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTick++ }

    fun openSystem(action: String, extra: Intent.() -> Unit = {}) {
        try {
            context.startActivity(Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                extra()
            })
        } catch (e: Exception) {
            // Ignored
        }
    }

    val overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    val batteryUnrestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        (context.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
    } else true
    val postNotifGranted = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val phoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
        PackageManager.PERMISSION_GRANTED
    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    val callGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
        PackageManager.PERMISSION_GRANTED

    // refreshTick read to re-evaluate on returning from system screens
    val _ = refreshTick

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Column {
                Text(
                    text = "AROHI PERMISSIONS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Real states reported by Android",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PermissionRow(
                icon = Icons.Default.Mic,
                name = "Microphone",
                detail = "Voice input via SpeechRecognizer",
                status = if (diagnostics.hasMicPermission) PermissionState.GRANTED else PermissionState.REQUIRED,
                onAction = {
                    permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
            )
            PermissionRow(
                icon = Icons.Default.Notifications,
                name = "Notification Access",
                detail = "AROHI Inbox reads real notifications",
                status = if (diagnostics.isNotificationListenerActive) PermissionState.GRANTED else PermissionState.REQUIRED,
                onAction = { openSystem(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
            )
            PermissionRow(
                icon = Icons.Default.ContactPhone,
                name = "Contacts",
                detail = "Name-based calling & messaging",
                status = if (diagnostics.hasContactsPermission) PermissionState.GRANTED else PermissionState.REQUIRED,
                onAction = { permLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS)) }
            )
            PermissionRow(
                icon = Icons.Default.CameraAlt,
                name = "Camera",
                detail = "Arohi Vision (user-triggered only)",
                status = if (cameraGranted) PermissionState.GRANTED else PermissionState.REQUIRED,
                onAction = { permLauncher.launch(arrayOf(Manifest.permission.CAMERA)) }
            )
            PermissionRow(
                icon = Icons.Default.PhoneAndroid,
                name = "Phone State",
                detail = "Incoming call detection",
                status = if (phoneGranted) PermissionState.GRANTED else PermissionState.REQUIRED,
                onAction = { permLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE)) }
            )
            PermissionRow(
                icon = Icons.Default.Security,
                name = "Direct Calling",
                detail = "One-tap calls without the dialer",
                status = if (callGranted) PermissionState.GRANTED else PermissionState.LIMITED,
                onAction = { permLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE)) }
            )
            PermissionRow(
                icon = Icons.Default.Accessibility,
                name = "Accessibility Service",
                detail = "Screen reading & touch automation",
                status = if (diagnostics.isAccessibilityActive) PermissionState.GRANTED else PermissionState.REQUIRED,
                onAction = { openSystem(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
            )
            PermissionRow(
                icon = Icons.Default.Layers,
                name = "Overlay (Floating Indicator)",
                detail = "Small Arohi pill while background service runs",
                status = if (overlayGranted) PermissionState.GRANTED else PermissionState.LIMITED,
                onAction = {
                    openSystem(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
            )
            PermissionRow(
                icon = Icons.Default.Notifications,
                name = "Notification Posting",
                detail = "Persistent background-service notice (Android 13+)",
                status = if (postNotifGranted) PermissionState.GRANTED else PermissionState.REQUIRED,
                onAction = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                }
            )
            PermissionRow(
                icon = Icons.Default.BatteryAlert,
                name = "Battery Optimization Exemption",
                detail = "Keeps background assistant alive",
                status = if (batteryUnrestricted) PermissionState.GRANTED else PermissionState.LIMITED,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        openSystem(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    }
                }
            )

            // Usage access (foreground app detection)
            val usageGranted = viewModel.telemetry.value.foregroundAppLabel != null
            PermissionRow(
                icon = Icons.Default.Layers,
                name = "Usage Access",
                detail = "Detect current foreground app",
                status = if (usageGranted) PermissionState.GRANTED else PermissionState.LIMITED,
                onAction = {
                    try {
                        openSystem(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    } catch (e: Exception) {
                        openSystem(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    }
                }
            )
        }
    }
}

private enum class PermissionState { GRANTED, LIMITED, REQUIRED }

@Composable
private fun PermissionRow(
    icon: ImageVector,
    name: String,
    detail: String,
    status: PermissionState,
    onAction: () -> Unit
) {
    val (color, label) = when (status) {
        PermissionState.GRANTED -> EmeraldSuccess to "GRANTED"
        PermissionState.LIMITED -> AmberWarning to "LIMITED"
        PermissionState.REQUIRED -> MagentaAccent to "REQUIRED"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x141E293B), Color(0x220D1222))
                )
            )
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onAction)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(detail, fontSize = 11.sp, color = TextMuted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (status == PermissionState.GRANTED) "Settings" else "Open settings",
                fontSize = 9.sp,
                color = TextSecondary
            )
        }
    }
}
