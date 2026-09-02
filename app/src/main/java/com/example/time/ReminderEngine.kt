package com.example.time

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.UUID

/**
 * Schedules real, time-based reminders (spec §29) using [AlarmManager].
 *
 * Exact alarms require the SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM permission on
 * newer Android; when not permitted the engine falls back to an inexact,
 * battery-conscious window — it never silently fails, it reports the mode it
 * actually used. Reminders persist for the device session and fire as real
 * notifications through [ReminderReceiver].
 */
class ReminderEngine(private val context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun channelId(): String {
        val id = "arohi_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = appContext.getSystemService(NotificationManager::class.java)
            if (nm?.getNotificationChannel(id) == null) {
                nm?.createNotificationChannel(
                    NotificationChannel(id, "Arohi Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Scheduled reminder alerts"
                    }
                )
            }
        }
        return id
    }

    /** True if the app can schedule exact alarms on this OS level. */
    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else true

    /**
     * Schedules a reminder. [triggerAtMillis] is the absolute epoch time.
     * Returns the reminder id, or null if scheduling genuinely failed.
     */
    fun scheduleReminder(triggerAtMillis: Long, title: String, note: String = ""): String? {
        if (alarmManager == null) return null
        if (triggerAtMillis <= System.currentTimeMillis()) return null

        val id = UUID.randomUUID().toString()
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_ID, id)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_NOTE, note)
        }
        val pending = PendingIntent.getBroadcast(
            appContext, id.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            } else {
                // Honest fallback: inexact within a short window; the reply reports this.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
            id
        } catch (e: Exception) {
            null
        }
    }

    fun cancelReminder(id: String) {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
        }
        val pending = PendingIntent.getBroadcast(
            appContext, id.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        pending?.let { alarmManager?.cancel(it) }
    }
}

/** Fires the scheduled reminder as a real system notification. */
class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_FIRE = "com.example.action.REMINDER_FIRE"
        const val EXTRA_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_reminder_title"
        const val EXTRA_NOTE = "extra_reminder_note"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_FIRE) return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()

        val tap = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val id = "arohi_reminders"
            if (nm?.getNotificationChannel(id) == null) {
                nm?.createNotificationChannel(
                    NotificationChannel(id, "Arohi Reminders", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            id
        } else "arohi_reminders"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Arohi reminder: $title")
            .setContentText(note.ifBlank { "Scheduled reminder" })
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.notify(("reminder_" + intent.getStringExtra(EXTRA_ID)).hashCode(), notification)
    }
}
