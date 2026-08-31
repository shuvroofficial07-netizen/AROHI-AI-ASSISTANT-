package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.ArohiApplication
import com.example.MainActivity
import com.example.R
import com.example.engine.SystemEventLevel

class ArohiBackgroundService : Service() {

    companion object {
        const val CHANNEL_ID = "arohi_foreground_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.ACTION_START_FOREGROUND"
        const val ACTION_STOP = "com.example.ACTION_STOP_FOREGROUND"

        var isRunning: Boolean = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, ArohiBackgroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ArohiBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            isRunning = false
            ArohiOverlayService.stopService(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Real floating indicator — only when Android overlay permission exists
        if (ArohiOverlayService.canDrawOverlays(this)) {
            ArohiOverlayService.startService(this)
        }
        val app = applicationContext as? ArohiApplication
        app?.eventBus?.log(
            "SERVICE",
            "Background assistant running" +
                if (!ArohiOverlayService.canDrawOverlays(this)) " (overlay not permitted)" else "",
            SystemEventLevel.SUCCESS
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        ArohiOverlayService.stopService(this)
        val app = applicationContext as? ArohiApplication
        app?.eventBus?.log("SERVICE", "Background assistant stopped", SystemEventLevel.WARNING)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Arohi AI Operating Layer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Arohi AI Assistant active in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Arohi AI Assistant Active")
            .setContentText("Arohi AI Assistant by Shù Vrô is ready")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
