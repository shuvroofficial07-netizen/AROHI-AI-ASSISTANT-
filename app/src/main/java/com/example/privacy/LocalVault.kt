package com.example.privacy

import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.repository.SettingsRepository

/**
 * Transparent at-rest encryption for the user's own content (chat messages and memories).
 *
 * The encryption key never leaves the device: it is generated inside the Android Keystore
 * (see [CryptoManager]) and only ever used here. Rows written while the Privacy Center toggle is
 * ON carry the `enc1:` prefix; everything else is stored as plain text, so [decrypt] is always
 * safe to call on any stored value and old data keeps working after the toggle is flipped.
 */
class LocalVault(private val settingsRepository: SettingsRepository) {

    fun isEnabled(): Boolean = settingsRepository.isLocalEncryptionEnabled()

    fun encrypt(value: String): String {
        if (!isEnabled() || value.isBlank() || CryptoManager.isEncrypted(value)) return value
        return CryptoManager.encrypt(value)
    }

    fun decrypt(value: String): String = CryptoManager.decrypt(value)

    fun encryptMessage(message: MessageEntity): MessageEntity = message.copy(
        content = encrypt(message.content)
    )

    fun decryptMessage(message: MessageEntity): MessageEntity = message.copy(
        content = decrypt(message.content)
    )

    fun encryptMemory(memory: MemoryEntity): MemoryEntity = memory.copy(
        value = encrypt(memory.value)
    )

    fun decryptMemory(memory: MemoryEntity): MemoryEntity = memory.copy(
        value = decrypt(memory.value)
    )

    /**
     * Rewrites every stored message/memory to match the current toggle, so flipping "local
     * encryption" actually protects the data that already exists instead of only new writes.
     */
    suspend fun reconvertStoredData(messageDao: MessageDao, memoryDao: MemoryDao, encryptNow: Boolean): Int {
        var converted = 0

        for (message in messageDao.getAllMessagesOnce()) {
            val want = if (encryptNow) encryptNowValue(message.content) else CryptoManager.decrypt(message.content)
            if (want != message.content) {
                messageDao.updateMessage(message.copy(content = want))
                converted++
            }
        }

        for (memory in memoryDao.getAllMemoriesOnce()) {
            val want = if (encryptNow) encryptNowValue(memory.value) else CryptoManager.decrypt(memory.value)
            if (want != memory.value) {
                memoryDao.updateMemory(memory.copy(value = want))
                converted++
            }
        }

        return converted
    }

    private fun encryptNowValue(value: String): String =
        if (value.isBlank() || CryptoManager.isEncrypted(value)) value else CryptoManager.encrypt(value)
}
