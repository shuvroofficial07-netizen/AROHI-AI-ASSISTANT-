package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String, // PROFILE, PREFERENCES, APP_ALIASES, IMPORTANT_FACTS, CUSTOM_COMMANDS
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)
