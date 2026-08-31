package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.repository.SettingsRepository
import com.example.service.ArohiAccessibilityService
import com.example.service.ArohiNotificationListenerService
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel

/**
 * Guided first-launch setup. Every step reflects REAL permission state and
 * opens the REAL Android permission/settings screens — nothing is bypassed.
 */
@Composable
fun FirstRunSetupScreen(
    viewModel: ArohiViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 13

    val diagnostics by viewModel.diagnostics.collectAsState()
    val privateMode by viewModel.privateModeFlow.collectAsState()
    val languageCode by viewModel.languageCodeFlow.collectAsState()
    val personality by viewModel.personalityStyleFlow.collectAsState()
    val apiKey by viewModel.apiKeyFlow.collectAsState()
    val report by viewModel.diagnosticReport.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    fun openSettings(action: String, extra: Intent.() -> Unit = {}) {
        try {
            context.startActivity(Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                extra()
            })
        } catch (e: Exception) {
            // Ignored
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Progress header
        Text(
            text = "FIRST-LAUNCH SETUP — ${step + 1}/$totalSteps",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
            color = CyanPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x1AFFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((step + 1) / totalSteps.toFloat())
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(CyanPrimary, VioletBright, MagentaAccent)))
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (step) {
                0 -> {
                    StepTitle("Welcome to Arohi", Icons.Default.AutoAwesome, CyanPrimary)
                    StepBody(
                        "আমি আরোহী — Arohi AI Assistant by Shù Vrô।\n\n" +
                            "আমি আপনার ফোনের সত্যিকারের AI অপারেটিং লেয়ার: ভয়েস, Gemini ক্লাউড AI, নোটিফিকেশন, কল, ক্যামেরা, মেমোরি ও অটোমেশন — সবকিছু রিয়েল।\n\n" +
                            "পরের ধাপগুলোতে শুধু সেই পারমিশনগুলোই চাওয়া হবে যেগুলো আমার কোনো না কোনো বাস্তব ফিচারের জন্য দরকার।"
                    )
                }
                1 -> {
                    StepTitle("Microphone", Icons.Default.Mic, EmeraldSuccess)
                    StepBody("কথা বলার জন্য মাইক্রোফোন দরকার — SpeechRecognizer দিয়ে আপনার ভয়েস শুনে আমি কাজ করি।")
                    val granted = diagnostics.hasMicPermission
                    StatusLine(granted, "Granted", "Not granted")
                    SetupButton(if (granted) "Granted ✓" else "Grant microphone") {
                        if (!granted) permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    }
                }
                2 -> {
                    StepTitle("Notifications", Icons.Default.Notifications, VioletBright)
                    StepBody("AROHI Inbox-এর জন্য Notification Access দরকার — আসল নোটিফিকেশন পড়ে আমি আপনাকে জানাই।")
                    val granted = diagnostics.isNotificationListenerActive
                    StatusLine(granted, "Access granted", "Access required")
                    SetupButton(if (granted) "Granted ✓" else "Open notification access") {
                        if (!granted) openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    }
                }
                3 -> {
                    StepTitle("Contacts", Icons.Default.ContactPhone, CyanPrimary)
                    StepBody("\"Rahim কে কল দাও\" — নাম ধরে কল করতে কন্টাক্ট পড়া দরকার।")
                    val granted = diagnostics.hasContactsPermission
                    StatusLine(granted, "Granted", "Not granted")
                    SetupButton(if (granted) "Granted ✓" else "Grant contacts") {
                        if (!granted) permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                    }
                }
                4 -> {
                    StepTitle("Camera (Arohi Vision)", Icons.Default.CameraAlt, MagentaAccent)
                    StepBody("Arohi Vision-এ ক্যামেরা ব্যবহার হয় শুধু তখনই, যখন আপনি নিজে Start Vision চাপবেন।")
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    StatusLine(granted, "Granted", "Not granted")
                    SetupButton(if (granted) "Granted ✓" else "Grant camera") {
                        if (!granted) permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    }
                }
                5 -> {
                    StepTitle("Phone (Call Intelligence)", Icons.Default.PhoneAndroid, EmeraldSuccess)
                    StepBody("ইনকামিং কল শনাক্ত করতে ও কলার নাম বলতে READ_PHONE_STATE দরকার। সরাসরি কলের জন্য CALL_PHONE।")
                    val phoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                        PackageManager.PERMISSION_GRANTED
                    StatusLine(phoneGranted, "Granted", "Not granted")
                    SetupButton(if (phoneGranted) "Granted ✓" else "Grant phone access") {
                        if (!phoneGranted) permissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE)
                        )
                    }
                }
                6 -> {
                    StepTitle("Accessibility (Screen Control)", Icons.Default.Settings, VioletBright)
                    StepBody("স্ক্রিন পড়া, বাটন ক্লিক, স্ক্রল ও Back/Home নিয়ন্ত্রণের জন্য Accessibility Service চালু করুন। আমি কখনো PIN/পাসওয়ার্ড বাইপাস করি না।")
                    val granted = diagnostics.isAccessibilityActive
                    StatusLine(granted, "Enabled", "Disabled")
                    SetupButton(if (granted) "Enabled ✓" else "Open accessibility settings") {
                        if (!granted) openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    }
                }
                7 -> {
                    StepTitle("Floating Indicator (Overlay)", Icons.Default.AutoAwesome, CyanPrimary)
                    StepBody("ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট চলাকালে ছোট ফ্লোটিং Arohi পিল দেখাতে overlay পারমিশন দরকার।")
                    val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
                    StatusLine(granted, "Allowed", "Not allowed")
                    SetupButton(if (granted) "Allowed ✓" else "Open overlay settings") {
                        if (!granted) openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    }
                }
                8 -> {
                    StepTitle("Background Assistant", Icons.Default.Security, EmeraldSuccess)
                    StepBody("ব্যাকগ্রাউন্ডে AROHI চালু রাখতে ফোরগ্রাউন্ড সার্ভিস। Android/Samsung রেস্ট্রিকশন মানা হবে এবং বাস্তব সীমাবদ্ধতা দেখানো হবে।")
                    val granted = diagnostics.isBackgroundServiceActive
                    StatusLine(granted, "Running", "Stopped")
                    SetupButton(if (granted) "Running ✓" else "Start background service") {
                        if (!granted) viewModel.startBackgroundOperatingService()
                    }
                }
                9 -> {
                    StepTitle("Gemini AI Configuration", Icons.Default.Key, VioletBright)
                    StepBody("আপনার Gemini API Key দিন (aistudio.google.com/apikey থেকে ফ্রি)। Key আপনার ফোনেই সংরক্ষিত থাকে।")
                    var keyInput by remember { mutableStateOf(apiKey) }
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Gemini API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0x1AFFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    SetupButton("Save & test key") {
                        if (keyInput.isNotBlank()) viewModel.saveApiKey(keyInput)
                    }
                    SetupButton("Skip for now", secondary = true) { }
                }
                10 -> {
                    StepTitle("Privacy Settings", Icons.Default.Lock, MagentaAccent)
                    StepBody("Private Mode চালু থাকলে নোটিফিকেশনের প্রিভিউ লুকানো থাকবে এবং সংবেদনশীল ভয়েস অ্যানাউন্সমেন্ট বন্ধ থাকবে।")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Private Mode", color = Color.White, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = privateMode,
                            onCheckedChange = { viewModel.setPrivateMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MagentaAccent,
                                checkedTrackColor = MagentaAccent.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
                11 -> {
                    StepTitle("Voice & Personality", Icons.Default.RecordVoiceOver, CyanPrimary)
                    StepBody("ভয়েসের ভাষা ও ব্যক্তিত্ব বেছে নিন — পরে সেটিংস থেকে বদলাতে পারবেন।")
                    SettingsRepository.LANGUAGE_OPTIONS.forEach { (code, label) ->
                        val selected = code == languageCode
                        SetupChoiceRow(label, selected) { viewModel.setLanguageCode(code) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsRepository.PERSONALITY_STYLES.forEach { style ->
                        val selected = style == personality
                        SetupChoiceRow(style, selected) { viewModel.setPersonalityStyle(style) }
                    }
                }
                12 -> {
                    StepTitle("System Diagnostics", Icons.Default.WarningAmber, EmeraldSuccess)
                    StepBody("শেষ ধাপ: আসল সিস্টেম চেক।")
                    val ready = report.readyCount
                    val limited = report.limitedCount
                    val error = report.errorCount
                    Text(
                        text = "Ready: $ready | Limited: $limited | Error: $error",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = if (error > 0) MagentaAccent else if (limited > 0) Color(0xFFF59E0B) else EmeraldSuccess
                    )
                    SetupButton("Run full diagnostics") { viewModel.runFullDiagnostics() }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { if (step > 0) step-- }
            ) {
                Text("Back", color = if (step > 0) TextSecondary else TextMuted)
            }
            if (step < totalSteps - 1) {
                Button(
                    onClick = { step++ },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Next", color = Color(0xFF020205), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        viewModel.markSetupComplete()
                        onDone()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Finish — Start Arohi", color = Color(0xFF020205), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StepTitle(title: String, icon: ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.15f))
                .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun StepBody(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = TextSecondary,
        lineHeight = 20.sp
    )
}

@Composable
private fun StatusLine(granted: Boolean, grantedText: String, deniedText: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (granted) EmeraldSuccess else MagentaAccent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (granted) grantedText else deniedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (granted) EmeraldSuccess else MagentaAccent
        )
    }
}

@Composable
private fun SetupChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) CyanPrimary.copy(alpha = 0.15f) else Color(0x0DFFFFFF))
            .border(
                1.dp,
                if (selected) CyanPrimary.copy(alpha = 0.5f) else Color(0x1AFFFFFF),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(label, fontSize = 12.sp, color = if (selected) CyanPrimary else TextSecondary)
    }
}

@Composable
private fun SetupButton(text: String, secondary: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) Color(0x14FFFFFF) else CyanPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = if (secondary) CyanPrimary else Color(0xFF020205),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
