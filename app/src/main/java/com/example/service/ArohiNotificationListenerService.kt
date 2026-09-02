package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.TelephonyManager
import android.util.Log
import com.example.ArohiApplication
import com.example.data.repository.NotificationAnnouncePolicy
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArohiNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "ArohiNotifListener"

        var isConnected: Boolean = false
            private set

        fun isNotificationAccessGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val pkgName = context.packageName
            return flat.split(":").any { it.contains(pkgName) }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tts: TextToSpeechManager? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        releaseTts()
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
        releaseTts()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return // Ignore self
        if (sbn.isOngoing) return // Persistent/service notifications are not messages

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()

        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkg
        }

        val priority = sbn.notification.priority
        val key = sbn.key ?: ""
        val app = applicationContext as? ArohiApplication ?: return

        serviceScope.launch {
            try {
                app.notificationRepository.insertNotification(
                    packageName = pkg,
                    appName = appName,
                    title = title,
                    text = text,
                    subText = subText,
                    priority = priority,
                    key = key
                )
                maybeAnnounce(app, appName, title, text, priority)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to record notification: ${t.message}")
            }
        }
    }

    /**
     * Speaks a real notification only when the user has enabled it and the device is not
     * silenced. Content is never invented: when the notification carries no readable body we
     * say so explicitly.
     */
    private suspend fun maybeAnnounce(
        app: ArohiApplication,
        appName: String,
        title: String,
        text: String,
        priority: Int
    ) {
        val settings = app.settingsRepository
        val policy = settings.getNotificationAnnouncePolicy()
        if (policy == NotificationAnnouncePolicy.OFF) return
        if (settings.isSilenceMode()) return
        if (policy == NotificationAnnouncePolicy.IMPORTANT_ONLY && priority < Notification.PRIORITY_DEFAULT) return
        if (policy == NotificationAnnouncePolicy.SELECTED_APPS &&
            !settings.getAnnouncedPackages().any { appName.equals(it, ignoreCase = true) }
        ) {
            return
        }
        if (isDeviceSilenced() || isInCall()) return

        val sender = title.ifBlank { "একজন" }
        val spoken = if (settings.isPrivateMode() || text.isBlank()) {
            if (text.isBlank()) {
                "বস, $appName-এ $sender-এর একটি নতুন মেসেজ এসেছে, কিন্তু notification থেকে পুরো মেসেজটি পাওয়া যাচ্ছে না।"
            } else {
                "বস, $appName-এ $sender-এর একটি নতুন মেসেজ এসেছে। প্রাইভেসি মোড চালু আছে, তাই কনটেন্ট পড়ছি না।"
            }
        } else {
            "বস, $appName-এ $sender-এর একটা মেসেজ এসেছে: $text"
        }

        withContext(Dispatchers.Main) {
            val speaker = tts ?: TextToSpeechManager(applicationContext).also { tts = it }
            speaker.speak(spoken)
        }
    }

    /** Respects DND, silent/vibrate ringer mode — Arohi stays quiet when the phone is quiet. */
    private fun isDeviceSilenced(): Boolean {
        return try {
            val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audio != null && audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val filter = nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
                if (filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                    filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
                ) {
                    return true
                }
            }
            false
        } catch (t: Throwable) {
            false
        }
    }

    private fun isInCall(): Boolean {
        return try {
            val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audio?.mode == AudioManager.MODE_IN_CALL || audio?.mode == AudioManager.MODE_IN_COMMUNICATION
        } catch (t: Throwable) {
            false
        }
    }

    private fun releaseTts() {
        try {
            tts?.shutdown()
        } catch (t: Throwable) {
            Log.w(TAG, "TTS shutdown failed: ${t.message}")
        }
        tts = null
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
