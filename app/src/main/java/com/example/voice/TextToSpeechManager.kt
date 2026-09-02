package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(
    private val context: Context,
    private val onSpeakingStarted: () -> Unit = {},
    private val onSpeakingFinished: () -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPitch = 1.15f
    private var currentSpeed = 1.0f
    private var currentLanguageTag: String = "bn-BD"

    /** Real readiness flag used by diagnostics (spec §58 TTS). */
    fun isReady(): Boolean = isInitialized && (tts != null)

    /** True when a TTS engine exists on the device (set asynchronously after init). */
    fun isEnginePresent(): Boolean = tts != null

    fun currentLanguageLabel(): String = currentLanguageTag

    /** Sets the TTS voice language; returns true only if the language is usable. */
    fun setLanguage(languageTag: String): Boolean {
        val engine = tts ?: return false
        currentLanguageTag = languageTag
        val locale = when (languageTag) {
            "en-US" -> Locale.US
            "en-GB" -> Locale.UK
            "hi-IN" -> Locale("hi", "IN")
            else -> Locale("bn", "BD")
        }
        val result = engine.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setupLanguageAndVoice()
            }
        }
    }

    private fun setupLanguageAndVoice() {
        val ttsInstance = tts ?: return
        val bengali = Locale("bn", "BD")
        val result = ttsInstance.setLanguage(bengali)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            val bengaliIndia = Locale("bn", "IN")
            val resIndia = ttsInstance.setLanguage(bengaliIndia)
            if (resIndia == TextToSpeech.LANG_MISSING_DATA || resIndia == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsInstance.language = Locale.getDefault()
            }
        }

        ttsInstance.setPitch(currentPitch)
        ttsInstance.setSpeechRate(currentSpeed)

        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                onSpeakingStarted()
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                onSpeakingFinished()
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                onSpeakingFinished()
            }
        })
    }

    fun setVoicePitch(pitch: Float) {
        currentPitch = pitch
        tts?.setPitch(pitch)
    }

    fun setVoiceSpeed(speed: Float) {
        currentSpeed = speed
        tts?.setSpeechRate(speed)
    }

    fun speak(text: String, utteranceId: String = "arohi_speech_${System.currentTimeMillis()}") {
        if (!isInitialized || text.isBlank()) return

        stop() // Stop previous speech

        val cleanText = text.replace(Regex("[*#_`~]"), "").trim()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
            onSpeakingFinished()
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            // Ignored
        }
    }
}
