package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("arohi_settings_prefs", Context.MODE_PRIVATE)

    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    private val _modelNameFlow = MutableStateFlow(getModelName())
    val modelNameFlow: StateFlow<String> = _modelNameFlow.asStateFlow()

    private val _proactiveEnabledFlow = MutableStateFlow(isProactiveEnabled())
    val proactiveEnabledFlow: StateFlow<Boolean> = _proactiveEnabledFlow.asStateFlow()

    private val _privateModeFlow = MutableStateFlow(isPrivateMode())
    val privateModeFlow: StateFlow<Boolean> = _privateModeFlow.asStateFlow()

    private val _silenceModeFlow = MutableStateFlow(isSilenceMode())
    val silenceModeFlow: StateFlow<Boolean> = _silenceModeFlow.asStateFlow()

    private val _voicePitchFlow = MutableStateFlow(getVoicePitch())
    val voicePitchFlow: StateFlow<Float> = _voicePitchFlow.asStateFlow()

    private val _voiceSpeedFlow = MutableStateFlow(getVoiceSpeed())
    val voiceSpeedFlow: StateFlow<Float> = _voiceSpeedFlow.asStateFlow()

    private val _assistantNameFlow = MutableStateFlow(getAssistantIdentity())
    val assistantNameFlow: StateFlow<String> = _assistantNameFlow.asStateFlow()

    private val _languageCodeFlow = MutableStateFlow(getLanguageCode())
    val languageCodeFlow: StateFlow<String> = _languageCodeFlow.asStateFlow()

    private val _voiceNameFlow = MutableStateFlow(getVoiceName())
    val voiceNameFlow: StateFlow<String> = _voiceNameFlow.asStateFlow()

    private val _personalityStyleFlow = MutableStateFlow(getPersonalityStyle())
    val personalityStyleFlow: StateFlow<String> = _personalityStyleFlow.asStateFlow()

    private val _wakeWordFlow = MutableStateFlow(getWakeWord())
    val wakeWordFlow: StateFlow<String> = _wakeWordFlow.asStateFlow()

    private val _callerAnnouncementFlow = MutableStateFlow(isCallerAnnouncementEnabled())
    val callerAnnouncementFlow: StateFlow<Boolean> = _callerAnnouncementFlow.asStateFlow()

    private val _notificationAnnouncementFlow = MutableStateFlow(isNotificationAnnouncementEnabled())
    val notificationAnnouncementFlow: StateFlow<Boolean> = _notificationAnnouncementFlow.asStateFlow()

    private val _cloudAiFlow = MutableStateFlow(isCloudAiEnabled())
    val cloudAiFlow: StateFlow<Boolean> = _cloudAiFlow.asStateFlow()

    private val _visionAiFlow = MutableStateFlow(isVisionAiEnabled())
    val visionAiFlow: StateFlow<Boolean> = _visionAiFlow.asStateFlow()

    private val _notificationAiFlow = MutableStateFlow(isNotificationAiEnabled())
    val notificationAiFlow: StateFlow<Boolean> = _notificationAiFlow.asStateFlow()

    private val _messageAiFlow = MutableStateFlow(isMessageAiAnalysisEnabled())
    val messageAiFlow: StateFlow<Boolean> = _messageAiFlow.asStateFlow()

    private val _firstLaunchFlow = MutableStateFlow(isFirstLaunch())
    val firstLaunchFlow: StateFlow<Boolean> = _firstLaunchFlow.asStateFlow()

    fun getApiKey(): String {
        val customKey = prefs.getString(KEY_API_KEY, "") ?: ""
        if (customKey.isNotBlank()) return customKey
        // Fall back to BuildConfig.GEMINI_API_KEY if available and not a placeholder
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && !buildKey.contains("MY_GEMINI_API_KEY") && !buildKey.contains("YOUR_")) {
                buildKey
            } else {
                customKey
            }
        } catch (e: Exception) {
            customKey
        }
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
        _apiKeyFlow.value = getApiKey()
    }

    fun getModelName(): String {
        return prefs.getString(KEY_MODEL_NAME, "gemini-3.5-flash") ?: "gemini-3.5-flash"
    }

    fun setModelName(name: String) {
        prefs.edit().putString(KEY_MODEL_NAME, name).apply()
        _modelNameFlow.value = name
    }

    fun getGeminiTimeoutSeconds(): Int {
        return prefs.getInt(KEY_GEMINI_TIMEOUT_SEC, 60)
    }

    fun setGeminiTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_GEMINI_TIMEOUT_SEC, seconds.coerceIn(10, 300)).apply()
    }

    fun getGeminiRetryCount(): Int {
        return prefs.getInt(KEY_GEMINI_RETRY_COUNT, 0)
    }

    fun setGeminiRetryCount(count: Int) {
        prefs.edit().putInt(KEY_GEMINI_RETRY_COUNT, count.coerceIn(0, 5)).apply()
    }

    fun isProactiveEnabled(): Boolean {
        return prefs.getBoolean(KEY_PROACTIVE_ENABLED, true)
    }

    fun setProactiveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROACTIVE_ENABLED, enabled).apply()
        _proactiveEnabledFlow.value = enabled
    }

    fun isPrivateMode(): Boolean {
        return prefs.getBoolean(KEY_PRIVATE_MODE, false)
    }

    fun setPrivateMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVATE_MODE, enabled).apply()
        _privateModeFlow.value = enabled
    }

    fun isSilenceMode(): Boolean {
        return prefs.getBoolean(KEY_SILENCE_MODE, false)
    }

    fun setSilenceMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SILENCE_MODE, enabled).apply()
        _silenceModeFlow.value = enabled
    }

    fun getVoicePitch(): Float {
        return prefs.getFloat(KEY_VOICE_PITCH, 1.15f) // Slightly higher feminine pitch
    }

    fun setVoicePitch(pitch: Float) {
        prefs.edit().putFloat(KEY_VOICE_PITCH, pitch).apply()
        _voicePitchFlow.value = pitch
    }

    fun getVoiceSpeed(): Float {
        return prefs.getFloat(KEY_VOICE_SPEED, 1.0f)
    }

    fun setVoiceSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_VOICE_SPEED, speed).apply()
        _voiceSpeedFlow.value = speed
    }

    fun getAssistantIdentity(): String {
        return prefs.getString(KEY_ASSISTANT_IDENTITY, "Arohi AI Assistant by Shù Vrô") ?: "Arohi AI Assistant by Shù Vrô"
    }

    /** Speech-recognition language (real language codes, e.g. bn-BD, en-US). */
    fun getLanguageCode(): String {
        return prefs.getString(KEY_LANGUAGE_CODE, "bn-BD") ?: "bn-BD"
    }

    fun setLanguageCode(code: String) {
        prefs.edit().putString(KEY_LANGUAGE_CODE, code).apply()
        _languageCodeFlow.value = code
    }

    /** Selected TTS engine voice name; blank means the engine default. */
    fun getVoiceName(): String {
        return prefs.getString(KEY_VOICE_NAME, "") ?: ""
    }

    fun setVoiceName(name: String) {
        prefs.edit().putString(KEY_VOICE_NAME, name).apply()
        _voiceNameFlow.value = name
    }

    fun getPersonalityStyle(): String {
        return prefs.getString(KEY_PERSONALITY_STYLE, DEFAULT_PERSONALITY) ?: DEFAULT_PERSONALITY
    }

    fun setPersonalityStyle(style: String) {
        prefs.edit().putString(KEY_PERSONALITY_STYLE, style).apply()
        _personalityStyleFlow.value = style
    }

    fun getWakeWord(): String {
        return prefs.getString(KEY_WAKE_WORD, "আরোহী") ?: "আরোহী"
    }

    fun setWakeWord(word: String) {
        prefs.edit().putString(KEY_WAKE_WORD, word.trim()).apply()
        _wakeWordFlow.value = getWakeWord()
    }

    fun isCallerAnnouncementEnabled(): Boolean {
        return prefs.getBoolean(KEY_CALLER_ANNOUNCEMENT, true)
    }

    fun setCallerAnnouncementEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CALLER_ANNOUNCEMENT, enabled).apply()
        _callerAnnouncementFlow.value = enabled
    }

    fun isNotificationAnnouncementEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_ANNOUNCEMENT, true)
    }

    fun setNotificationAnnouncementEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ANNOUNCEMENT, enabled).apply()
        _notificationAnnouncementFlow.value = enabled
    }

    fun isCloudAiEnabled(): Boolean = prefs.getBoolean(KEY_CLOUD_AI_ENABLED, true)
    fun setCloudAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_AI_ENABLED, enabled).apply()
        _cloudAiFlow.value = enabled
    }

    fun isVisionAiEnabled(): Boolean = prefs.getBoolean(KEY_VISION_AI_ENABLED, true)
    fun setVisionAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VISION_AI_ENABLED, enabled).apply()
        _visionAiFlow.value = enabled
    }

    fun isNotificationAiEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_AI_ENABLED, true)
    fun setNotificationAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_AI_ENABLED, enabled).apply()
        _notificationAiFlow.value = enabled
    }

    fun isMessageAiAnalysisEnabled(): Boolean = prefs.getBoolean(KEY_MESSAGE_AI_ENABLED, true)
    fun setMessageAiAnalysisEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MESSAGE_AI_ENABLED, enabled).apply()
        _messageAiFlow.value = enabled
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun markSetupComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        _firstLaunchFlow.value = false
    }

    companion object {
        private const val KEY_API_KEY = "key_gemini_api_key"
        private const val KEY_MODEL_NAME = "key_model_name"
        private const val KEY_GEMINI_TIMEOUT_SEC = "key_gemini_timeout_sec"
        private const val KEY_GEMINI_RETRY_COUNT = "key_gemini_retry_count"
        private const val KEY_PROACTIVE_ENABLED = "key_proactive_enabled"
        private const val KEY_PRIVATE_MODE = "key_private_mode"
        private const val KEY_SILENCE_MODE = "key_silence_mode"
        private const val KEY_VOICE_PITCH = "key_voice_pitch"
        private const val KEY_VOICE_SPEED = "key_voice_speed"
        private const val KEY_ASSISTANT_IDENTITY = "key_assistant_identity"
        private const val KEY_LANGUAGE_CODE = "key_language_code"
        private const val KEY_VOICE_NAME = "key_voice_name"
        private const val KEY_PERSONALITY_STYLE = "key_personality_style"
        private const val KEY_WAKE_WORD = "key_wake_word"
        private const val KEY_CALLER_ANNOUNCEMENT = "key_caller_announcement"
        private const val KEY_NOTIFICATION_ANNOUNCEMENT = "key_notification_announcement"
        private const val KEY_CLOUD_AI_ENABLED = "key_cloud_ai_enabled"
        private const val KEY_VISION_AI_ENABLED = "key_vision_ai_enabled"
        private const val KEY_NOTIFICATION_AI_ENABLED = "key_notification_ai_enabled"
        private const val KEY_MESSAGE_AI_ENABLED = "key_message_ai_enabled"
        private const val KEY_FIRST_LAUNCH = "key_first_launch_done"

        const val DEFAULT_PERSONALITY = "Warm Bengali Companion"

        val PERSONALITY_STYLES = listOf(
            DEFAULT_PERSONALITY,
            "Friendly",
            "Playful",
            "Formal",
            "Concise",
            "Detailed",
            "Bengali-first",
            "English-first",
            "Auto Detect"
        )

        val LANGUAGE_OPTIONS = listOf(
            "bn-BD" to "বাংলা (Bengali)",
            "bn-IN" to "বাংলা (ভারত)",
            "en-US" to "English (US)",
            "en-IN" to "English (India)",
            "hi-IN" to "हिन्दी (Hindi)",
            "ur-IN" to "اردو (Urdu)",
            "ta-IN" to "தமிழ் (Tamil)",
            "te-IN" to "తెలుగు (Telugu)",
            "kn-IN" to "ಕನ್ನಡ (Kannada)",
            "ml-IN" to "മലയാളം (Malayalam)",
            "mr-IN" to "मराठी (Marathi)",
            "gu-IN" to "ગુજરાતી (Gujarati)",
            "pa-IN" to "ਪੰਜਾਬੀ (Punjabi)"
        )
    }
}
