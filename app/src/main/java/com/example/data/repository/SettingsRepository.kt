package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.privacy.CryptoManager
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

    // ---------------------------------------------------------------- new personalisation state

    private val _personalityIntensityFlow = MutableStateFlow(getPersonalityIntensity())
    val personalityIntensityFlow: StateFlow<Float> = _personalityIntensityFlow.asStateFlow()

    private val _pronounStyleFlow = MutableStateFlow(getPronounStyle())
    val pronounStyleFlow: StateFlow<String> = _pronounStyleFlow.asStateFlow()

    private val _userNameFlow = MutableStateFlow(getUserName())
    val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    private val _userNicknameFlow = MutableStateFlow(getUserNickname())
    val userNicknameFlow: StateFlow<String> = _userNicknameFlow.asStateFlow()

    private val _themeAccentFlow = MutableStateFlow(getThemeAccent())
    val themeAccentFlow: StateFlow<String> = _themeAccentFlow.asStateFlow()

    private val _avatarOutfitFlow = MutableStateFlow(getAvatarOutfit())
    val avatarOutfitFlow: StateFlow<String> = _avatarOutfitFlow.asStateFlow()

    private val _avatarModeFlow = MutableStateFlow(getAvatarMode())
    val avatarModeFlow: StateFlow<String> = _avatarModeFlow.asStateFlow()

    private val _fontScaleFlow = MutableStateFlow(getFontScale())
    val fontScaleFlow: StateFlow<Float> = _fontScaleFlow.asStateFlow()

    private val _highContrastFlow = MutableStateFlow(isHighContrastEnabled())
    val highContrastFlow: StateFlow<Boolean> = _highContrastFlow.asStateFlow()

    private val _streamingEnabledFlow = MutableStateFlow(isStreamingEnabled())
    val streamingEnabledFlow: StateFlow<Boolean> = _streamingEnabledFlow.asStateFlow()

    private val _offlineFallbackFlow = MutableStateFlow(isOfflineFallbackEnabled())
    val offlineFallbackFlow: StateFlow<Boolean> = _offlineFallbackFlow.asStateFlow()

    private val _encryptionFlow = MutableStateFlow(isLocalEncryptionEnabled())
    val encryptionFlow: StateFlow<Boolean> = _encryptionFlow.asStateFlow()

    private val _cloudSyncFlow = MutableStateFlow(isCloudSyncEnabled())
    val cloudSyncFlow: StateFlow<Boolean> = _cloudSyncFlow.asStateFlow()

    private val _morningCheckInFlow = MutableStateFlow(isMorningCheckInEnabled())
    val morningCheckInFlow: StateFlow<Boolean> = _morningCheckInFlow.asStateFlow()

    private val _eveningCheckInFlow = MutableStateFlow(isEveningCheckInEnabled())
    val eveningCheckInFlow: StateFlow<Boolean> = _eveningCheckInFlow.asStateFlow()

    private val _weatherCityFlow = MutableStateFlow(getWeatherCity())
    val weatherCityFlow: StateFlow<String> = _weatherCityFlow.asStateFlow()

    private val _autoMemoryFlow = MutableStateFlow(isAutoMemoryEnabled())
    val autoMemoryFlow: StateFlow<Boolean> = _autoMemoryFlow.asStateFlow()

    private val _weatherAlertsFlow = MutableStateFlow(isWeatherAlertsEnabled())
    val weatherAlertsFlow: StateFlow<Boolean> = _weatherAlertsFlow.asStateFlow()

    fun getApiKey(): String {
        val customKey = prefs.getString(KEY_API_KEY, "") ?: ""
        if (customKey.isNotBlank()) {
            val decrypted = CryptoManager.decrypt(customKey)
            if (decrypted.isNotBlank()) return decrypted
        }
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
        val trimmed = key.trim()
        val stored = if (isLocalEncryptionEnabled() && trimmed.isNotBlank()) {
            CryptoManager.encrypt(trimmed)
        } else {
            trimmed
        }
        prefs.edit().putString(KEY_API_KEY, stored).apply()
        _apiKeyFlow.value = getApiKey()
    }

    fun getModelName(): String {
        return prefs.getString(KEY_MODEL_NAME, "gemini-3.5-flash") ?: "gemini-3.5-flash"
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

    fun setAssistantIdentity(value: String) {
        prefs.edit().putString(KEY_ASSISTANT_IDENTITY, value.trim()).apply()
        _assistantNameFlow.value = value.trim()
    }

    // ---------------------------------------------------------------- personalisation getters

    /** 0f = fully focused, 1f = fully playful. */
    fun getPersonalityIntensity(): Float = prefs.getFloat(KEY_PERSONALITY, 0.55f)

    fun setPersonalityIntensity(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_PERSONALITY, clamped).apply()
        _personalityIntensityFlow.value = clamped
    }

    /** "tumi" (informal, default) or "apni" (polite). */
    fun getPronounStyle(): String = prefs.getString(KEY_PRONOUN, PRONOUN_TUMI) ?: PRONOUN_TUMI

    fun setPronounStyle(value: String) {
        val normalized = if (value == PRONOUN_APNI) PRONOUN_APNI else PRONOUN_TUMI
        prefs.edit().putString(KEY_PRONOUN, normalized).apply()
        _pronounStyleFlow.value = normalized
    }

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun setUserName(value: String) {
        prefs.edit().putString(KEY_USER_NAME, value.trim()).apply()
        _userNameFlow.value = value.trim()
    }

    fun getUserNickname(): String = prefs.getString(KEY_USER_NICKNAME, "") ?: ""

    fun setUserNickname(value: String) {
        prefs.edit().putString(KEY_USER_NICKNAME, value.trim()).apply()
        _userNicknameFlow.value = value.trim()
    }

    fun getThemeAccent(): String = prefs.getString(KEY_THEME_ACCENT, "cyan") ?: "cyan"

    fun setThemeAccent(value: String) {
        prefs.edit().putString(KEY_THEME_ACCENT, value).apply()
        _themeAccentFlow.value = value
    }

    fun getAvatarOutfit(): String = prefs.getString(KEY_AVATAR_OUTFIT, "cyber") ?: "cyber"

    fun setAvatarOutfit(value: String) {
        prefs.edit().putString(KEY_AVATAR_OUTFIT, value).apply()
        _avatarOutfitFlow.value = value
    }

    /** "2D" (illustrated portrait) or "3D" (animated energy orb). */
    fun getAvatarMode(): String = prefs.getString(KEY_AVATAR_MODE, AVATAR_3D) ?: AVATAR_3D

    fun setAvatarMode(value: String) {
        val normalized = if (value == AVATAR_2D) AVATAR_2D else AVATAR_3D
        prefs.edit().putString(KEY_AVATAR_MODE, normalized).apply()
        _avatarModeFlow.value = normalized
    }

    fun getFontScale(): Float = prefs.getFloat(KEY_FONT_SCALE, 1.0f)

    fun setFontScale(value: Float) {
        val clamped = value.coerceIn(0.85f, 1.4f)
        prefs.edit().putFloat(KEY_FONT_SCALE, clamped).apply()
        _fontScaleFlow.value = clamped
    }

    fun isHighContrastEnabled(): Boolean = prefs.getBoolean(KEY_HIGH_CONTRAST, false)

    fun setHighContrastEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
        _highContrastFlow.value = enabled
    }

    fun isStreamingEnabled(): Boolean = prefs.getBoolean(KEY_STREAMING, true)

    fun setStreamingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STREAMING, enabled).apply()
        _streamingEnabledFlow.value = enabled
    }

    fun isOfflineFallbackEnabled(): Boolean = prefs.getBoolean(KEY_OFFLINE_FALLBACK, true)

    fun setOfflineFallbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE_FALLBACK, enabled).apply()
        _offlineFallbackFlow.value = enabled
    }

    fun isLocalEncryptionEnabled(): Boolean = prefs.getBoolean(KEY_ENCRYPTION, false)

    fun setLocalEncryptionEnabled(enabled: Boolean) {
        val currentKey = prefs.getString(KEY_API_KEY, "") ?: ""
        if (enabled && currentKey.isNotBlank() && !CryptoManager.isEncrypted(currentKey)) {
            prefs.edit().putString(KEY_API_KEY, CryptoManager.encrypt(currentKey)).apply()
        } else if (!enabled && CryptoManager.isEncrypted(currentKey)) {
            prefs.edit().putString(KEY_API_KEY, CryptoManager.decrypt(currentKey)).apply()
        }
        prefs.edit().putBoolean(KEY_ENCRYPTION, enabled).apply()
        _encryptionFlow.value = enabled
    }

    fun isCloudSyncEnabled(): Boolean = prefs.getBoolean(KEY_CLOUD_SYNC, false)

    fun setCloudSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_SYNC, enabled).apply()
        _cloudSyncFlow.value = enabled
    }

    fun isMorningCheckInEnabled(): Boolean = prefs.getBoolean(KEY_MORNING_CHECKIN, false)

    fun setMorningCheckInEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MORNING_CHECKIN, enabled).apply()
        _morningCheckInFlow.value = enabled
    }

    fun isEveningCheckInEnabled(): Boolean = prefs.getBoolean(KEY_EVENING_CHECKIN, false)

    fun setEveningCheckInEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EVENING_CHECKIN, enabled).apply()
        _eveningCheckInFlow.value = enabled
    }

    fun getMorningCheckInTime(): String = prefs.getString(KEY_MORNING_TIME, "08:30") ?: "08:30"

    fun setMorningCheckInTime(value: String) {
        prefs.edit().putString(KEY_MORNING_TIME, value).apply()
    }

    fun getEveningCheckInTime(): String = prefs.getString(KEY_EVENING_TIME, "21:30") ?: "21:30"

    fun setEveningCheckInTime(value: String) {
        prefs.edit().putString(KEY_EVENING_TIME, value).apply()
    }

    fun getWeatherCity(): String = prefs.getString(KEY_WEATHER_CITY, "Dhaka") ?: "Dhaka"

    fun setWeatherCity(value: String) {
        val city = value.trim().ifBlank { "Dhaka" }
        prefs.edit().putString(KEY_WEATHER_CITY, city).apply()
        _weatherCityFlow.value = city
    }

    fun isWeatherAlertsEnabled(): Boolean = prefs.getBoolean(KEY_WEATHER_ALERTS, true)

    fun setWeatherAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEATHER_ALERTS, enabled).apply()
        _weatherAlertsFlow.value = enabled
    }

    fun isAutoMemoryEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_MEMORY, true)

    fun setAutoMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_MEMORY, enabled).apply()
        _autoMemoryFlow.value = enabled
    }

    fun isCloudAiEnabled(): Boolean = prefs.getBoolean(KEY_CLOUD_AI_ENABLED, true)
    fun setCloudAiEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_CLOUD_AI_ENABLED, enabled).apply()

    fun isVisionAiEnabled(): Boolean = prefs.getBoolean(KEY_VISION_AI_ENABLED, true)
    fun setVisionAiEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_VISION_AI_ENABLED, enabled).apply()

    fun isNotificationAiEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_AI_ENABLED, true)
    fun setNotificationAiEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFICATION_AI_ENABLED, enabled).apply()

    companion object {
        const val PRONOUN_TUMI = "tumi"
        const val PRONOUN_APNI = "apni"
        const val AVATAR_2D = "2D"
        const val AVATAR_3D = "3D"

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

        // new keys
        private const val KEY_PERSONALITY = "key_personality_intensity"
        private const val KEY_PRONOUN = "key_pronoun_style"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_NICKNAME = "key_user_nickname"
        private const val KEY_THEME_ACCENT = "key_theme_accent"
        private const val KEY_AVATAR_OUTFIT = "key_avatar_outfit"
        private const val KEY_AVATAR_MODE = "key_avatar_mode"
        private const val KEY_FONT_SCALE = "key_font_scale"
        private const val KEY_HIGH_CONTRAST = "key_high_contrast"
        private const val KEY_STREAMING = "key_streaming_enabled"
        private const val KEY_OFFLINE_FALLBACK = "key_offline_fallback"
        private const val KEY_ENCRYPTION = "key_local_encryption"
        private const val KEY_CLOUD_SYNC = "key_cloud_sync"
        private const val KEY_MORNING_CHECKIN = "key_morning_checkin"
        private const val KEY_EVENING_CHECKIN = "key_evening_checkin"
        private const val KEY_MORNING_TIME = "key_morning_time"
        private const val KEY_EVENING_TIME = "key_evening_time"
        private const val KEY_WEATHER_CITY = "key_weather_city"
        private const val KEY_WEATHER_ALERTS = "key_weather_alerts"
        private const val KEY_AUTO_MEMORY = "key_auto_memory"
    }
}
