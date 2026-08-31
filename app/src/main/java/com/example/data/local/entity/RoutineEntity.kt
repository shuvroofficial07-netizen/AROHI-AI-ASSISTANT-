package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String,
    val triggerPhrase: String,
    val actionsJson: String, // JSON array of executable commands/tools
    val isEnabled: Boolean = true,
    val iconName: String = "routine"
)
