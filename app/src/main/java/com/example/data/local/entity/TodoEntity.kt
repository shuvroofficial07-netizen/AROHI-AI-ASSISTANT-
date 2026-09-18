package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A real to-do item persisted in Room and editable from the UI or by voice. */
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val details: String = "",
    val isDone: Boolean = false,
    val priority: Int = 1,
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
