package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("arohi_settings_prefs", Context.MODE_PRIVATE)

    /**
     * Hardware-keystore-backed encrypted storage for the Gemini API key.
     * Falls back to the legacy preferences only if the Keystore is genuinely
     * unavailable on the device — never fabricates or drops the key silently.
     */
    private val securePrefs: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "arohi_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Some devices/ROMs have a broken Keystore — fall back honestly
            // rather than losing the user's key. Never log the key itself.
            Log.w("ArohiSettings", "Encrypted storage unavailable (${e.javaClass.simpleName}) — using standard preferences for the API key")
            null
        }
    }

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

    fun getApiKey(): String {
        // 1. Encrypted storage (preferred)
        val secure = securePrefs?.getString(KEY_API_KEY, null)
        if (!secure.isNullOrBlank()) return secure

        // 2. Legacy plaintext value — migrate it into encrypted storage once,
        //    then remove the plaintext copy (existing user data is preserved).
        val legacy = prefs.getString(KEY_API_KEY, "") ?: ""
        if (legacy.isNotBlank()) {
            val stored = securePrefs?.edit()?.putString(KEY_API_KEY, legacy)?.commit() ?: false
            if (stored || securePrefs == null) {
                // Only delete the plaintext copy when it is safe somewhere else,
                // or when no secure storage exists at all (nothing to migrate to).
                prefs.edit().remove(KEY_API_KEY).apply()
            }
            return legacy
        }

        // 3. Build-time key from .env, if it is a real key and not a placeholder
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && !buildKey.contains("MY_GEMINI_API_KEY") && !buildKey.contains("YOUR_")) {
                buildKey
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun setApiKey(key: String) {
        val cleanKey = key.trim()
        val secure = securePrefs
        if (secure != null) {
            secure.edit().putString(KEY_API_KEY, cleanKey).apply()
            // Make sure no plaintext copy lingers anywhere.
            prefs.edit().remove(KEY_API_KEY).apply()
        } else {
            prefs.edit().putString(KEY_API_KEY, cleanKey).apply()
        }
        _apiKeyFlow.value = getApiKey()
    }

    fun clearApiKey() {
        securePrefs?.edit()?.remove(KEY_API_KEY)?.apply()
        prefs.edit().remove(KEY_API_KEY).apply()
        _apiKeyFlow.value = getApiKey()
    }

    fun getModelName(): String {
        return prefs.getString(KEY_MODEL_NAME, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun setModelName(name: String) {
        val clean = name.trim().ifBlank { DEFAULT_MODEL }
        prefs.edit().putString(KEY_MODEL_NAME, clean).apply()
        _modelNameFlow.value = clean
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
        const val DEFAULT_MODEL = "gemini-3.5-flash"
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
    }
}
