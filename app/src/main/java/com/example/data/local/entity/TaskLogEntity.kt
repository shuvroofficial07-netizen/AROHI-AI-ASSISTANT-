package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_logs")
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskName: String,
    val status: String, // EXECUTING, COMPLETED, FAILED, CANCELLED
    val stepsJson: String,
    val resultSummary: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
