package com.example.time

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.example.core.result.ArohiErrorCode
import com.example.core.result.ArohiResult
import com.example.core.text.DurationParser

/**
 * Real alarm/timer bridge to the system Clock app via the official
 * [AlarmClock] intent API (spec §27). This is the only supported way for a
 * third-party app to create alarms: it hands the request to the clock app which
 * actually schedules it. Arohi never pretends the alarm exists — the clock app
 * performs it and confirms to the user.
 */
class AlarmClockEngine(private val context: Context) {

    /** Creates a real alarm at [hour24]:[minute]. Opens the clock UI to confirm. */
    fun setAlarm(hour24: Int, minute: Int, label: String? = null, message: String = ""): ArohiResult<String> {
        return try {
            val hour = hour24.coerceIn(0, 23)
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_SKIP_UI, false) // always confirm in the real clock app
                putExtra(AlarmClock.EXTRA_VIBRATE, true)
                if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ArohiResult.success(
                message.ifBlank { "অ্যালার্ম সেট করার জন্য ক্লক অ্যাপ খোলা হয়েছে (${"%02d:%02d".format(hour, minute)})।" },
                verified = false // the system clock performs the actual persistence
            )
        } catch (e: Exception) {
            ArohiResult.failed(
                ArohiErrorCode.FEATURE_UNSUPPORTED,
                "কোনো ক্লক অ্যাপ পাওয়া যায়নি।",
                technicalCause = e.message
            )
        }
    }

    /** Creates a countdown timer in the system clock app (spec uses this for "timer" too). */
    fun setSystemTimer(seconds: Long, label: String? = null): ArohiResult<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1).toInt())
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ArohiResult.success("সিস্টেম টাইমার (${seconds}s) চালু করতে ক্লক অ্যাপ খোলা হয়েছে।", verified = false)
        } catch (e: Exception) {
            ArohiResult.failed(ArohiErrorCode.FEATURE_UNSUPPORTED, "টাইমার অ্যাপ পাওয়া যায়নি।", technicalCause = e.message)
        }
    }

    /** Shows the list of alarms in the clock app. */
    fun showAlarms(): ArohiResult<String> = try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        ArohiResult.success("অ্যালার্ম তালিকা খোলা হয়েছে।")
    } catch (e: Exception) {
        ArohiResult.failed(ArohiErrorCode.FEATURE_UNSUPPORTED, "ক্লক অ্যাপ পাওয়া যায়নি।", technicalCause = e.message)
    }

    companion object {
        /**
         * Helper used by the command layer: parses "সকাল ৭টায় alarm" into a clock
         * time, returning null when no time can be extracted (never fakes an alarm).
         */
        fun parseAlarmTime(text: String): DurationParser.ClockTime? =
            DurationParser.parseClockTime(text, assumePm = false)
    }
}
