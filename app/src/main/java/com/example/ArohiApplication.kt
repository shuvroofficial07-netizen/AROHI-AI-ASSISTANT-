package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.NoteRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ReminderRepository
import com.example.data.repository.RoutineRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskLogRepository
import com.example.data.repository.TodoRepository
import com.example.data.repository.WeatherRepository
import com.example.device.AppDiscoveryManager
import com.example.device.CalendarHelper
import com.example.device.ContactsManager
import com.example.device.DeviceStateManager
import com.example.device.ReminderScheduler
import com.example.device.TelephonyHelper
import com.example.engine.ArohiBrain
import com.example.engine.EmotionEngine
import com.example.engine.LocalCommandEngine
import com.example.engine.MemoryExtractor
import com.example.engine.OfflineFallbackEngine
import com.example.engine.PersonaEngine
import com.example.engine.ProactiveSuggestionEngine
import com.example.engine.ProductivityEngine
import com.example.engine.VerificationEngine
import com.example.service.ArohiBackgroundService
import com.example.service.DiagnosticService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ArohiApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    val memoryRepository by lazy { MemoryRepository(database.memoryDao()) }
    val notificationRepository by lazy { NotificationRepository(database.notificationDao()) }
    val routineRepository by lazy { RoutineRepository(database.routineDao()) }
    val conversationRepository by lazy { ConversationRepository(database.messageDao()) }
    val taskLogRepository by lazy { TaskLogRepository(database.taskLogDao()) }
    val reminderRepository by lazy { ReminderRepository(database.reminderDao(), reminderScheduler) }
    val todoRepository by lazy { TodoRepository(database.todoDao()) }
    val noteRepository by lazy { NoteRepository(database.noteDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }

    val deviceStateManager by lazy { DeviceStateManager(this) }
    val appDiscoveryManager by lazy { AppDiscoveryManager(this) }
    val contactsManager by lazy { ContactsManager(this) }
    val telephonyHelper by lazy { TelephonyHelper(this) }
    val diagnosticService by lazy { DiagnosticService(this, settingsRepository) }

    val reminderScheduler by lazy { ReminderScheduler(this) }
    val calendarHelper by lazy { CalendarHelper(this) }
    val weatherRepository by lazy { WeatherRepository(this, settingsRepository) }

    val verificationEngine by lazy { VerificationEngine() }
    val emotionEngine by lazy { EmotionEngine() }
    val personaEngine by lazy { PersonaEngine() }
    val proactiveSuggestionEngine by lazy { ProactiveSuggestionEngine() }
    val offlineFallbackEngine by lazy { OfflineFallbackEngine() }
    val memoryExtractor by lazy { MemoryExtractor(memoryRepository, settingsRepository) }

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

    val productivityEngine by lazy {
        ProductivityEngine(
            reminderRepository = reminderRepository,
            todoRepository = todoRepository,
            noteRepository = noteRepository,
            weatherRepository = weatherRepository,
            calendarHelper = calendarHelper,
            offlineFallbackEngine = offlineFallbackEngine
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
            reminderRepository = reminderRepository,
            todoRepository = todoRepository,
            noteRepository = noteRepository,
            weatherRepository = weatherRepository,
            calendarHelper = calendarHelper,
            productivityEngine = productivityEngine,
            offlineFallbackEngine = offlineFallbackEngine,
            proactiveSuggestionEngine = proactiveSuggestionEngine,
            memoryExtractor = memoryExtractor,
            personaEngine = personaEngine
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appDiscoveryManager.refreshInstalledApps()

        // Re-arm real alarms after a cold start and keep the pro-active check-ins alive.
        applicationScope.launch(Dispatchers.IO) {
            runCatching { reminderRepository.rescheduleAll() }
            runCatching {
                reminderScheduler.scheduleCheckIns(
                    morningEnabled = settingsRepository.isMorningCheckInEnabled(),
                    morningTime = settingsRepository.getMorningCheckInTime(),
                    eveningEnabled = settingsRepository.isEveningCheckInEnabled(),
                    eveningTime = settingsRepository.getEveningCheckInTime()
                )
            }
        }
    }

    /** Convenience used by the UI to start the operating layer. */
    fun startBackgroundLayer() {
        ArohiBackgroundService.startService(this)
    }

    companion object {
        lateinit var instance: ArohiApplication
            private set
    }
}
