package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ArohiApplication
import com.example.data.local.entity.ReminderRepeat
import com.example.device.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager when a real reminder is due. Shows the notification and, for repeating
 * reminders, immediately schedules the next occurrence.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return
        val reminderId = intent.getLongExtra(EXTRA_ID, -1L)
        if (reminderId <= 0) return

        val app = context.applicationContext as? ArohiApplication ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val reminder = app.reminderRepository.getById(reminderId) ?: return@launch
                if (reminder.isCompleted || !reminder.isEnabled) return@launch

                NotificationHelper.show(
                    context = context,
                    channelId = NotificationHelper.CHANNEL_REMINDERS,
                    notificationId = reminderId.toInt(),
                    title = "⏰ ${reminder.title}",
                    text = if (reminder.note.isBlank()) {
                        "এখন সময় হয়ে গেছে — ${ReminderScheduler.formatBengaliDateTime(reminder.triggerAt)}"
                    } else {
                        reminder.note
                    },
                    bigText = reminder.note.ifBlank { "${reminder.title} — এখনই করার সময়।" },
                    speakable = true
                )

                if (reminder.repeatRule != ReminderRepeat.NONE) {
                    val next = ReminderScheduler.nextTrigger(System.currentTimeMillis(), reminder.repeatRule)
                    app.reminderRepository.updateReminder(reminder.copy(triggerAt = next))
                } else {
                    app.reminderRepository.markCompleted(reminder.id, true)
                }
            } catch (e: Exception) {
                // Nothing to do — the alarm simply does not repeat.
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.example.arohi.action.REMINDER"
        const val EXTRA_ID = "extra_reminder_id"
    }
}
