package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RoutineRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskLogRepository
import com.example.core.agent.TaskAgent
import com.example.core.capability.CapabilityDeviceProbe
import com.example.core.intent.IntentClassifier
import com.example.core.permissions.PermissionManager
import com.example.core.personality.PersonalityEngine
import com.example.device.AppDiscoveryManager
import com.example.device.ContactsManager
import com.example.device.DeviceStateManager
import com.example.device.TelephonyHelper
import com.example.engine.ArohiBrain
import com.example.engine.CommandOrchestrator
import com.example.engine.EmotionEngine
import com.example.engine.LocalCommandEngine
import com.example.engine.VerificationEngine
import com.example.service.ArohiBackgroundService
import com.example.service.DiagnosticService
import com.example.system.ArohiDiagnostics
import com.example.time.AlarmClockEngine
import com.example.time.ReminderEngine
import com.example.time.TimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ArohiApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    val memoryRepository by lazy { MemoryRepository(database.memoryDao()) }
    val notificationRepository by lazy { NotificationRepository(database.notificationDao()) }
    val routineRepository by lazy { RoutineRepository(database.routineDao()) }
    val conversationRepository by lazy { ConversationRepository(database.messageDao()) }
    val taskLogRepository by lazy { TaskLogRepository(database.taskLogDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }

    val deviceStateManager by lazy { DeviceStateManager(this) }
    val appDiscoveryManager by lazy { AppDiscoveryManager(this) }
    val contactsManager by lazy { ContactsManager(this) }
    val telephonyHelper by lazy { TelephonyHelper(this) }
    val diagnosticService by lazy { DiagnosticService(this, settingsRepository) }

    val verificationEngine by lazy { VerificationEngine() }
    val emotionEngine by lazy { EmotionEngine() }

    // --- New modular core subsystems (spec §3) ---
    val personalityEngine by lazy { PersonalityEngine() }
    val permissionManager by lazy { PermissionManager(this) }
    val capabilityProbe by lazy { CapabilityDeviceProbe(this, settingsRepository) }
    val timerEngine by lazy { TimerEngine(this) }
    val alarmClockEngine by lazy { AlarmClockEngine(this) }
    val reminderEngine by lazy { ReminderEngine(this) }
    val taskAgent by lazy { TaskAgent(applicationScope) }

    /** Real subsystem diagnostics (spec §58/§60). Voice managers are provided lazily
     *  by the ViewModel when available; checks degrade to platform queries otherwise. */
    val arohiDiagnostics by lazy {
        ArohiDiagnostics(
            context = this,
            settingsRepository = settingsRepository,
            database = database,
            speechRecognizer = null,
            tts = null
        )
    }

    val intentClassifier by lazy {
        IntentClassifier(appAliases = appDiscoveryManager.buildAliasMap())
    }

    val commandOrchestrator by lazy {
        CommandOrchestrator(
            classifier = intentClassifier,
            permissionManager = permissionManager,
            personalityEngine = personalityEngine,
            timerEngine = timerEngine,
            alarmClockEngine = alarmClockEngine,
            diagnostics = arohiDiagnostics,
            memoryRepository = memoryRepository,
            notificationRepository = notificationRepository,
            settingsRepository = settingsRepository
        )
    }

    val localCommandEngine by lazy {
        LocalCommandEngine(
            deviceStateManager = deviceStateManager,
            appDiscoveryManager = appDiscoveryManager,
            contactsManager = contactsManager,
            telephonyHelper = telephonyHelper,
            memoryRepository = memoryRepository,
            notificationRepository = notificationRepository,
            routineRepository = routineRepository,
            settingsRepository = settingsRepository,
            verificationEngine = verificationEngine
        )
    }

    val brain by lazy {
        ArohiBrain(
            context = this,
            deviceStateManager = deviceStateManager,
            appDiscoveryManager = appDiscoveryManager,
            contactsManager = contactsManager,
            telephonyHelper = telephonyHelper,
            memoryRepository = memoryRepository,
            notificationRepository = notificationRepository,
            routineRepository = routineRepository,
            conversationRepository = conversationRepository,
            settingsRepository = settingsRepository,
            localCommandEngine = localCommandEngine,
            verificationEngine = verificationEngine,
            emotionEngine = emotionEngine,
            commandOrchestrator = commandOrchestrator,
            diagnosticsProvider = { arohiDiagnostics.runFullReport().renderText() },
            timerActions = object : ArohiBrain.TimerActions {
                override fun startSeconds(seconds: Long, label: String): String {
                    val res = timerEngine.start(seconds * 1000L, label)
                    return res.message
                }
                override fun list(): String {
                    val active = timerEngine.timers.value.filter {
                        it.state == com.example.time.TimerState.RUNNING
                    }
                    return if (active.isEmpty()) "চলমান কোনো টাইমার নেই।"
                    else active.joinToString("\n") {
                        "• ${it.label}: ${com.example.time.TimerEngine.formatDuration(it.remainingMillis)} বাকি"
                    }
                }
            },
            alarmActions = object : ArohiBrain.AlarmActions {
                override fun setAlarm(hour24: Int, minute: Int): String {
                    return alarmClockEngine.setAlarm(hour24, minute).message
                }
            }
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appDiscoveryManager.refreshInstalledApps()
    }

    companion object {
        lateinit var instance: ArohiApplication
            private set
    }
}
