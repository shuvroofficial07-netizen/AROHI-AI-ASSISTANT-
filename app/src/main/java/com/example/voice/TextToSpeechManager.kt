package com.example.voice

import android.content.Context
import android.content.Intent
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

    private val _availableVoices = MutableStateFlow<List<String>>(emptyList())
    val availableVoices: StateFlow<List<String>> = _availableVoices.asStateFlow()

    private var currentPitch = 1.15f
    private var currentSpeed = 1.0f
    private var selectedVoiceName: String = ""

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setupLanguageAndVoice()
                refreshVoiceList()
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

        applySelectedVoice()
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

    /** Real list of voices installed on this device for the TTS engine. */
    fun refreshVoiceList() {
        val ttsInstance = tts ?: return
        try {
            val voices = ttsInstance.voices ?: emptySet()
            _availableVoices.value = voices
                .filter { !it.isNetworkConnectionRequired }
                .map { voice -> voice.name }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            _availableVoices.value = emptyList()
        }
    }

    fun setVoiceByName(name: String) {
        selectedVoiceName = name
        applySelectedVoice()
    }

    private fun applySelectedVoice() {
        val ttsInstance = tts ?: return
        if (selectedVoiceName.isBlank()) return
        try {
            val voice = ttsInstance.voices?.firstOrNull { it.name == selectedVoiceName }
            if (voice != null) {
                ttsInstance.voice = voice
            }
        } catch (e: Exception) {
            // Keep the engine default voice
        }
    }

    companion object {
        /** Real check: does this device have any TTS engine that can speak? */
        fun isTtsEngineAvailable(context: Context): Boolean {
            return try {
                val intent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
                val resolvers = context.packageManager.queryIntentActivities(intent, 0)
                resolvers.isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
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
