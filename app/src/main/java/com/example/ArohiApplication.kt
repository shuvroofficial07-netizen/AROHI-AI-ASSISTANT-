package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RoutineRepository
import com.example.data.repository.SettingsRepository
import com.example.device.AppDiscoveryManager
import com.example.device.ContactsManager
import com.example.device.DeviceStateManager
import com.example.device.TelephonyHelper
import com.example.engine.ArohiBrain
import com.example.engine.EmotionEngine
import com.example.engine.LocalCommandEngine
import com.example.engine.VerificationEngine
import com.example.service.ArohiBackgroundService
import com.example.service.DiagnosticService
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
    val settingsRepository by lazy { SettingsRepository(this) }

    val deviceStateManager by lazy { DeviceStateManager(this) }
    val appDiscoveryManager by lazy { AppDiscoveryManager(this) }
    val contactsManager by lazy { ContactsManager(this) }
    val telephonyHelper by lazy { TelephonyHelper(this) }
    val diagnosticService by lazy { DiagnosticService(this, settingsRepository) }

    val verificationEngine by lazy { VerificationEngine() }
    val emotionEngine by lazy { EmotionEngine() }

    val localCommandEngine by lazy {
        LocalCommandEngine(
            deviceStateManager = deviceStateManager,
            appDiscoveryManager = appDiscoveryManager,
            contactsManager = contactsManager,
            telephonyHelper = telephonyHelper,
            memoryRepository = memoryRepository,
            notificationRepository = notificationRepository,
            routineRepository = routineRepository,
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
            emotionEngine = emotionEngine
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
