package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ArohiApplication
import com.example.engine.ProactiveSuggestionEngine
import com.example.device.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

/** Which pro-active check-in fired. */
enum class CheckInType(val requestCode: Int) {
    MORNING(2001),
    EVENING(2002);

    fun bengaliLabel(): String = if (this == MORNING) "সকালের চেক-ইন" else "রাতের চেক-ইন"
}

/**
 * Pro-active light check-in. AROHI herself reaches out — weather aware in the morning,
 * summary + wind-down in the evening — and then arms the next day's alarm.
 */
class ProactiveCheckInReceiver : BroadcastReceiver() {

    private val suggestionEngine = ProactiveSuggestionEngine()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CHECK_IN) return
        val app = context.applicationContext as? ArohiApplication ?: return
        val type = try {
            CheckInType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: CheckInType.MORNING.name)
        } catch (e: Exception) {
            CheckInType.MORNING
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val settings = app.settingsRepository
                if (type == CheckInType.MORNING && !settings.isMorningCheckInEnabled()) return@launch
                if (type == CheckInType.EVENING && !settings.isEveningCheckInEnabled()) return@launch

                val weather = if (settings.isWeatherAlertsEnabled()) {
                    runCatching { app.weatherRepository.refresh() }.getOrNull()
                } else {
                    null
                }
                val reminders = runCatching { app.reminderRepository.getActiveReminders() }.getOrDefault(emptyList())
                val todos = runCatching { app.todoRepository.getPendingTodos() }.getOrDefault(emptyList())
                val (battery, isCharging, _) = app.deviceStateManager.getBatteryInfo()

                val message = suggestionEngine.buildCheckIn(
                    type = type,
                    weather = weather,
                    reminders = reminders,
                    todos = todos,
                    batteryPercent = battery,
                    isCharging = isCharging,
                    userName = settings.getUserName()
                )

                NotificationHelper.show(
                    context = context,
                    channelId = NotificationHelper.CHANNEL_CHECK_IN,
                    notificationId = type.requestCode,
                    title = if (type == CheckInType.MORNING) "🌅 শুভ সকাল" else "🌙 শুভ সন্ধ্যা",
                    text = message,
                    bigText = message,
                    speakable = true
                )

                // Re-arm tomorrow's check-in because this alarm is one-shot.
                val time = if (type == CheckInType.MORNING) settings.getMorningCheckInTime()
                else settings.getEveningCheckInTime()
                ReminderScheduler(context).scheduleCheckIn(type, time)
            } catch (e: Exception) {
                // Ignore — the user can re-enable check-ins from settings.
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CHECK_IN = "com.example.arohi.action.CHECK_IN"
        const val EXTRA_TYPE = "extra_check_in_type"

        fun nextCheckInLabel(type: CheckInType, time: String): String {
            val (hour, minute) = ReminderScheduler.parseTimeOfDay(time)
            val next = ReminderScheduler.nextOccurrence(hour, minute)
            val calendar = Calendar.getInstance().apply { timeInMillis = next }
            return ReminderScheduler.formatBengaliDateTime(calendar.timeInMillis)
        }
    }
}
