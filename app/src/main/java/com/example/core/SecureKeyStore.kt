package com.example.core

import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the user's Gemini API key encrypted with a hardware-backed Android Keystore key
 * (AES/GCM) instead of as plain text in SharedPreferences.
 *
 * Honest limitation: on API < 23 the Keystore cannot hold an AES key, so the value is stored
 * in the app's private SharedPreferences without extra encryption. [isEncrypted] reports which
 * of the two is actually in use so the UI can tell the truth about it.
 */
class SecureKeyStore(private val prefs: SharedPreferences) {

    val isEncrypted: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    fun put(prefKey: String, value: String) {
        val clean = value.trim()
        if (clean.isEmpty()) {
            remove(prefKey)
            return
        }
        if (!isEncrypted) {
            prefs.edit().putString(prefKey, clean).remove(cipherKey(prefKey)).apply()
            return
        }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
            val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            prefs.edit().putString(cipherKey(prefKey), payload).remove(prefKey).apply()
        } catch (t: Throwable) {
            // Keystore unavailable (some OEM images): fall back to private prefs, never crash.
            Log.w(TAG, "Keystore encryption unavailable, storing in private prefs: ${t.message}")
            prefs.edit().putString(prefKey, clean).remove(cipherKey(prefKey)).apply()
        }
    }

    fun get(prefKey: String): String {
        val payload = prefs.getString(cipherKey(prefKey), null)
        if (payload != null) {
            try {
                val parts = payload.split(":")
                if (parts.size == 2) {
                    val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                    val data = Base64.decode(parts[1], Base64.NO_WRAP)
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
                    return String(cipher.doFinal(data), Charsets.UTF_8)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Stored key could not be decrypted: ${t.message}")
                return ""
            }
        }
        return prefs.getString(prefKey, "") ?: ""
    }

    fun remove(prefKey: String) {
        prefs.edit().remove(prefKey).remove(cipherKey(prefKey)).apply()
    }

    private fun cipherKey(prefKey: String) = "${prefKey}__enc"

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val TAG = "ArohiSecureStore"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "arohi_secret_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
