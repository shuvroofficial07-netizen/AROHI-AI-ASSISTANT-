package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A real note stored in Room, creatable by voice or from the productivity screen. */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
