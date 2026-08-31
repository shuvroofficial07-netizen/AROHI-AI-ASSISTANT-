package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // USER, AROHI, SYSTEM, TOOL
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: String = "IDLE",
    val isVoice: Boolean = false,
    val toolCallJson: String? = null,
    val toolResultJson: String? = null
)
