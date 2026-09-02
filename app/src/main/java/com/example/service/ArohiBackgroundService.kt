package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real foreground service for the background assistant.
 *
 * Honest scope: this keeps a real Android foreground service alive with a persistent
 * notification and reports its real state. Android (and Samsung's power manager) can still
 * stop it — when that happens [state] becomes [BackgroundServiceState.STOPPED] and the UI
 * says so instead of pretending the assistant is still running.
 */
enum class BackgroundServiceState {
    STOPPED,
    STARTING,
    RUNNING,
    FAILED
}

class ArohiBackgroundService : Service() {

    companion object {
        private const val TAG = "ArohiBgService"
        const val CHANNEL_ID = "arohi_foreground_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.ACTION_START_FOREGROUND"
        const val ACTION_STOP = "com.example.ACTION_STOP_FOREGROUND"

        private val _state = MutableStateFlow(BackgroundServiceState.STOPPED)

        /** Real, observable service state. */
        val state: StateFlow<BackgroundServiceState> = _state.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError.asStateFlow()

        val isRunning: Boolean
            get() = _state.value == BackgroundServiceState.RUNNING

        /**
         * Asks Android to start the service. Returns false with a real reason when the
         * platform refuses (background start restrictions, missing permission, ...).
         */
        fun startService(context: Context): Result<Unit> {
            val intent = Intent(context, ArohiBackgroundService::class.java).apply {
                action = ACTION_START
            }
            return try {
                _state.value = BackgroundServiceState.STARTING
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Result.success(Unit)
            } catch (t: Throwable) {
                _state.value = BackgroundServiceState.FAILED
                _lastError.value = t.message ?: t.javaClass.simpleName
                Log.e(TAG, "Android refused to start the background service", t)
                Result.failure(t)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ArohiBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.stopService(intent)
            } catch (t: Throwable) {
                Log.w(TAG, "stopService failed: ${t.message}")
            } finally {
                _state.value = BackgroundServiceState.STOPPED
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            _state.value = BackgroundServiceState.STOPPED
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        return try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildForegroundNotification(),
                foregroundType()
            )
            _state.value = BackgroundServiceState.RUNNING
            _lastError.value = null
            // START_STICKY: Android restarts the service when it kills it for memory pressure.
            // It cannot survive a user "Force stop" or an OEM battery kill — that is reported.
            START_STICKY
        } catch (t: Throwable) {
            _state.value = BackgroundServiceState.FAILED
            _lastError.value = t.message ?: t.javaClass.simpleName
            Log.e(TAG, "startForeground rejected by the platform", t)
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // The UI was swiped away; the foreground service itself keeps running.
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_state.value != BackgroundServiceState.FAILED) {
            _state.value = BackgroundServiceState.STOPPED
        }
    }

    /**
     * Android 10+ requires the declared foreground service type to match a permission the app
     * actually holds. Requesting the microphone type without RECORD_AUDIO granted throws a
     * SecurityException on Android 14+, so the type is chosen from real permission state.
     */
    private fun foregroundType(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        val micGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        return if (micGranted) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
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
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ArohiBackgroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Arohi AI Assistant Active")
            .setContentText("Arohi AI Assistant by Shù Vrô is ready")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .addAction(0, "STOP", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
