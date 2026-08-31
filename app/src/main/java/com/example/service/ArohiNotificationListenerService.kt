package com.example.service

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import com.example.ArohiApplication
import com.example.engine.SystemEventLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArohiNotificationListenerService : NotificationListenerService() {

    companion object {
        var isConnected: Boolean = false
            private set

        var instance: ArohiNotificationListenerService? = null
            private set

        fun isNotificationAccessGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val pkgName = context.packageName
            return flat.contains(pkgName)
        }
    }

    private var announcementTts: TextToSpeech? = null
    private var ttsReady = false

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        instance = this
        val app = applicationContext as? ArohiApplication
        app?.eventBus?.log("INBOX", "Notification listener connected", SystemEventLevel.SUCCESS)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        if (instance == this) instance = null
        val app = applicationContext as? ArohiApplication
        app?.eventBus?.log("INBOX", "Notification listener disconnected", SystemEventLevel.WARNING)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return // Ignore self

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkg
        }

        val priority = sbn.notification.priority
        val key = sbn.key ?: ""

        val app = applicationContext as? ArohiApplication
        val notificationRepo = app?.notificationRepository

        if (notificationRepo != null) {
            CoroutineScope(Dispatchers.IO).launch {
                notificationRepo.insertNotification(
                    packageName = pkg,
                    appName = appName,
                    title = title,
                    text = text,
                    subText = subText,
                    priority = priority,
                    key = key
                )
            }
        }
        app?.eventBus?.log("INBOX", "Notification from $appName", SystemEventLevel.INFO)

        // Real voice announcement — respects silence, private mode and user settings
        val settings = app?.settingsRepository
        if (settings != null &&
            !settings.isSilenceMode() &&
            !settings.isPrivateMode() &&
            settings.isNotificationAnnouncementEnabled()
        ) {
            val message = if (text.isNotBlank()) {
                "$appName থেকে নোটিফিকেশন: $title। $text"
            } else {
                "$appName থেকে নোটিফিকেশন: $title"
            }
            speakAnnouncement(message)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Removal is reflected by the user dismissing it in the shade or in AROHI Inbox
    }

    /** Real dismiss: cancels the system notification by its captured key. */
    fun dismissNotificationByKey(key: String): Boolean {
        if (key.isBlank()) return false
        return try {
            cancelNotification(key)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun speakAnnouncement(message: String) {
        if (message.isBlank()) return
        if (announcementTts == null) {
            announcementTts = TextToSpeech(applicationContext) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    val clean = message.replace(Regex("[*#_`~]"), "").trim()
                    announcementTts?.speak(clean, TextToSpeech.QUEUE_ADD, null, "arohi_notif_${System.currentTimeMillis()}")
                }
            }
        } else if (ttsReady) {
            val clean = message.replace(Regex("[*#_`~]"), "").trim()
            announcementTts?.speak(clean, TextToSpeech.QUEUE_ADD, null, "arohi_notif_${System.currentTimeMillis()}")
        }
    }

    override fun onDestroy() {
        try {
            announcementTts?.stop()
            announcementTts?.shutdown()
        } catch (e: Exception) {
            // Ignored
        }
        announcementTts = null
        ttsReady = false
        super.onDestroy()
    }
}
