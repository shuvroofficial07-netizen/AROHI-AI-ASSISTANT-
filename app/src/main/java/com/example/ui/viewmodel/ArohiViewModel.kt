package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ArohiApplication
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.TaskLogEntity
import com.example.data.local.entity.TodoEntity
import com.example.data.remote.ClaudeClient
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiConnectionState
import com.example.data.remote.InlineData
import com.example.device.DeviceTelemetry
import com.example.device.ReminderScheduler
import com.example.device.WeatherSnapshot
import com.example.privacy.CloudSyncManager
import com.example.privacy.DataExporter
import com.example.service.CheckInType
import com.example.engine.ArohiEmotion
import com.example.service.ArohiAccessibilityService
import com.example.service.ArohiBackgroundService
import com.example.service.ArohiNotificationListenerService
import com.example.service.DiagnosticCategory
import com.example.service.DiagnosticItem
import com.example.service.DiagnosticReport
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskLogRepository
import com.example.service.DiagnosticService
import com.example.service.DiagnosticStatusLevel
import com.example.voice.SpeechRecognitionManager
import com.example.voice.SpeechState
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Streaming (typewriter) reply shown in the chat screen while AROHI composes her answer. */
data class LiveReply(
    val text: String,
    val isComplete: Boolean,
    val startedAt: Long = System.currentTimeMillis()
)

data class DiagnosticsStatus(
    val hasMicPermission: Boolean,
    val hasContactsPermission: Boolean,
    val isAccessibilityActive: Boolean,
    val isNotificationListenerActive: Boolean,
    val isBackgroundServiceActive: Boolean,
    val isGeminiConnected: Boolean,
    val geminiDetails: String
)

class ArohiViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ArohiApplication

    // State flows from repositories
    val messages: StateFlow<List<MessageEntity>> = app.conversationRepository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = app.notificationRepository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifCount: StateFlow<Int> = app.notificationRepository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val memories: StateFlow<List<MemoryEntity>> = app.memoryRepository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<RoutineEntity>> = app.routineRepository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real smart tasks backed by Room task_logs
    val taskLogs: StateFlow<List<TaskLogEntity>> = app.taskLogRepository.allTaskLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _runningTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    val runningTaskIds: StateFlow<Set<Long>> = _runningTaskIds.asStateFlow()

    // Live Device Telemetry
    private val _telemetry = MutableStateFlow(app.deviceStateManager.getTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    // Engine & Emotion states
    val emotion: StateFlow<ArohiEmotion> = app.emotionEngine.currentEmotion
    val isProcessing: StateFlow<Boolean> = app.brain.isProcessing

    // Settings
    val apiKeyFlow = app.settingsRepository.apiKeyFlow
    val modelNameFlow = app.settingsRepository.modelNameFlow
    val proactiveEnabledFlow = app.settingsRepository.proactiveEnabledFlow
    val silenceModeFlow = app.settingsRepository.silenceModeFlow
    val personalityIntensityFlow = app.settingsRepository.personalityIntensityFlow
    val pronounStyleFlow = app.settingsRepository.pronounStyleFlow
    val userNameFlow = app.settingsRepository.userNameFlow
    val userNicknameFlow = app.settingsRepository.userNicknameFlow
    val themeAccentFlow = app.settingsRepository.themeAccentFlow
    val avatarOutfitFlow = app.settingsRepository.avatarOutfitFlow
    val avatarModeFlow = app.settingsRepository.avatarModeFlow
    val fontScaleFlow = app.settingsRepository.fontScaleFlow
    val highContrastFlow = app.settingsRepository.highContrastFlow
    val streamingEnabledFlow = app.settingsRepository.streamingEnabledFlow
    val offlineFallbackFlow = app.settingsRepository.offlineFallbackFlow
    val encryptionFlow = app.settingsRepository.encryptionFlow
    val cloudSyncFlow = app.settingsRepository.cloudSyncFlow
    val morningCheckInFlow = app.settingsRepository.morningCheckInFlow
    val eveningCheckInFlow = app.settingsRepository.eveningCheckInFlow
    val weatherCityFlow = app.settingsRepository.weatherCityFlow
    val privateModeFlow = app.settingsRepository.privateModeFlow
    val autoMemoryFlow = app.settingsRepository.autoMemoryFlow
    val weatherAlertsFlow = app.settingsRepository.weatherAlertsFlow
    val voiceSpeedFlow = app.settingsRepository.voiceSpeedFlow
    val voicePitchFlow = app.settingsRepository.voicePitchFlow
    val assistantNameFlow = app.settingsRepository.assistantNameFlow
    val providerFlow = app.settingsRepository.providerFlow
    val anthropicKeyFlow = app.settingsRepository.anthropicKeyFlow
    val anthropicModelFlow = app.settingsRepository.anthropicModelFlow

    // Productivity data (Room backed)
    val reminders: StateFlow<List<ReminderEntity>> = app.reminderRepository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todos: StateFlow<List<TodoEntity>> = app.todoRepository.allTodos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = app.noteRepository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weather
    val weather: StateFlow<WeatherSnapshot?> = app.weatherRepository.snapshot
    val isWeatherRefreshing: StateFlow<Boolean> = app.weatherRepository.isRefreshing
    val weatherError: StateFlow<String?> = app.weatherRepository.lastError

    private val _proactiveSuggestion = MutableStateFlow<String?>(null)
    val proactiveSuggestion: StateFlow<String?> = _proactiveSuggestion.asStateFlow()

    private val _liveReply = MutableStateFlow<LiveReply?>(null)
    val liveReply: StateFlow<LiveReply?> = _liveReply.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastExportPath = MutableStateFlow<String?>(null)
    val lastExportPath: StateFlow<String?> = _lastExportPath.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow<String?>(null)
    val cloudSyncStatus: StateFlow<String?> = _cloudSyncStatus.asStateFlow()

    // Gemini connection status
    private val _geminiState = MutableStateFlow(GeminiConnectionState.DISCONNECTED)
    val geminiState: StateFlow<GeminiConnectionState> = _geminiState.asStateFlow()

    private val _geminiStatusMessage = MutableStateFlow("Initializing...")
    val geminiStatusMessage: StateFlow<String> = _geminiStatusMessage.asStateFlow()

    // Diagnostics
    val diagnosticReport: StateFlow<DiagnosticReport> = app.diagnosticService.report
    val isDiagnosticsChecking: StateFlow<Boolean> = app.diagnosticService.isChecking

    private val _diagnostics = MutableStateFlow(
        DiagnosticsStatus(
            hasMicPermission = false,
            hasContactsPermission = false,
            isAccessibilityActive = false,
            isNotificationListenerActive = false,
            isBackgroundServiceActive = false,
            isGeminiConnected = false,
            geminiDetails = "Checking..."
        )
    )
    val diagnostics: StateFlow<DiagnosticsStatus> = _diagnostics.asStateFlow()

    // Voice Manager Instances
    val ttsManager = TextToSpeechManager(
        context = app,
        onSpeakingStarted = {
            app.emotionEngine.setEmotion(ArohiEmotion.SPEAKING)
        },
        onSpeakingFinished = {
            if (app.emotionEngine.currentEmotion.value == ArohiEmotion.SPEAKING) {
                app.emotionEngine.setEmotion(ArohiEmotion.IDLE)
            }
        }
    )

    val speechManager = SpeechRecognitionManager(
        context = app,
        onResult = { recognizedText ->
            sendUserMessage(recognizedText, isVoice = true)
        },
        onError = { errorMsg ->
            app.emotionEngine.setEmotion(ArohiEmotion.IDLE)
        }
    )

    val speechState: StateFlow<SpeechState> = speechManager.speechState
    val rmsLevel: StateFlow<Float> = speechManager.rmsLevel
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    init {
        // Run initial diagnostics
        viewModelScope.launch(Dispatchers.IO) {
            runFullDiagnostics()
        }

        // Start telemetry polling loop
        viewModelScope.launch(Dispatchers.Default) {
            var tick = 0
            while (true) {
                _telemetry.value = app.deviceStateManager.getTelemetry()
                refreshDiagnostics()
                // Refresh the pro-active suggestion every 30s (cheap, uses cached weather)
                if (tick % 10 == 0) {
                    runCatching { refreshProactiveSuggestion() }
                }
                tick++
                delay(3000)
            }
        }

        // Apply TTS preferences
        ttsManager.setVoicePitch(app.settingsRepository.getVoicePitch())
        ttsManager.setVoiceSpeed(app.settingsRepository.getVoiceSpeed())

        // Re-arm the pro-active check-ins and refresh weather once on startup
        runCatching { applyCheckInSchedule() }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { app.weatherRepository.refresh(false) }
            runCatching { refreshProactiveSuggestion() }
        }
    }

    fun sendUserMessage(text: String, isVoice: Boolean = false, imageBase64: String? = null) {
        val cleanInput = text.trim()
        if (cleanInput.isEmpty() && imageBase64 == null) return

        viewModelScope.launch(Dispatchers.IO) {
            // Save user message to Room
            app.conversationRepository.addMessage(
                role = "USER",
                content = cleanInput.ifEmpty { "[Captured Image Analysis]" },
                isVoice = isVoice
            )

            val imageInline = imageBase64?.let {
                InlineData(mimeType = "image/jpeg", data = it)
            }

            val response = app.brain.processInput(cleanInput, imageInline)

            // Speak response if voice or general assistant response
            if (!app.settingsRepository.isSilenceMode() && response.text.isNotBlank()) {
                launch(Dispatchers.Main) {
                    ttsManager.speak(response.text)
                }
            }

            // Streaming / typing effect for the chat screen
            if (app.settingsRepository.isStreamingEnabled() && response.text.isNotBlank()) {
                revealProgressively(response.text)
            } else {
                _liveReply.value = null
            }
        }
    }

    /** Reveals a finished reply character by character so the UI feels alive. */
    private suspend fun revealProgressively(fullText: String) {
        val startedAt = System.currentTimeMillis()
        _liveReply.value = LiveReply("", false, startedAt)
        val chunk = if (fullText.length > 240) 4 else 2
        var index = 0
        while (index < fullText.length) {
            index = (index + chunk).coerceAtMost(fullText.length)
            _liveReply.value = LiveReply(fullText.substring(0, index), false, startedAt)
            delay(22)
        }
        _liveReply.value = LiveReply(fullText, true, startedAt)
        delay(500)
        _liveReply.value = null
    }

    fun startListening() {
        ttsManager.stop()
        speechManager.startListening("bn-BD")
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun silenceAssistant() {
        ttsManager.stop()
        speechManager.stopListening()
        app.emotionEngine.setEmotion(ArohiEmotion.CALM)
    }

    fun toggleFlashlight() {
        val current = _telemetry.value.isFlashlightOn
        app.deviceStateManager.toggleFlashlight(!current)
        _telemetry.value = app.deviceStateManager.getTelemetry()
    }

    fun setMediaVolume(percent: Int) {
        app.deviceStateManager.setMediaVolume(percent)
        _telemetry.value = app.deviceStateManager.getTelemetry()
    }

    fun saveApiKey(apiKey: String) {
        app.settingsRepository.setApiKey(apiKey)
        checkGeminiConnection(apiKey)
    }

    /** Switch the cloud brain between Gemini and Claude (both stay free to choose). */
    fun setBrainProvider(provider: String) {
        app.settingsRepository.setProvider(provider)
        val key = app.settingsRepository.getActiveApiKey()
        if (key.isNotBlank()) checkActiveProvider(key)
    }

    fun saveAnthropic(apiKey: String, model: String) {
        app.settingsRepository.setAnthropicApiKey(apiKey)
        if (model.isNotBlank()) app.settingsRepository.setAnthropicModel(model)
        app.settingsRepository.setProvider(SettingsRepository.PROVIDER_CLAUDE)
        if (apiKey.isNotBlank()) checkClaudeConnection(apiKey)
    }

    fun checkClaudeConnection(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _geminiState.value = GeminiConnectionState.CHECKING
            _geminiStatusMessage.value = "Checking Claude link..."
            val (state, msg) = ClaudeClient.testConnection(
                apiKey,
                app.settingsRepository.getAnthropicModel()
            )
            _geminiState.value = state
            _geminiStatusMessage.value = msg
            _statusMessage.value = msg
        }
    }

    /** Tests whichever provider is currently selected. */
    fun checkActiveProvider(apiKey: String) {
        if (app.settingsRepository.isClaudeProvider()) {
            checkClaudeConnection(apiKey)
        } else {
            checkGeminiConnection(apiKey)
        }
    }

    fun setAnthropicModelName(model: String) {
        app.settingsRepository.setAnthropicModel(model)
    }

    fun checkGeminiConnection(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _geminiState.value = GeminiConnectionState.CHECKING
            _geminiStatusMessage.value = "Checking Gemini link..."
            val (state, msg) = GeminiClient.testConnection(apiKey, app.settingsRepository.getModelName())
            _geminiState.value = state
            _geminiStatusMessage.value = msg
            runFullDiagnostics()
        }
    }

    fun runFullDiagnostics() {
        viewModelScope.launch(Dispatchers.IO) {
            val report = app.diagnosticService.runFullDiagnostics()
            val geminiItem = report.items.find { it.id == "gemini_ai" }
            val isGeminiOk = geminiItem?.status == DiagnosticStatusLevel.READY
            _geminiState.value = if (isGeminiOk) GeminiConnectionState.CONNECTED else GeminiConnectionState.DISCONNECTED
            _geminiStatusMessage.value = geminiItem?.details ?: "Tested"

            val hasMic = speechManager.hasMicPermission()
            val hasContacts = app.contactsManager.hasContactsPermission()
            val isAccess = ArohiAccessibilityService.isAccessibilityPermissionGranted(app)
            val isNotif = ArohiNotificationListenerService.isNotificationAccessGranted(app)
            val isBg = ArohiBackgroundService.isRunning

            _diagnostics.value = DiagnosticsStatus(
                hasMicPermission = hasMic,
                hasContactsPermission = hasContacts,
                isAccessibilityActive = isAccess,
                isNotificationListenerActive = isNotif,
                isBackgroundServiceActive = isBg,
                isGeminiConnected = isGeminiOk,
                geminiDetails = _geminiStatusMessage.value
            )
        }
    }

    fun refreshDiagnostics() {
        val hasMic = speechManager.hasMicPermission()
        val hasContacts = app.contactsManager.hasContactsPermission()
        val isAccess = ArohiAccessibilityService.isAccessibilityPermissionGranted(app)
        val isNotif = ArohiNotificationListenerService.isNotificationAccessGranted(app)
        val isBg = ArohiBackgroundService.isRunning
        val isGemini = _geminiState.value == GeminiConnectionState.CONNECTED

        _diagnostics.value = DiagnosticsStatus(
            hasMicPermission = hasMic,
            hasContactsPermission = hasContacts,
            isAccessibilityActive = isAccess,
            isNotificationListenerActive = isNotif,
            isBackgroundServiceActive = isBg,
            isGeminiConnected = isGemini,
            geminiDetails = _geminiStatusMessage.value
        )
    }

    fun startBackgroundOperatingService() {
        ArohiBackgroundService.startService(app)
        refreshDiagnostics()
    }

    fun stopBackgroundOperatingService() {
        ArohiBackgroundService.stopService(app)
        refreshDiagnostics()
    }

    fun saveMemory(category: String, key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            app.memoryRepository.saveMemory(category, key, value)
        }
    }

    fun deleteMemory(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            app.memoryRepository.deleteById(id)
        }
    }

    fun addRoutine(name: String, description: String, trigger: String, actionsJson: String, icon: String) {
        viewModelScope.launch(Dispatchers.IO) {
            app.routineRepository.addRoutine(name, description, trigger, actionsJson, icon)
        }
    }

    fun toggleRoutine(id: Int, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            app.routineRepository.toggleRoutine(id, enabled)
        }
    }

    fun deleteRoutine(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            app.routineRepository.deleteRoutine(id)
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            app.notificationRepository.markAsRead(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            app.notificationRepository.clearAll()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            app.conversationRepository.clearHistory()
        }
    }

    fun addSmartTask(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            app.taskLogRepository.addTask(clean)
        }
    }

    fun deleteSmartTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            app.taskLogRepository.deleteTask(id)
        }
    }

    fun clearFinishedTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            app.taskLogRepository.allTaskLogs.first().let { all ->
                all.filter { it.status == TaskLogRepository.STATUS_COMPLETED || it.status == TaskLogRepository.STATUS_FAILED }
                    .forEach { app.taskLogRepository.deleteTask(it.id) }
            }
        }
    }

    /**
     * Runs a saved smart task through the real AROHI brain (local engine first,
     * then Gemini + tools), then records the genuine outcome in Room.
     */
    fun runSmartTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_runningTaskIds.value.contains(id)) return@launch
            _runningTaskIds.value = _runningTaskIds.value + id
            app.taskLogRepository.markExecuting(id)
            try {
                val task = app.taskLogRepository.allTaskLogs.first().find { it.id == id }
                val command = task?.taskName ?: return@launch
                val response = app.brain.processInput(command)
                val succeeded = response.emotion != ArohiEmotion.ERROR && response.emotion != ArohiEmotion.CONFUSED
                app.taskLogRepository.markFinished(id, succeeded, response.text)
                if (!app.settingsRepository.isSilenceMode() && response.text.isNotBlank()) {
                    launch(Dispatchers.Main) {
                        ttsManager.speak(response.text)
                    }
                }
            } catch (e: Exception) {
                app.taskLogRepository.markFinished(id, false, e.localizedMessage ?: "Unknown error")
            } finally {
                _runningTaskIds.value = _runningTaskIds.value - id
            }
        }
    }

    // ------------------------------------------------------------------ productivity

    fun addReminder(title: String, timeText: String, repeatRule: String = "NONE") {
        val cleanTitle = title.trim().ifBlank { "রিমাইন্ডার" }
        val triggerAt = com.example.engine.TimePhraseParser.parse(timeText)
        if (triggerAt == null) {
            _statusMessage.value = "সময়টা বুঝতে পারিনি — যেমন লিখুন: আগামীকাল সকাল ৭টায়"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            app.reminderRepository.addReminder(cleanTitle, triggerAt, repeatRule = repeatRule)
            _statusMessage.value = "'$cleanTitle' ${ReminderScheduler.formatBengaliDateTime(triggerAt)}-এ সেট হয়েছে"
        }
    }

    fun toggleReminderDone(id: Long, isDone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { app.reminderRepository.markCompleted(id, isDone) }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { app.reminderRepository.deleteReminder(id) }
    }

    fun clearReminders() {
        viewModelScope.launch(Dispatchers.IO) { app.reminderRepository.clearAll() }
    }

    fun addTodo(title: String, priority: Int = 1) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) { app.todoRepository.addTodo(clean, priority = priority) }
    }

    fun toggleTodo(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { app.todoRepository.toggleTodo(id) }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { app.todoRepository.deleteTodo(id) }
    }

    fun clearCompletedTodos() {
        viewModelScope.launch(Dispatchers.IO) { app.todoRepository.clearCompleted() }
    }

    fun addNote(title: String, content: String) {
        val cleanContent = content.trim()
        if (cleanContent.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) { app.noteRepository.addNote(title, cleanContent) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { app.noteRepository.deleteNote(id) }
    }

    // ------------------------------------------------------------------ weather + pro-active

    fun refreshWeather(force: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            app.weatherRepository.refresh(force)
            refreshProactiveSuggestion()
        }
    }

    fun setWeatherCity(city: String) {
        app.settingsRepository.setWeatherCity(city)
        app.weatherRepository.clearCache()
        refreshWeather(true)
    }

    fun refreshProactiveSuggestion() {
        viewModelScope.launch(Dispatchers.IO) {
            val telemetry = app.deviceStateManager.getTelemetry()
            val suggestion = app.proactiveSuggestionEngine.suggestion(
                weather = app.weatherRepository.cached(),
                reminders = app.reminderRepository.getActiveReminders(),
                todos = app.todoRepository.getPendingTodos(),
                batteryPercent = telemetry.batteryPercent,
                isCharging = telemetry.isCharging
            )
            _proactiveSuggestion.value = suggestion
            if (app.settingsRepository.isCloudSyncEnabled()) {
                runCatching { CloudSyncManager.upload(app, DataExporter.buildExportJson(app)) }
            }
        }
    }

    fun setMorningCheckIn(enabled: Boolean) {
        app.settingsRepository.setMorningCheckInEnabled(enabled)
        applyCheckInSchedule()
    }

    fun setEveningCheckIn(enabled: Boolean) {
        app.settingsRepository.setEveningCheckInEnabled(enabled)
        applyCheckInSchedule()
    }

    fun setCheckInTimes(morning: String, evening: String) {
        app.settingsRepository.setMorningCheckInTime(morning)
        app.settingsRepository.setEveningCheckInTime(evening)
        applyCheckInSchedule()
    }

    private fun applyCheckInSchedule() {
        app.reminderScheduler.scheduleCheckIns(
            morningEnabled = app.settingsRepository.isMorningCheckInEnabled(),
            morningTime = app.settingsRepository.getMorningCheckInTime(),
            eveningEnabled = app.settingsRepository.isEveningCheckInEnabled(),
            eveningTime = app.settingsRepository.getEveningCheckInTime()
        )
    }

    fun previewCheckIn(type: CheckInType) {
        viewModelScope.launch(Dispatchers.IO) {
            val telemetry = app.deviceStateManager.getTelemetry()
            val message = app.proactiveSuggestionEngine.buildCheckIn(
                type = type,
                weather = app.weatherRepository.cached(),
                reminders = app.reminderRepository.getActiveReminders(),
                todos = app.todoRepository.getPendingTodos(),
                batteryPercent = telemetry.batteryPercent,
                isCharging = telemetry.isCharging,
                userName = app.settingsRepository.getUserName()
            )
            withContext(Dispatchers.Main) { ttsManager.speak(message) }
            _statusMessage.value = message
        }
    }

    // ------------------------------------------------------------------ personalisation

    fun setPrivateMode(enabled: Boolean) = app.settingsRepository.setPrivateMode(enabled)

    fun setPersonalityIntensity(value: Float) = app.settingsRepository.setPersonalityIntensity(value)

    fun setPronounStyle(value: String) = app.settingsRepository.setPronounStyle(value)

    fun setUserName(value: String) = app.settingsRepository.setUserName(value)

    fun setUserNickname(value: String) = app.settingsRepository.setUserNickname(value)

    fun setThemeAccent(value: String) = app.settingsRepository.setThemeAccent(value)

    fun setAvatarOutfit(value: String) = app.settingsRepository.setAvatarOutfit(value)

    fun setAvatarMode(value: String) = app.settingsRepository.setAvatarMode(value)

    fun setFontScale(value: Float) = app.settingsRepository.setFontScale(value)

    fun setHighContrast(enabled: Boolean) = app.settingsRepository.setHighContrastEnabled(enabled)

    fun setStreamingEnabled(enabled: Boolean) = app.settingsRepository.setStreamingEnabled(enabled)

    fun setOfflineFallbackEnabled(enabled: Boolean) = app.settingsRepository.setOfflineFallbackEnabled(enabled)

    fun setLocalEncryptionEnabled(enabled: Boolean) {
        app.settingsRepository.setLocalEncryptionEnabled(enabled)
        _statusMessage.value = if (enabled) "লোকাল এনক্রিপশন চালু হয়েছে" else "লোকাল এনক্রিপশন বন্ধ হয়েছে"
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        app.settingsRepository.setCloudSyncEnabled(enabled)
        _statusMessage.value = if (enabled) "ক্লাউড সিঙ্ক চালু হয়েছে" else "ক্লাউড সিঙ্ক বন্ধ হয়েছে"
    }

    fun setWeatherAlertsEnabled(enabled: Boolean) = app.settingsRepository.setWeatherAlertsEnabled(enabled)

    fun setAutoMemoryEnabled(enabled: Boolean) = app.settingsRepository.setAutoMemoryEnabled(enabled)

    fun setVoiceSpeed(value: Float) {
        app.settingsRepository.setVoiceSpeed(value)
        ttsManager.setVoiceSpeed(value)
    }

    fun setVoicePitch(value: Float) {
        app.settingsRepository.setVoicePitch(value)
        ttsManager.setVoicePitch(value)
    }

    fun previewEmotion(emotion: ArohiEmotion) {
        app.emotionEngine.setEmotion(emotion)
    }

    fun speakText(text: String) {
        ttsManager.speak(text)
    }

    /** Used when a notification was tapped with a speakable payload. */
    fun speakExternalText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.Main) { ttsManager.speak(text) }
    }

    // ------------------------------------------------------------------ privacy & data

    fun exportUserData(share: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = DataExporter.writeExportFile(app, app)
            _lastExportPath.value = file?.absolutePath
            _statusMessage.value = if (file != null) "এক্সপোর্ট সম্পন্ন: ${file.name}" else "এক্সপোর্ট ব্যর্থ হয়েছে"
            if (file != null && share) {
                withContext(Dispatchers.Main) { DataExporter.shareExport(app, file) }
            }
        }
    }

    fun shareLastExport() {
        val path = _lastExportPath.value ?: return
        val file = java.io.File(path)
        if (file.exists()) DataExporter.shareExport(app, file)
    }

    fun syncToCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!CloudSyncManager.isAvailable(app)) {
                _cloudSyncStatus.value = "Firebase কনফিগার করা নেই — google-services.json যোগ করলে সিঙ্ক চালু হবে।"
                return@launch
            }
            val payload = DataExporter.buildExportJson(app)
            val result = CloudSyncManager.upload(app, payload)
            _cloudSyncStatus.value = result.fold(
                onSuccess = { "ক্লাউডে ব্যাকআপ হয়েছে ✅" },
                onFailure = { "ব্যাকআপ ব্যর্থ: ${it.localizedMessage}" }
            )
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            _cloudSyncStatus.value = "ক্লাউড থেকে নামানো হচ্ছে..."
            val result = CloudSyncManager.download(app)
            _cloudSyncStatus.value = result.fold(
                onSuccess = { payload ->
                    val restored = runCatching { applyCloudBackup(payload) }.getOrElse {
                        return@fold "রিস্টোর ব্যর্থ: ${it.localizedMessage}"
                    }
                    "রিস্টোর সম্পন্ন — $restored"
                },
                onFailure = { "রিস্টোর ব্যর্থ: ${it.localizedMessage}" }
            )
        }
    }

    /**
     * Re-applies a previously uploaded backup. Items are merged by key/title so restoring twice
     * never creates duplicates, and reminders are re-armed with the real AlarmManager.
     */
    private suspend fun applyCloudBackup(payload: String): String {
        val root = org.json.JSONObject(payload)
        var memoryCount = 0
        var todoCount = 0
        var noteCount = 0
        var reminderCount = 0

        root.optJSONArray("memories")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val key = item.optString("key").trim()
                val value = item.optString("value").trim()
                if (key.isBlank() || value.isBlank()) continue
                if (app.memoryRepository.getByKey(key) != null) continue
                app.memoryRepository.saveMemory(
                    category = item.optString("category", "IMPORTANT_FACTS"),
                    key = key,
                    value = value
                )
                memoryCount++
            }
        }

        root.optJSONArray("todos")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                if (title.isBlank()) continue
                val done = item.optBoolean("isDone", item.optBoolean("isCompleted", false))
                if (done) continue
                if (app.todoRepository.findOpenByTitle(title) != null) continue
                app.todoRepository.addTodo(
                    title = title,
                    details = item.optString("details", ""),
                    priority = item.optInt("priority", 1)
                )
                todoCount++
            }
        }

        root.optJSONArray("notes")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                val content = item.optString("content")
                if (title.isBlank() && content.isBlank()) continue
                val existing = app.noteRepository.getNotes().any { it.title == title && it.content == content }
                if (existing) continue
                app.noteRepository.addNote(title.ifBlank { "নোট" }, content)
                noteCount++
            }
        }

        val now = System.currentTimeMillis()
        root.optJSONArray("reminders")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                var triggerAt = item.optLong("triggerAt", 0L)
                if (title.isBlank() || item.optBoolean("isCompleted", false)) continue
                // Past-dated entries move to "tomorrow at the same time" instead of firing instantly.
                if (triggerAt <= now) triggerAt = now + 24L * 60L * 60L * 1000L
                val exists = app.reminderRepository.getActiveReminders().any { it.title == title }
                if (exists) continue
                app.reminderRepository.addReminder(
                    title = title,
                    triggerAt = triggerAt,
                    repeatRule = item.optString("repeatRule", "NONE")
                )
                reminderCount++
            }
        }

        refreshProactiveSuggestion()
        return "মেমোরি $memoryCount, টু-ডু $todoCount, নোট $noteCount, রিমাইন্ডার $reminderCount যোগ হয়েছে"
    }

    fun deleteAllUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            DataExporter.deleteAllLocalData(app)
            _statusMessage.value = "সব লোকাল ডেটা মুছে ফেলা হয়েছে"
        }
    }

    fun dismissStatus() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        speechManager.destroy()
    }
}
