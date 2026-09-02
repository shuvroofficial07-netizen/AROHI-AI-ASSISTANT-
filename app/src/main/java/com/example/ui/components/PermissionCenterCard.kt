package com.example.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.example.service.ArohiAccessibilityService
import com.example.service.ArohiNotificationListenerService
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

private data class PermissionRow(
    val title: String,
    val why: String,
    val granted: Boolean,
    val onFix: () -> Unit
)

/**
 * Permission Center — explains every permission BEFORE asking, shows the real granted state
 * (re-read every time the screen resumes) and opens the correct Android settings page.
 *
 * Android does not allow an app to grant itself permissions, so nothing here is automatic.
 */
@Composable
fun PermissionCenterCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }

    // Re-read the real permission state whenever the user comes back from Android settings.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshKey++ }

    val rows = remember(refreshKey) {
        buildList {
            add(
                PermissionRow(
                    title = "Microphone",
                    why = "ভয়েস কমান্ড শোনার জন্য। না দিলে টেক্সট চ্যাট কাজ করবে, ভয়েস নয়।",
                    granted = context.hasPermission(Manifest.permission.RECORD_AUDIO),
                    onFix = { runtimeLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) }
                )
            )
            add(
                PermissionRow(
                    title = "Contacts",
                    why = "নাম বলে কল করার জন্য কন্টাক্ট খুঁজতে হয়। না দিলে শুধু নম্বর দিয়ে কল হবে।",
                    granted = context.hasPermission(Manifest.permission.READ_CONTACTS),
                    onFix = { runtimeLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS)) }
                )
            )
            add(
                PermissionRow(
                    title = "Phone / Call",
                    why = "সরাসরি কল করার জন্য। না দিলে Arohi ডায়ালার খুলে দেবে।",
                    granted = context.hasPermission(Manifest.permission.CALL_PHONE),
                    onFix = {
                        runtimeLauncher.launch(
                            arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)
                        )
                    }
                )
            )
            add(
                PermissionRow(
                    title = "Camera",
                    why = "Vision AI দিয়ে আপনি যা দেখাচ্ছেন তা বিশ্লেষণ করার জন্য।",
                    granted = context.hasPermission(Manifest.permission.CAMERA),
                    onFix = { runtimeLauncher.launch(arrayOf(Manifest.permission.CAMERA)) }
                )
            )
            if (Build.VERSION.SDK_INT >= 33) {
                add(
                    PermissionRow(
                        title = "Notifications (post)",
                        why = "ব্যাকগ্রাউন্ড সার্ভিসের স্থায়ী নোটিফিকেশন দেখানোর জন্য।",
                        granted = context.hasPermission("android.permission.POST_NOTIFICATIONS"),
                        onFix = { runtimeLauncher.launch(arrayOf("android.permission.POST_NOTIFICATIONS")) }
                    )
                )
            }
            add(
                PermissionRow(
                    title = "Notification access",
                    why = "মেসেজ নোটিফিকেশন পড়ে জানানোর জন্য। এটি Android Settings থেকে দিতে হয়।",
                    granted = ArohiNotificationListenerService.isNotificationAccessGranted(context),
                    onFix = {
                        context.openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    }
                )
            )
            add(
                PermissionRow(
                    title = "Accessibility",
                    why = "অন্য অ্যাপে বাটন চেনা ও ট্যাপ করার জন্য। শুধু আপনি চালু করলেই কাজ করে।",
                    granted = ArohiAccessibilityService.isAccessibilityPermissionGranted(context),
                    onFix = { context.openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
                )
            )
            add(
                PermissionRow(
                    title = "Display over other apps",
                    why = "ফ্লোটিং Arohi ইন্ডিকেটর দেখানোর জন্য।",
                    granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        Settings.canDrawOverlays(context),
                    onFix = {
                        context.openSettings(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    }
                )
            )
            add(
                PermissionRow(
                    title = "Battery optimisation",
                    why = "ব্যাকগ্রাউন্ড সার্ভিস যেন তাড়াতাড়ি বন্ধ না হয়। তবুও Android/Samsung বন্ধ করতে পারে।",
                    granted = context.isIgnoringBatteryOptimizations(),
                    onFix = { context.openSettings(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) }
                )
            )
        }
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "PERMISSION CENTER",
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = CyanPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "প্রতিটি পারমিশন কেন দরকার তা আগে ব্যাখ্যা করা হয়েছে। Android নিজে থেকে পারমিশন দেওয়ার সুযোগ দেয় না — আপনি চাইলে তবেই খোলা হবে।",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (row.granted) EmeraldSuccess else TextMuted)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(text = row.why, fontSize = 10.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (row.granted) "GRANTED" else "OPEN",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (row.granted) EmeraldSuccess else CyanPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                (if (row.granted) EmeraldSuccess else CyanPrimary).copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !row.granted) { row.onFix() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.isIgnoringBatteryOptimizations(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager ?: return false
    return pm.isIgnoringBatteryOptimizations(packageName)
}

/** Opens a real Android settings page, and says so honestly when the device has none. */
private fun Context.openSettings(action: String, data: Uri? = null) {
    val intent = Intent(action).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (data != null) this.data = data
    }
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            this,
            "এই ডিভাইসে এই সেটিংস স্ক্রিনটি নেই।",
            Toast.LENGTH_LONG
        ).show()
    }
}
