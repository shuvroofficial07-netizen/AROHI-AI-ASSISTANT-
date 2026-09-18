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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ArohiEmotion
import com.example.ui.components.ArohiAvatarStage
import com.example.ui.components.AvatarModeChip
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentPalettes
import com.example.ui.theme.AvatarOutfits
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel

/**
 * Avatar studio + personalisation + accessibility, all wired to real persisted settings.
 */
@Composable
fun AvatarStudioScreen(
    viewModel: ArohiViewModel,
    modifier: Modifier = Modifier
) {
    val emotion by viewModel.emotion.collectAsState()
    val speechState by viewModel.speechState.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val avatarMode by viewModel.avatarModeFlow.collectAsState()
    val outfitId by viewModel.avatarOutfitFlow.collectAsState()
    val accentId by viewModel.themeAccentFlow.collectAsState()
    val intensity by viewModel.personalityIntensityFlow.collectAsState()
    val pronoun by viewModel.pronounStyleFlow.collectAsState()
    val userName by viewModel.userNameFlow.collectAsState()
    val nickname by viewModel.userNicknameFlow.collectAsState()
    val fontScale by viewModel.fontScaleFlow.collectAsState()
    val highContrast by viewModel.highContrastFlow.collectAsState()
    val voiceSpeed by viewModel.voiceSpeedFlow.collectAsState()
    val voicePitch by viewModel.voicePitchFlow.collectAsState()

    var userNameInput by remember(userName) { mutableStateOf(userName) }
    var nicknameInput by remember(nickname) { mutableStateOf(nickname) }
    var speedSlider by remember(voiceSpeed) { mutableFloatStateOf(voiceSpeed) }
    var pitchSlider by remember(voicePitch) { mutableFloatStateOf(voicePitch) }

    val outfit = AvatarOutfits.byId(outfitId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "AVATAR STUDIO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
            color = CyanPrimary
        )
        Text(
            text = "অ্যাভাটার ও ব্যক্তিত্ব",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ArohiAvatarStage(
                emotion = emotion,
                speechState = speechState,
                rmsLevel = rmsLevel,
                isSpeaking = isSpeaking,
                is3D = avatarMode == "3D",
                outfit = outfit,
                modifier = Modifier.testTag("avatar_studio_preview"),
                size = 210.dp,
                onClick = { viewModel.speakText("নমস্কার! আমি আরোহী।") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            AvatarModeChip(
                label = "2D পোর্ট্রেট",
                selected = avatarMode == "2D",
                onClick = { viewModel.setAvatarMode("2D") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            AvatarModeChip(
                label = "3D অ্যানিমেটেড",
                selected = avatarMode == "3D",
                onClick = { viewModel.setAvatarMode("3D") }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            AvatarModeChip(
                label = emotion.bengaliLabel,
                selected = true,
                onClick = { viewModel.previewEmotion(ArohiEmotion.IDLE) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ----------------------------------------------------------- expression previews
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SectionHeader(Icons.Default.Face, "অ্যাভাটার এক্সপ্রেশন প্রিভিউ")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "আরোহী প্রতিটি উত্তরের সাথে ইমোশন ট্যাগ পাঠায়, সেই অনুযায়ী মুখ বদলায়।",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                val previews = listOf(
                    ArohiEmotion.HAPPY,
                    ArohiEmotion.EXCITED,
                    ArohiEmotion.THINKING,
                    ArohiEmotion.CONCERNED,
                    ArohiEmotion.PLAYFUL,
                    ArohiEmotion.SAD,
                    ArohiEmotion.CURIOUS,
                    ArohiEmotion.CALM
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    previews.take(4).forEach { item ->
                        ChipButton(
                            label = item.bengaliLabel.substringBefore(" ("),
                            selected = emotion == item,
                            modifier = Modifier.weight(1f)
                        ) { viewModel.previewEmotion(item) }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    previews.drop(4).forEach { item ->
                        ChipButton(
                            label = item.bengaliLabel.substringBefore(" ("),
                            selected = emotion == item,
                            modifier = Modifier.weight(1f)
                        ) { viewModel.previewEmotion(item) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ----------------------------------------------------------- personality
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SectionHeader(Icons.Default.Tune, "পার্সোনালিটি ইনটেনসিটি")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        intensity < 0.34f -> "ফোকাসড মোড — কম কথা, সরাসরি উত্তর।"
                        intensity < 0.67f -> "ব্যালান্সড মোড — উষ্ণ, মাঝে মাঝে হালকা রসিকতা।"
                        else -> "প্লেফুল মোড — দুষ্টুমি, মজার টিপ্পনী সহ।"
                    },
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Slider(
                    value = intensity,
                    onValueChange = { viewModel.setPersonalityIntensity(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MagentaAccent,
                        activeTrackColor = MagentaAccent,
                        inactiveTrackColor = Color(0x1AFFFFFF)
                    )
                )
                Text(
                    text = "ফোকাসড  ←→  প্লেফুল",
                    fontSize = 10.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("সম্বোধন", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AvatarModeChip(
                        label = "তুমি (আপনার মতো বন্ধু)",
                        selected = pronoun == "tumi",
                        onClick = { viewModel.setPronounStyle("tumi") }
                    )
                    AvatarModeChip(
                        label = "আপনি (ভদ্র)",
                        selected = pronoun == "apni",
                        onClick = { viewModel.setPronounStyle("apni") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = { userNameInput = it },
                    label = { Text("তোমার নাম (আরোহী মনে রাখবে)", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_name_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = darkField()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { nicknameInput = it },
                    label = { Text("ডাকনাম", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = darkField()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.setUserName(userNameInput)
                        viewModel.setUserNickname(nicknameInput)
                        viewModel.speakText("ঠিক আছে, আমি তোমাকে ${nicknameInput.ifBlank { userNameInput }} বলে ডাকব।")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletBright),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("নাম সেভ করুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ----------------------------------------------------------- theme & outfit
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SectionHeader(Icons.Default.Palette, "থিম কালার ও আউটফিট")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentPalettes.all.forEach { accent ->
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.primary.copy(alpha = if (accent.id == accentId) 0.45f else 0.18f))
                                .border(
                                    width = if (accent.id == accentId) 2.dp else 1.dp,
                                    color = accent.primary,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setThemeAccent(accent.id) }
                                .testTag("accent_${accent.id}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("অ্যাভাটার আউটফিট", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AvatarOutfits.all.take(3).forEach { item ->
                        ChipButton(
                            label = item.label,
                            selected = item.id == outfitId,
                            modifier = Modifier.weight(1f)
                        ) { viewModel.setAvatarOutfit(item.id) }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AvatarOutfits.all.drop(3).forEach { item ->
                        ChipButton(
                            label = item.label,
                            selected = item.id == outfitId,
                            modifier = Modifier.weight(1f)
                        ) { viewModel.setAvatarOutfit(item.id) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ----------------------------------------------------------- accessibility + voice
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SectionHeader(Icons.Default.Accessibility, "অ্যাক্সেসিবিলিটি ও ভয়েস")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ফন্ট সাইজ: ${(fontScale * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Slider(
                    value = fontScale,
                    onValueChange = { viewModel.setFontScale(it) },
                    valueRange = 0.85f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary,
                        inactiveTrackColor = Color(0x1AFFFFFF)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("হাই-কনট্রাস্ট মোড", fontSize = 12.sp, color = Color.White)
                    Switch(checked = highContrast, onCheckedChange = { viewModel.setHighContrast(it) })
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "কথার গতি: ${String.format("%.2f", speedSlider)}x",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Slider(
                    value = speedSlider,
                    onValueChange = {
                        speedSlider = it
                        viewModel.setVoiceSpeed(it)
                    },
                    valueRange = 0.7f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary,
                        inactiveTrackColor = Color(0x1AFFFFFF)
                    )
                )
                Text(
                    text = "ভয়েস পিচ: ${String.format("%.2f", pitchSlider)}x",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Slider(
                    value = pitchSlider,
                    onValueChange = {
                        pitchSlider = it
                        viewModel.setVoicePitch(it)
                    },
                    valueRange = 0.8f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = VioletBright,
                        activeTrackColor = VioletBright,
                        inactiveTrackColor = Color(0x1AFFFFFF)
                    )
                )

                Button(
                    onClick = { viewModel.speakText("নমস্কার! আমি আরোহী, তোমার ব্যক্তিগত সঙ্গী।") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x14FFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = CyanPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ভয়েস টেস্ট করুন", fontSize = 11.sp, color = CyanPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun ChipButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) CyanPrimary.copy(alpha = 0.18f) else Color(0x0DFFFFFF))
            .border(
                1.dp,
                if (selected) CyanPrimary.copy(alpha = 0.6f) else Color(0x1AFFFFFF),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (selected) CyanPrimary else TextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun darkField() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = Color(0x1AFFFFFF),
    focusedContainerColor = Color(0x0DFFFFFF),
    unfocusedContainerColor = Color(0x0DFFFFFF),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
