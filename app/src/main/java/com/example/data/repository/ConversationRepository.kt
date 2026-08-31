package com.example.data.repository

import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

class ConversationRepository(private val messageDao: MessageDao) {
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()

    suspend fun getRecentMessages(limit: Int = 20): List<MessageEntity> {
        return messageDao.getRecentMessages(limit)
    }

    suspend fun addMessage(
        role: String,
        content: String,
        emotion: String = "IDLE",
        isVoice: Boolean = false,
        toolCallJson: String? = null,
        toolResultJson: String? = null
    ): Long {
        return messageDao.insertMessage(
            MessageEntity(
                role = role,
                content = content,
                emotion = emotion,
                isVoice = isVoice,
                toolCallJson = toolCallJson,
                toolResultJson = toolResultJson
            )
        )
    }

    suspend fun clearHistory() {
        messageDao.clearAll()
    }
}
