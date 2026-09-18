package com.example.data.repository

import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.MessageEntity
import com.example.privacy.LocalVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversationRepository(
    private val messageDao: MessageDao,
    private val vault: LocalVault
) {
    /** Decrypted for the UI — rows on disk may be encrypted when the user enabled it. */
    val allMessages: Flow<List<MessageEntity>> =
        messageDao.getAllMessages().map { messages -> messages.map { vault.decryptMessage(it) } }

    suspend fun getRecentMessages(limit: Int = 20): List<MessageEntity> {
        return messageDao.getRecentMessages(limit).map { vault.decryptMessage(it) }
    }

    suspend fun addMessage(
        role: String,
        content: String,
        emotion: String = "IDLE",
        isVoice: Boolean = false,
        toolCallJson: String? = null,
        toolResultJson: String? = null
    ): Long {
        val message = MessageEntity(
            role = role,
            content = content,
            emotion = emotion,
            isVoice = isVoice,
            toolCallJson = toolCallJson,
            toolResultJson = toolResultJson
        )
        return messageDao.insertMessage(vault.encryptMessage(message))
    }

    suspend fun clearHistory() {
        messageDao.clearAll()
    }
}
