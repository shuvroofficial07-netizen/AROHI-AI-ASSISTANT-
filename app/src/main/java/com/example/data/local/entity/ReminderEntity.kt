package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Real, alarm-backed reminder. [triggerAt] is an absolute epoch-millis timestamp that is
 * handed to AlarmManager, so it fires even when the app is closed.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val note: String = "",
    val triggerAt: Long,
    val repeatRule: String = ReminderRepeat.NONE,
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** Supported recurrence rules for reminders. */
object ReminderRepeat {
    const val NONE = "NONE"
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val WEEKDAYS = "WEEKDAYS"

    val all = listOf(NONE, DAILY, WEEKLY, MONTHLY, WEEKDAYS)

    fun bengaliLabel(rule: String): String = when (rule) {
        DAILY -> "প্রতিদিন"
        WEEKLY -> "প্রতি সপ্তাহে"
        MONTHLY -> "প্রতি মাসে"
        WEEKDAYS -> "সপ্তাহের কর্মদিবসে"
        else -> "একবার"
    }
}
