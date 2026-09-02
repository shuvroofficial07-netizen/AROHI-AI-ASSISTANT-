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
    private val onSpeakingFinished: () -> Unit = {},
    private val onInitFailed: () -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initFailed = false

    /** True once the TTS engine REALLY finished initializing successfully. */
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPitch = 1.15f
    private var currentSpeed = 1.0f

    // Text requested before the engine finished initializing is queued and
    // spoken as soon as init succeeds — so the very first "Test Voice" tap
    // always produces real speech instead of silently doing nothing.
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                initFailed = false
                _isReady.value = true
                setupLanguageAndVoice()
                pendingText?.let { queued ->
                    pendingText = null
                    speakNow(queued)
                }
            } else {
                initFailed = true
                isInitialized = false
                _isReady.value = false
                pendingText = null
                onInitFailed()
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

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                onSpeakingFinished()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
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
        if (text.isBlank()) return
        if (isInitialized) {
            speakNow(text, utteranceId)
        } else if (!initFailed) {
            // Engine still initializing — queue and speak when actually ready.
            pendingText = text
        }
        // If initialization FAILED we do not pretend to speak; the caller can
        // observe [isReady] and report the honest state.
    }

    private fun speakNow(text: String, utteranceId: String = "arohi_speech_${System.currentTimeMillis()}") {
        val engine = tts ?: return

        stop() // Stop previous speech

        val cleanText = text.replace(Regex("[*#_`~]"), "").trim()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
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
