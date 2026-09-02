package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.core.SecureKeyStore
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Notification voice-announcement policy (requirement: configurable, privacy aware). */
enum class NotificationAnnouncePolicy {
    OFF,
    IMPORTANT_ONLY,
    SELECTED_APPS,
    ALL
}

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("arohi_settings_prefs", Context.MODE_PRIVATE)

    private val secureStore = SecureKeyStore(prefs)

    /** True when the API key is protected by an Android Keystore AES key on this device. */
    val isApiKeyEncrypted: Boolean = secureStore.isEncrypted

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

    private val _timeoutSecondsFlow = MutableStateFlow(getRequestTimeoutSeconds())
    val timeoutSecondsFlow: StateFlow<Int> = _timeoutSecondsFlow.asStateFlow()

    private val _maxRetriesFlow = MutableStateFlow(getMaxRetries())
    val maxRetriesFlow: StateFlow<Int> = _maxRetriesFlow.asStateFlow()

    private val _announcePolicyFlow = MutableStateFlow(getNotificationAnnouncePolicy())
    val announcePolicyFlow: StateFlow<NotificationAnnouncePolicy> = _announcePolicyFlow.asStateFlow()

    init {
        applyNetworkConfig()
    }

    fun getNotificationAnnouncePolicy(): NotificationAnnouncePolicy {
        val stored = prefs.getString(KEY_ANNOUNCE_POLICY, NotificationAnnouncePolicy.IMPORTANT_ONLY.name)
        return runCatching { NotificationAnnouncePolicy.valueOf(stored!!) }
            .getOrDefault(NotificationAnnouncePolicy.IMPORTANT_ONLY)
    }

    fun setNotificationAnnouncePolicy(policy: NotificationAnnouncePolicy) {
        prefs.edit().putString(KEY_ANNOUNCE_POLICY, policy.name).apply()
        _announcePolicyFlow.value = policy
    }

    /** App labels the user explicitly allowed for SELECTED_APPS mode. */
    fun getAnnouncedPackages(): Set<String> =
        prefs.getStringSet(KEY_ANNOUNCED_APPS, emptySet()) ?: emptySet()

    fun setAnnouncedPackages(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_ANNOUNCED_APPS, apps).apply()
    }

    fun getApiKey(): String {
        val customKey = secureStore.get(KEY_API_KEY)
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
        secureStore.put(KEY_API_KEY, key)
        _apiKeyFlow.value = getApiKey()
    }

    /** Removes the stored key completely (encrypted blob included). */
    fun clearApiKey() {
        secureStore.remove(KEY_API_KEY)
        _apiKeyFlow.value = getApiKey()
    }

    /** True when a usable key exists (user-provided or supplied at build time via .env). */
    fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    fun isUserProvidedKey(): Boolean = secureStore.get(KEY_API_KEY).isNotBlank()

    // ---- Gemini network configuration (real values used by GeminiClient) ----

    fun getRequestTimeoutSeconds(): Int =
        prefs.getInt(KEY_TIMEOUT_SECONDS, GeminiClient.DEFAULT_TIMEOUT_SECONDS)

    fun setRequestTimeoutSeconds(seconds: Int) {
        val clean = seconds.coerceIn(5, 180)
        prefs.edit().putInt(KEY_TIMEOUT_SECONDS, clean).apply()
        _timeoutSecondsFlow.value = clean
        applyNetworkConfig()
    }

    fun getMaxRetries(): Int = prefs.getInt(KEY_MAX_RETRIES, GeminiClient.DEFAULT_MAX_RETRIES)

    fun setMaxRetries(retries: Int) {
        val clean = retries.coerceIn(0, 3)
        prefs.edit().putInt(KEY_MAX_RETRIES, clean).apply()
        _maxRetriesFlow.value = clean
        applyNetworkConfig()
    }

    private fun applyNetworkConfig() {
        GeminiClient.configure(getRequestTimeoutSeconds(), getMaxRetries())
    }

    fun getModelName(): String {
        val stored = prefs.getString(KEY_MODEL_NAME, null)
        // Older builds defaulted to a model id that the API does not serve; migrate silently.
        if (stored.isNullOrBlank() || stored !in GeminiClient.SELECTABLE_MODELS) {
            return GeminiClient.DEFAULT_MODEL
        }
        return stored
    }

    fun setModelName(name: String) {
        prefs.edit().putString(KEY_MODEL_NAME, name).apply()
        _modelNameFlow.value = name
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

    fun isCloudAiEnabled(): Boolean = prefs.getBoolean(KEY_CLOUD_AI_ENABLED, true)
    fun setCloudAiEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_CLOUD_AI_ENABLED, enabled).apply()

    fun isVisionAiEnabled(): Boolean = prefs.getBoolean(KEY_VISION_AI_ENABLED, true)
    fun setVisionAiEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_VISION_AI_ENABLED, enabled).apply()

    fun isNotificationAiEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_AI_ENABLED, true)
    fun setNotificationAiEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFICATION_AI_ENABLED, enabled).apply()

    companion object {
        private const val KEY_API_KEY = "key_gemini_api_key"
        private const val KEY_MODEL_NAME = "key_model_name"
        private const val KEY_PROACTIVE_ENABLED = "key_proactive_enabled"
        private const val KEY_PRIVATE_MODE = "key_private_mode"
        private const val KEY_SILENCE_MODE = "key_silence_mode"
        private const val KEY_VOICE_PITCH = "key_voice_pitch"
        private const val KEY_VOICE_SPEED = "key_voice_speed"
        private const val KEY_ASSISTANT_IDENTITY = "key_assistant_identity"
        private const val KEY_CLOUD_AI_ENABLED = "key_cloud_ai_enabled"
        private const val KEY_VISION_AI_ENABLED = "key_vision_ai_enabled"
        private const val KEY_NOTIFICATION_AI_ENABLED = "key_notification_ai_enabled"
        private const val KEY_TIMEOUT_SECONDS = "key_gemini_timeout_seconds"
        private const val KEY_MAX_RETRIES = "key_gemini_max_retries"
        private const val KEY_ANNOUNCE_POLICY = "key_notification_announce_policy"
        private const val KEY_ANNOUNCED_APPS = "key_notification_announced_apps"
    }
}
