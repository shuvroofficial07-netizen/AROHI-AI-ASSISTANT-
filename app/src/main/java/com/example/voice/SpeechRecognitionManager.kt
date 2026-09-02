package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SpeechState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

class SpeechRecognitionManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val _speechState = MutableStateFlow(SpeechState.IDLE)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _speechState.value = SpeechState.LISTENING
        }

        override fun onBeginningOfSpeech() {
            _speechState.value = SpeechState.LISTENING
        }

        override fun onRmsChanged(rmsdB: Float) {
            _rmsLevel.value = (rmsdB.coerceIn(0f, 10f) / 10f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _speechState.value = SpeechState.PROCESSING
        }

        override fun onError(error: Int) {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ডিং ত্রুটি (Audio recording error)"
                SpeechRecognizer.ERROR_CLIENT -> "ক্লায়েন্ট সাইড ত্রুটি (Client error)"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন পারমিশন প্রয়োজন (Microphone permission required)"
                SpeechRecognizer.ERROR_NETWORK -> "নেটওয়ার্ক সমস্যা (Network error)"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "নেটওয়ার্ক টাইমআউট (Network timeout)"
                SpeechRecognizer.ERROR_NO_MATCH -> "কোনো ভয়েস সনাক্ত হয়নি (No speech heard)"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ভয়েস ইঞ্জিন ব্যস্ত (Recognizer busy)"
                SpeechRecognizer.ERROR_SERVER -> "সার্ভার এরর (Server error)"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কথা বলা হয়নি (Speech timeout)"
                else -> "ভয়েস এরর ($error)"
            }
            _speechState.value = SpeechState.ERROR
            _rmsLevel.value = 0f
            onError(errorMsg)
            _speechState.value = SpeechState.IDLE
        }

        override fun onResults(results: Bundle?) {
            _speechState.value = SpeechState.IDLE
            _rmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                onResult(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onPartialResult(matches[0])
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Real check: does the device have *any* speech recognizer installed? (spec §58 SPEECH). */
    fun isRecognitionAvailable(): Boolean = try {
        SpeechRecognizer.isRecognitionAvailable(context)
    } catch (e: Exception) {
        false
    }

    /** Languages Arohi will request recognition in. Availability per language is the
     *  recognizer's responsibility; Arohi requests them explicitly. */
    fun supportedLanguages(): List<String> =
        if (isRecognitionAvailable()) listOf("bn-BD", "en-US", "hi-IN") else emptyList()

    @Volatile
    var currentLanguageCode: String = "bn-BD"
        private set

    /** Switches recognition language (spec §5 language selection). */
    fun setLanguage(code: String) {
        currentLanguageCode = code
    }

    /** True while a recognition session is active — used for barge-in/interruption. */
    val isListening: Boolean get() = _speechState.value == SpeechState.LISTENING

    /** Barge-in / interruption (spec §5): cancels an in-progress recognition. */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // Ignored
        }
        _speechState.value = SpeechState.IDLE
        _rmsLevel.value = 0f
    }

    fun startListening(languageCode: String = currentLanguageCode) {
        currentLanguageCode = languageCode
        if (!hasMicPermission()) {
            onError("মাইক্রোফোন পারমিশন সক্রিয় করুন (Grant Microphone Permission)")
            return
        }

        try {
            stopListening()
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(recognitionListener)
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            _speechState.value = SpeechState.LISTENING
        } catch (e: Exception) {
            _speechState.value = SpeechState.IDLE
            onError("ভয়েস রিকগনিশন শুরু করা যায়নি: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignored
        }
        _speechState.value = SpeechState.IDLE
        _rmsLevel.value = 0f
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignored
        }
    }
}
