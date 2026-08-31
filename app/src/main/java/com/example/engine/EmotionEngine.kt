package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ArohiEmotion(val bengaliLabel: String, val glowColor: Color, val secondaryColor: Color) {
    IDLE("প্রস্তুত (Idle)", CyanPrimary, VioletBright),
    LISTENING("শুনছি (Listening)", EmeraldSuccess, CyanPrimary),
    THINKING("ভাবছি (Thinking)", VioletBright, MagentaAccent),
    SPEAKING("বলছি (Speaking)", CyanPrimary, MagentaAccent),
    EXECUTING("কাজ করছি (Executing)", AmberWarning, CyanPrimary),
    HAPPY("আনন্দিত (Happy)", EmeraldSuccess, MagentaAccent),
    PLAYFUL("দুষ্টুমি (Playful)", MagentaAccent, CyanPrimary),
    CURIOUS("কৌতূহলী (Curious)", CyanPrimary, AmberWarning),
    FOCUSED("মনোযোগী (Focused)", VioletBright, CyanPrimary),
    CONFUSED("অস্পষ্ট (Confused)", AmberWarning, CrimsonError),
    CONCERNED("চিন্তিত (Concerned)", CrimsonError, AmberWarning),
    EXCITED("উত্তেজিত (Excited)", MagentaAccent, EmeraldSuccess),
    SAD("বিষণ্ণ (Sad)", VioletSecondary, ArohiEmotion.IDLE.glowColor),
    ANNOYED("বিরক্ত (Annoyed)", CrimsonError, MagentaAccent),
    CALM("শান্ত (Calm)", CyanPrimary, VioletSecondary),
    ERROR("ত্রুটি (Error)", CrimsonError, CrimsonError);
}

class EmotionEngine {
    private val _currentEmotion = MutableStateFlow(ArohiEmotion.IDLE)
    val currentEmotion: StateFlow<ArohiEmotion> = _currentEmotion.asStateFlow()

    fun setEmotion(emotion: ArohiEmotion) {
        _currentEmotion.value = emotion
    }

    fun inferEmotionFromText(text: String): ArohiEmotion {
        val lower = text.lowercase()
        return when {
            lower.contains("দুঃখিত") || lower.contains("error") || lower.contains("ব্যর্থ") -> ArohiEmotion.ERROR
            lower.contains("হা হা") || lower.contains("মজা") || lower.contains("awesome") || lower.contains("দারুণ") -> ArohiEmotion.HAPPY
            lower.contains("চিন্তা করবেন না") || lower.contains("শান্ত") -> ArohiEmotion.CALM
            lower.contains("কাজটি সফল") || lower.contains("করে দিয়েছি") || lower.contains("হয়ে গেছে") -> ArohiEmotion.HAPPY
            lower.contains("বুঝতে পারছি না") || lower.contains("আবার বলুন") -> ArohiEmotion.CONFUSED
            lower.contains("সতর্কতা") || lower.contains("সাবধান") -> ArohiEmotion.CONCERNED
            else -> ArohiEmotion.SPEAKING
        }
    }
}
