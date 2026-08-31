package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import com.example.service.DiagnosticStatusLevel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.example.ui.theme.AmberWarning
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel

@Composable
fun SettingsScreen(
    viewModel: ArohiViewModel,
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToGemini: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToBrain: () -> Unit = {},
    onNavigateToApps: () -> Unit = {},
    onNavigateToCalls: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentApiKey by viewModel.apiKeyFlow.collectAsState()

    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var pitchSlider by remember { mutableFloatStateOf(1.15f) }
    var speedSlider by remember { mutableFloatStateOf(1.0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Sleek Header
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "SYSTEM CONFIGURATION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                color = CyanPrimary
            )
            Text(
                text = "আরোহী সেটিংস",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick access to full control centers
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsLinkRow("Permission Center", "সব পারমিশনের লাইভ স্ট্যাটাস", onClick = onNavigateToPermissions)
                SettingsLinkRow("Gemini AI Control Center", "API key, model, connection test", onClick = onNavigateToGemini)
                SettingsLinkRow("Arohi Brain", "প্রসেসিং পাইপলাইন ও টাস্ক স্ট্যাটাস", onClick = onNavigateToBrain)
                SettingsLinkRow("Apps", "ইনস্টল করা অ্যাপ ও ভয়েস কমান্ড", onClick = onNavigateToApps)
                SettingsLinkRow("Calls", "কল স্ট্যাটাস ও কন্টাক্ট", onClick = onNavigateToCalls)
                SettingsLinkRow("About & Support", "Arohi AI Assistant by Shù Vrô", onClick = onNavigateToAbout)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Personality & Assistant Behavior (real persisted preferences)
        val personalityStyle by viewModel.personalityStyleFlow.collectAsState()
        val languageCode by viewModel.languageCodeFlow.collectAsState()
        val privateMode by viewModel.privateModeFlow.collectAsState()

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MagentaAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পার্সোনালিটি ও ভাষা",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Personality style chips — default: Warm Bengali Companion (বস)
                Text(
                    text = "Personality",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    com.example.data.repository.SettingsRepository.PERSONALITY_STYLES.take(3).forEach { style ->
                        val selected = style == personalityStyle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) MagentaAccent.copy(alpha = 0.2f) else Color(0x0DFFFFFF))
                                .border(
                                    1.dp,
                                    if (selected) MagentaAccent.copy(alpha = 0.6f) else Color(0x1AFFFFFF),
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable { viewModel.setPersonalityStyle(style) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = style,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MagentaAccent else TextSecondary
                            )
                        }
                    }
                }

                // Language selector — real SpeechRecognizer locales
                Text(
                    text = "Language (ভয়েস কমান্ড)",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    com.example.data.repository.SettingsRepository.LANGUAGE_OPTIONS.take(5).forEach { (code, label) ->
                        val selected = code == languageCode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) CyanPrimary.copy(alpha = 0.18f) else Color(0x0DFFFFFF))
                                .border(
                                    1.dp,
                                    if (selected) CyanPrimary.copy(alpha = 0.6f) else Color(0x1AFFFFFF),
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable { viewModel.setLanguageCode(code) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label.substringBefore(" (").substringBefore(" "),
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) CyanPrimary else TextSecondary
                            )
                        }
                    }
                }

                // Privacy: Private Mode toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0DFFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Private Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "নোটিফিকেশন প্রিভিউ লুকানো থাকবে",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = privateMode,
                        onCheckedChange = { viewModel.setPrivateMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmberWarning,
                            checkedTrackColor = AmberWarning.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Notifications, Calls & Behavior (real persisted toggles)
        val callerAnnouncement by viewModel.callerAnnouncementFlow.collectAsState()
        val notificationAnnouncement by viewModel.notificationAnnouncementFlow.collectAsState()
        val proactiveEnabled by viewModel.proactiveEnabledFlow.collectAsState()
        val silenceMode by viewModel.silenceModeFlow.collectAsState()

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "নোটিফিকেশন, কল ও আচরণ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                ToggleRow(
                    title = "Notification announcements",
                    subtitle = "ক্যাপচার করা নোটিফিকেশন পড়ে শোনাবে",
                    checked = notificationAnnouncement,
                    onCheckedChange = { viewModel.setNotificationAnnouncement(it) }
                )
                ToggleRow(
                    title = "Caller announcements",
                    subtitle = "ইনকামিং কলের নাম পড়ে শোনাবে",
                    checked = callerAnnouncement,
                    onCheckedChange = { viewModel.setCallerAnnouncement(it) }
                )
                ToggleRow(
                    title = "Proactive conversation",
                    subtitle = "নিজে থেকে কথা বলা (প্রয়োজনে)",
                    checked = proactiveEnabled,
                    onCheckedChange = { viewModel.setProactiveEnabled(it) }
                )
                ToggleRow(
                    title = "Silence mode",
                    subtitle = "কণ্ঠস্বর সম্পূর্ণ বন্ধ (\"চুপ করো\" সমর্থিত)",
                    checked = silenceMode,
                    onCheckedChange = { viewModel.setSilenceMode(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Gemini AI Configuration Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini API কনফিগারেশন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Gemini API Key", color = TextSecondary) },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle API Key Visibility",
                                tint = TextMuted
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        focusedContainerColor = Color(0x0DFFFFFF),
                        unfocusedContainerColor = Color(0x0DFFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { viewModel.checkGeminiConnection(apiKeyInput) },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary)
                    ) {
                        Text("টেস্ট লিঙ্ক", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { viewModel.saveApiKey(apiKeyInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_api_key_btn")
                    ) {
                        Text("সেভ করুন", color = Color(0xFF020205), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // System Diagnostics & Subsystem Health Card
        val report by viewModel.diagnosticReport.collectAsState()
        val isChecking by viewModel.isDiagnosticsChecking.collectAsState()

        val (diagColor, diagTitle) = when (report.overallStatus) {
            DiagnosticStatusLevel.READY -> Pair(EmeraldSuccess, "ALL SYSTEMS READY")
            DiagnosticStatusLevel.LIMITED -> Pair(Color(0xFFF59E0B), "STATUS: LIMITED")
            DiagnosticStatusLevel.ERROR -> Pair(MagentaAccent, "STATUS: ERROR")
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = diagColor.copy(alpha = 0.4f)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = diagColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "সিস্টেম ডায়াগনস্টিকস ও হেলথ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Monospace Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(diagColor.copy(alpha = 0.15f))
                            .border(1.dp, diagColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(diagColor)
                        )
                        Text(
                            text = when (report.overallStatus) {
                                DiagnosticStatusLevel.READY -> "READY"
                                DiagnosticStatusLevel.LIMITED -> "LIMITED"
                                DiagnosticStatusLevel.ERROR -> "ERROR"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = diagColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Gemini AI, ব্যাকগ্রাউন্ড সার্ভিস, ক্যামেরা ও সিস্টেম পারমিশনের লাইভ স্ট্যাটাস।",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ready: ${report.readyCount} | Limited: ${report.limitedCount} | Error: ${report.errorCount}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )

                    OutlinedButton(
                        onClick = { viewModel.runFullDiagnostics() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isChecking) "Checking..." else "Run Test",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Voice Synthesis (TTS) Settings
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = VioletBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ভয়েস ও স্পিচ টিউনিং (TTS Settings)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ভয়েস পিচ (Pitch): ${String.format("%.2f", pitchSlider)}x",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Slider(
                    value = pitchSlider,
                    onValueChange = {
                        pitchSlider = it
                        viewModel.ttsManager.setVoicePitch(it)
                    },
                    valueRange = 0.8f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = VioletBright,
                        activeTrackColor = VioletBright,
                        inactiveTrackColor = Color(0x1AFFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "কথা বলার গতি (Speed): ${String.format("%.2f", speedSlider)}x",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Slider(
                    value = speedSlider,
                    onValueChange = {
                        speedSlider = it
                        viewModel.ttsManager.setVoiceSpeed(it)
                    },
                    valueRange = 0.7f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary,
                        inactiveTrackColor = Color(0x1AFFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.ttsManager.speak("নমস্কার! আমি আরোহী, আপনার ভয়েস অ্যাসিস্ট্যান্ট।") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x14FFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("ভয়েস টেস্ট করুন (Test Voice)", fontSize = 11.sp, color = CyanPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Official Creator & Support Hub
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowBorder = true
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MagentaAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ডেভেলপার ও অফিসিয়াল সাপোর্ট সেন্টার",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MagentaAccent
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Arohi AI Assistant by Shù Vrô",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
                Text(
                    text = "AROHI AI ASSISTANT — NEXT GENERATION (v14.0.0)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldSuccess
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "নির্মাতা: Shù Vrô (Shuvro)\nইমেইল: shuvroofficial07@gmail.com\nউদ্দেশ্য: স্যামসাং গ্যালাক্সি ও অ্যান্ড্রয়েড প্ল্যাটফর্মে পূর্ণাঙ্গ AI অপারেটিং লেয়ার।",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Support Channels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { openUrl(context, "https://wa.me/8801915551436") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess)
                    ) {
                        Text("WhatsApp", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    OutlinedButton(
                        onClick = { openUrl(context, "https://t.me/Shuvrojr07") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary)
                    ) {
                        Text("Telegram", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    OutlinedButton(
                        onClick = { openUrl(context, "https://www.facebook.com/shuvromridha77") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MagentaAccent.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MagentaAccent)
                    ) {
                        Text("Facebook", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Ignored
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
        Text(
            text = "›",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = CyanPrimary
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyanPrimary,
                checkedTrackColor = CyanPrimary.copy(alpha = 0.3f)
            )
        )
    }
}
