package com.example.data.repository

import com.example.data.local.dao.ReminderDao
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.ReminderRepeat
import com.example.device.ReminderScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Every write in this repository also updates the real OS alarm, so the Room row and the
 * AlarmManager entry can never drift apart.
 */
class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val scheduler: ReminderScheduler
) {
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    suspend fun getActiveReminders(): List<ReminderEntity> = reminderDao.getActiveReminders()

    suspend fun getSchedulableReminders(): List<ReminderEntity> = reminderDao.getSchedulableReminders()

    suspend fun getById(id: Long): ReminderEntity? = reminderDao.getById(id)

    suspend fun search(query: String): List<ReminderEntity> = reminderDao.searchByTitle(query)

    suspend fun addReminder(
        title: String,
        triggerAt: Long,
        note: String = "",
        repeatRule: String = ReminderRepeat.NONE
    ): Long {
        val id = reminderDao.insertReminder(
            ReminderEntity(
                title = title.trim(),
                note = note.trim(),
                triggerAt = triggerAt,
                repeatRule = repeatRule
            )
        )
        reminderDao.getById(id)?.let { scheduler.schedule(it) }
        return id
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.updateReminder(reminder)
        scheduler.cancel(reminder.id)
        if (reminder.isEnabled && !reminder.isCompleted) {
            scheduler.schedule(reminder)
        }
    }

    suspend fun markCompleted(id: Long, completed: Boolean = true) {
        reminderDao.getById(id)?.let { reminder ->
            reminderDao.updateReminder(reminder.copy(isCompleted = completed))
            if (completed) scheduler.cancel(id) else scheduler.schedule(reminder)
        }
    }

    suspend fun deleteReminder(id: Long) {
        scheduler.cancel(id)
        reminderDao.deleteById(id)
    }

    suspend fun deleteCompleted() {
        reminderDao.deleteCompleted()
    }

    suspend fun clearAll() {
        reminderDao.getActiveReminders().forEach { scheduler.cancel(it.id) }
        reminderDao.clearAll()
    }

    /** Re-arms every alarm (used after a reboot or after the user re-enables the assistant). */
    suspend fun rescheduleAll() {
        scheduler.rescheduleAll(reminderDao.getSchedulableReminders())
    }
}
