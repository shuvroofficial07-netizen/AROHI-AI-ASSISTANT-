package com.example.service

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.ArohiApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ArohiNotificationListenerService : NotificationListenerService() {

    companion object {
        @Volatile
        var isConnected: Boolean = false
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

    // Service-scoped structured concurrency — all inserts are cancelled
    // reliably when the system destroys or disconnects the listener.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Honest state: access revoked or service disconnected — no stale "connected".
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return // Ignore self

        val extras = sbn.notification?.extras ?: return
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
            serviceScope.launch {
                try {
                    notificationRepo.insertNotification(
                        packageName = pkg,
                        appName = appName,
                        title = title,
                        text = text,
                        subText = subText,
                        priority = priority,
                        key = key
                    )
                } catch (e: Exception) {
                    // A failed insert must never crash the listener service.
                    android.util.Log.w(
                        "ArohiNotifListener",
                        "Failed to persist notification from $pkg: ${e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
        serviceScope.cancel()
    }
}
