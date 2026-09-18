package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ArohiApplication
import com.example.device.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AlarmManager forgets every alarm across reboots, so AROHI re-arms the user's real reminders
 * and pro-active check-ins right after the device starts.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val app = context.applicationContext as? ArohiApplication ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                app.reminderRepository.rescheduleAll()

                val settings = app.settingsRepository
                ReminderScheduler(context).scheduleCheckIns(
                    morningEnabled = settings.isMorningCheckInEnabled(),
                    morningTime = settings.getMorningCheckInTime(),
                    eveningEnabled = settings.isEveningCheckInEnabled(),
                    eveningTime = settings.getEveningCheckInTime()
                )
            } catch (e: Exception) {
                // Ignore — nothing else we can do at boot time.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
