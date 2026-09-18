package com.example.device

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.ReminderRepeat
import com.example.service.CheckInType
import com.example.service.ProactiveCheckInReceiver
import com.example.service.ReminderAlarmReceiver
import java.util.Calendar

/**
 * Schedules real alarms through AlarmManager. Everything here maps to a genuine OS alarm,
 * so reminders fire even when AROHI is not running.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun schedule(reminder: ReminderEntity): Boolean {
        if (!reminder.isEnabled || reminder.isCompleted) return false
        if (reminder.triggerAt <= System.currentTimeMillis()) return false
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_ID, reminder.id)
        }
        setAlarm(reminder.triggerAt, reminder.id.toInt(), intent)
        return true
    }

    fun cancel(reminderId: Long) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_ID, reminderId)
        }
        pendingIntent(reminderId.toInt(), intent)?.let { alarmManager?.cancel(it) }
    }

    fun rescheduleAll(reminders: List<ReminderEntity>) {
        for (reminder in reminders) {
            if (reminder.isEnabled && !reminder.isCompleted && reminder.triggerAt > System.currentTimeMillis()) {
                schedule(reminder)
            }
        }
    }

    /** Schedules (or cancels) the daily morning / evening check-in alarms. */
    fun scheduleCheckIns(
        morningEnabled: Boolean,
        morningTime: String,
        eveningEnabled: Boolean,
        eveningTime: String
    ) {
        if (morningEnabled) {
            scheduleCheckIn(CheckInType.MORNING, morningTime)
        } else {
            cancelCheckIn(CheckInType.MORNING)
        }
        if (eveningEnabled) {
            scheduleCheckIn(CheckInType.EVENING, eveningTime)
        } else {
            cancelCheckIn(CheckInType.EVENING)
        }
    }

    fun scheduleCheckIn(type: CheckInType, time: String) {
        val (hour, minute) = parseTimeOfDay(time)
        val triggerAt = nextOccurrence(hour, minute)
        val intent = Intent(context, ProactiveCheckInReceiver::class.java).apply {
            action = ProactiveCheckInReceiver.ACTION_CHECK_IN
            putExtra(ProactiveCheckInReceiver.EXTRA_TYPE, type.name)
        }
        setAlarm(triggerAt, type.requestCode, intent)
    }

    fun cancelCheckIn(type: CheckInType) {
        val intent = Intent(context, ProactiveCheckInReceiver::class.java).apply {
            action = ProactiveCheckInReceiver.ACTION_CHECK_IN
            putExtra(ProactiveCheckInReceiver.EXTRA_TYPE, type.name)
        }
        pendingIntent(type.requestCode, intent)?.let { alarmManager?.cancel(it) }
    }

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun setAlarm(triggerAt: Long, requestCode: Int, intent: Intent) {
        val manager = alarmManager ?: return
        val pi = pendingIntent(requestCode, intent) ?: return
        try {
            if (canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (e: Exception) {
            // Never crash the caller because of an alarm failure.
        }
    }

    private fun pendingIntent(requestCode: Int, intent: Intent): PendingIntent? {
        return try {
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** Parses "05:30" / "5:30" into a safe hour/minute pair. */
        fun parseTimeOfDay(value: String): Pair<Int, Int> {
            val parts = value.trim().split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        }

        /** The next epoch-millis timestamp for the given wall-clock time (today or tomorrow). */
        fun nextOccurrence(hour: Int, minute: Int, from: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = from
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= from) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        /** Real recurrence maths used after a repeating reminder fires. */
        fun nextTrigger(from: Long, repeatRule: String): Long {
            val calendar = Calendar.getInstance().apply { timeInMillis = from }
            when (repeatRule) {
                ReminderRepeat.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                ReminderRepeat.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                ReminderRepeat.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                ReminderRepeat.WEEKDAYS -> {
                    do {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY ||
                        calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                    )
                }
                else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        /** Human readable Bengali label for an absolute timestamp. */
        fun formatBengaliDateTime(timestamp: Long): String {
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val minuteText = if (minute < 10) "0$minute" else "$minute"
            val period = when (hour24) {
                in 4..11 -> "সকাল"
                in 12..15 -> "দুপুর"
                in 16..18 -> "বিকাল"
                in 19..20 -> "সন্ধ্যা"
                else -> "রাত"
            }
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            val months = listOf(
                "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
                "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
            )
            val monthName = months.getOrElse(calendar.get(Calendar.MONTH)) { "" }
            return "$period $hour12:$minuteText, ${calendar.get(Calendar.DAY_OF_MONTH)} $monthName"
        }
    }
}
