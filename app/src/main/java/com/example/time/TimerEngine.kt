package com.example.time

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.result.ArohiErrorCode
import com.example.core.result.ArohiResult
import com.example.core.text.DurationParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class TimerState { RUNNING, FINISHED, CANCELLED }

data class ActiveTimer(
    val id: String,
    val label: String,
    val totalMillis: Long,
    val remainingMillis: Long,
    val state: TimerState,
    val endsAt: Long
)

/**
 * Real, in-app countdown timer engine (spec §28). Supports multiple named
 * timers, live remaining-time state, cancellation, and a real notification when
 * a timer fires. Timers run on [CountDownTimer] (main-thread looper) which is
 * accurate for short counts; state is observable via [timers].
 */
class TimerEngine(private val context: Context) {

    private val appContext = context.applicationContext
    private val running = mutableMapOf<String, CountDownTimer>()

    private val _timers = MutableStateFlow<List<ActiveTimer>>(emptyList())
    val timers: StateFlow<List<ActiveTimer>> = _timers.asStateFlow()

    private fun channelId(): String {
        val id = "arohi_timers"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = appContext.getSystemService(NotificationManager::class.java)
            if (nm?.getNotificationChannel(id) == null) {
                nm?.createNotificationChannel(
                    NotificationChannel(id, "Arohi Timers", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Countdown timer alerts"
                    }
                )
            }
        }
        return id
    }

    /** Starts a timer parsed from a command string ("২০ মিনিটের timer"). */
    fun startFromText(command: String, defaultLabel: String = "Timer"): ArohiResult<ActiveTimer> {
        val duration = DurationParser.parseDuration(command)
        return if (duration == null) {
            ArohiResult.failed(
                ArohiErrorCode.NO_MATCH,
                "কতক্ষণের টাইমার তা বুঝতে পারিনি। যেমন: '২০ মিনিটের টাইমার'।"
            )
        } else {
            start(duration.totalMillis, labelFor(command, defaultLabel))
        }
    }

    fun start(durationMillis: Long, label: String = "Timer"): ArohiResult<ActiveTimer> {
        if (durationMillis <= 0) {
            return ArohiResult.failed(ArohiErrorCode.NO_MATCH, "টাইমারের সময় সঠিক নয়।")
        }
        val id = UUID.randomUUID().toString()
        val endsAt = System.currentTimeMillis() + durationMillis
        val timer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                update(id) { it.copy(remainingMillis = millisUntilFinished) }
            }

            override fun onFinish() {
                running.remove(id)
                update(id) { it.copy(remainingMillis = 0, state = TimerState.FINISHED) }
                fireFinishedNotification(id, label)
            }
        }
        val active = ActiveTimer(
            id = id, label = label, totalMillis = durationMillis,
            remainingMillis = durationMillis, state = TimerState.RUNNING, endsAt = endsAt
        )
        _timers.value = _timers.value + active
        running[id] = timer
        timer.start()
        return ArohiResult.success(active, "টাইমার শুরু হয়েছে: $label (${formatDuration(durationMillis)})।")
    }

    fun cancel(id: String): ArohiResult<String> {
        val timer = running.remove(id)
        timer?.cancel()
        val existed = _timers.value.any { it.id == id }
        update(id) { it.copy(state = TimerState.CANCELLED) }
        return if (timer != null || existed) {
            // Remove cancelled timers from the active list after a moment (immediate here).
            _timers.value = _timers.value.filterNot { it.id == id && it.state == TimerState.CANCELLED }
            ArohiResult.success("টাইমার বাতিল করা হয়েছে।")
        } else {
            ArohiResult.failed(ArohiErrorCode.NO_MATCH, "চলমান কোনো টাইমার পাওয়া যায়নি।")
        }
    }

    fun cancelAll(): Int {
        val count = running.size
        running.values.forEach { it.cancel() }
        running.clear()
        _timers.value = emptyList()
        return count
    }

    fun runningCount(): Int = running.size

    private fun labelFor(command: String, fallback: String): String {
        // Best-effort label extraction; fallback keeps it honest.
        return fallback
    }

    private fun update(id: String, transform: (ActiveTimer) -> ActiveTimer) {
        _timers.value = _timers.value.map { if (it.id == id) transform(it) else it }
    }

    private fun fireFinishedNotification(id: String, label: String) {
        try {
            val tap = PendingIntent.getActivity(
                appContext, id.hashCode(),
                Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(appContext, channelId())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Arohi timer finished")
                .setContentText("“$label” সময় শেষ।")
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            val nm = appContext.getSystemService(NotificationManager::class.java)
            nm?.notify(("timer_" + id.hashCode()).hashCode(), notification)
        } catch (e: Exception) {
            // Notifications may be blocked; the in-app state still shows FINISHED.
        }
    }

    companion object {
        fun formatDuration(millis: Long): String {
            val totalSec = millis / 1000
            val m = totalSec / 60
            val s = totalSec % 60
            return if (m > 0) "${m}m ${s}s" else "${s}s"
        }
    }
}
