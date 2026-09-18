package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Single place that creates AROHI's notification channels and posts real notifications for
 * reminders and pro-active check-ins.
 */
object NotificationHelper {

    const val CHANNEL_REMINDERS = "arohi_reminders_channel"
    const val CHANNEL_CHECK_IN = "arohi_checkin_channel"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "আরোহী রিমাইন্ডার",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "রিমাইন্ডার ও অ্যালার্মের জন্য আরোহীর নোটিফিকেশন"
            enableVibration(true)
        }

        val checkIn = NotificationChannel(
            CHANNEL_CHECK_IN,
            "আরোহী চেক-ইন",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "সকাল/রাতের হালকা চেক-ইন ও প্রাসঙ্গিক সাজেশন"
        }

        manager.createNotificationChannel(reminders)
        manager.createNotificationChannel(checkIn)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun show(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        text: String,
        bigText: String? = null,
        speakable: Boolean = false
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_SPEAK_ON_OPEN, speakable)
                putExtra(EXTRA_SPEAK_TEXT, if (speakable) text else null)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText ?: text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(
                if (channelId == CHANNEL_REMINDERS) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call — ignore.
        }
    }

    const val EXTRA_SPEAK_ON_OPEN = "extra_speak_on_open"
    const val EXTRA_SPEAK_TEXT = "extra_speak_text"
}
