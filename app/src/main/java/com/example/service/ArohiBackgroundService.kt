package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ArohiBackgroundService : Service() {

    companion object {
        const val CHANNEL_ID = "arohi_foreground_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.ACTION_START_FOREGROUND"
        const val ACTION_STOP = "com.example.ACTION_STOP_FOREGROUND"

        /** Honest lifecycle states — only RUNNING when the system really runs us. */
        enum class ServiceState {
            STOPPED, STARTING, RUNNING, STOPPING, ERROR
        }

        private val _state = MutableStateFlow(ServiceState.STOPPED)
        val state: StateFlow<ServiceState> = _state.asStateFlow()

        /** Last real error that prevented starting/running, if any. */
        @Volatile
        var lastError: String? = null
            private set

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startService(context: Context) {
            _state.value = ServiceState.STARTING
            val intent = Intent(context, ArohiBackgroundService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Real Android restriction (e.g. ForegroundServiceStartNotAllowedException
                // when started from the background on Android 12+): report it honestly.
                isRunning = false
                lastError = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "start failed"}"
                _state.value = ServiceState.ERROR
            }
        }

        fun stopService(context: Context) {
            _state.value = ServiceState.STOPPING
            val intent = Intent(context, ArohiBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                lastError = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "stop failed"}"
            }
            // If the service was never alive, stopService() does nothing and
            // onStartCommand never fires — reflect the real final state here.
            if (!isRunning) {
                _state.value = ServiceState.STOPPED
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            _state.value = ServiceState.STOPPING
            isRunning = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            _state.value = ServiceState.STOPPED
            return START_NOT_STICKY
        }

        // A null intent means the system restarted a previously-killed sticky
        // service — that is a real restart, so re-promote to foreground.
        try {
            val notification = buildForegroundNotification()
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // The service keeps the Arohi operating layer alive; it does NOT
                // capture audio, so the honest type is SPECIAL_USE (which avoids
                // the Android 14+ SecurityException a microphone-type FGS would
                // throw while RECORD_AUDIO is not granted).
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundServiceType)

            isRunning = true
            lastError = null
            _state.value = ServiceState.RUNNING
        } catch (e: Exception) {
            // e.g. SecurityException / InvalidForegroundServiceTypeException —
            // report the real failure and stop instead of crashing.
            isRunning = false
            lastError = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "startForeground failed"}"
            _state.value = ServiceState.ERROR
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (_state.value != ServiceState.ERROR) {
            _state.value = ServiceState.STOPPED
        }
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
