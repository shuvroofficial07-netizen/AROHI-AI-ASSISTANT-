package com.example.privacy

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Real on-device encryption for local storage, backed by the Android Keystore (AES-256/GCM).
 *
 * Values are written as `enc1:<base64(iv + ciphertext)>`. If the keystore is unavailable the
 * helper degrades gracefully to plain storage instead of crashing, and [decrypt] returns any
 * legacy (unencrypted) value untouched.
 */
object CryptoManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "arohi_local_storage_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val PREFIX = "enc1:"

    fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return plainText
        if (isEncrypted(plainText)) return plainText
        return try {
            val key = getOrCreateKey() ?: return plainText
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = cipher.iv + cipherText
            PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    fun decrypt(storedValue: String): String {
        if (!isEncrypted(storedValue)) return storedValue
        return try {
            val key = getOrCreateKey() ?: return ""
            val payload = Base64.decode(storedValue.removePrefix(PREFIX), Base64.NO_WRAP)
            if (payload.size <= IV_LENGTH) return ""
            val iv = payload.copyOfRange(0, IV_LENGTH)
            val cipherText = payload.copyOfRange(IV_LENGTH, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private const val IV_LENGTH = 12

    private fun getOrCreateKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            existing?.secretKey ?: createKey()
        } catch (e: Exception) {
            null
        }
    }

    private fun createKey(): SecretKey? {
        return try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            null
        }
    }
}
