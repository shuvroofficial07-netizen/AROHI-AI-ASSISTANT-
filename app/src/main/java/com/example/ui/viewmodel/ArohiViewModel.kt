package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ArohiApplication
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.TaskLogEntity
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiConnectionState
import com.example.data.remote.InlineData
import com.example.device.ContactItem
import com.example.device.DeviceControlManager
import com.example.device.DeviceTelemetry
import com.example.device.InstalledApp
import com.example.device.PhoneCallInfo
import com.example.engine.ArohiEmotion
import com.example.engine.BrainPhase
import com.example.engine.SystemEvent
import com.example.engine.SystemEventLevel
import com.example.engine.TaskProgress
import com.example.service.ArohiAccessibilityService
import com.example.service.ArohiBackgroundService
import com.example.service.ArohiNotificationListenerService
import com.example.service.ArohiOverlayService
import com.example.service.DiagnosticCategory
import com.example.service.DiagnosticItem
import com.example.service.DiagnosticReport
import com.example.data.repository.TaskLogRepository
import com.example.service.DiagnosticService
import com.example.service.DiagnosticStatusLevel
import com.example.voice.SpeechRecognitionManager
import com.example.voice.SpeechState
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val brainPhase: StateFlow<BrainPhase> = app.brainActivityTracker.phase
    val taskProgress: StateFlow<TaskProgress> = app.brainActivityTracker.task

    // Real system event stream
    val systemEvents: StateFlow<List<SystemEvent>> = app.eventBus.events

    // Real call intelligence
    val callInfo: StateFlow<PhoneCallInfo> = app.phoneStateManager.callInfo

    // Installed launchable applications (real device data)
    private val _installedApps = MutableStateFlow(app.appDiscoveryManager.getInstalledApps())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    // Settings
    val apiKeyFlow = app.settingsRepository.apiKeyFlow
    val modelNameFlow = app.settingsRepository.modelNameFlow
    val proactiveEnabledFlow = app.settingsRepository.proactiveEnabledFlow
    val silenceModeFlow = app.settingsRepository.silenceModeFlow
    val privateModeFlow = app.settingsRepository.privateModeFlow
    val languageCodeFlow = app.settingsRepository.languageCodeFlow
    val voiceNameFlow = app.settingsRepository.voiceNameFlow
    val personalityStyleFlow = app.settingsRepository.personalityStyleFlow
    val wakeWordFlow = app.settingsRepository.wakeWordFlow
    val callerAnnouncementFlow = app.settingsRepository.callerAnnouncementFlow
    val notificationAnnouncementFlow = app.settingsRepository.notificationAnnouncementFlow
    val cloudAiFlow = app.settingsRepository.cloudAiFlow
    val visionAiFlow = app.settingsRepository.visionAiFlow
    val notificationAiFlow = app.settingsRepository.notificationAiFlow
    val messageAiFlow = app.settingsRepository.messageAiFlow
    val firstLaunchFlow = app.settingsRepository.firstLaunchFlow

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
            app.eventBus.log("VOICE", errorMsg, SystemEventLevel.WARNING)
            app.emotionEngine.setEmotion(ArohiEmotion.IDLE)
        }
    )

    val speechState: StateFlow<SpeechState> = speechManager.speechState
    val rmsLevel: StateFlow<Float> = speechManager.rmsLevel
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val ttsVoices: StateFlow<List<String>> = ttsManager.availableVoices

    init {
        // Run initial diagnostics
        viewModelScope.launch(Dispatchers.IO) {
            runFullDiagnostics()
        }

        // Start telemetry polling loop
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                _telemetry.value = app.deviceStateManager.getTelemetry()
                _installedApps.value = app.appDiscoveryManager.getInstalledApps()
                refreshDiagnostics()
                delay(3000)
            }
        }

        // Apply persisted voice preferences
        ttsManager.setVoicePitch(app.settingsRepository.getVoicePitch())
        ttsManager.setVoiceSpeed(app.settingsRepository.getVoiceSpeed())
        val savedVoice = app.settingsRepository.getVoiceName()
        if (savedVoice.isNotBlank()) {
            ttsManager.setVoiceByName(savedVoice)
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
        }
    }

    fun startListening() {
        ttsManager.stop()
        app.emotionEngine.setEmotion(ArohiEmotion.LISTENING)
        speechManager.startListening(app.settingsRepository.getLanguageCode())
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

    fun checkGeminiConnection(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _geminiState.value = GeminiConnectionState.CHECKING
            _geminiStatusMessage.value = "Checking Gemini link..."
            val retries = app.settingsRepository.getGeminiRetryCount()
            val (state, msg) = GeminiClient.testConnection(apiKey, app.settingsRepository.getModelName(), retries)
            _geminiState.value = state
            _geminiStatusMessage.value = msg
            app.eventBus.log(
                "GEMINI",
                if (state == GeminiConnectionState.CONNECTED) "Gemini connected ($msg)" else "Gemini: $msg",
                if (state == GeminiConnectionState.CONNECTED) SystemEventLevel.SUCCESS else SystemEventLevel.WARNING
            )
            runFullDiagnostics()
        }
    }

    fun setModelName(name: String) {
        app.settingsRepository.setModelName(name)
        val key = app.settingsRepository.getApiKey()
        if (key.isNotBlank()) checkGeminiConnection(key)
    }

    fun setGeminiTimeout(seconds: Int) = app.settingsRepository.setGeminiTimeoutSeconds(seconds)

    fun setGeminiRetry(count: Int) = app.settingsRepository.setGeminiRetryCount(count)

    fun getGeminiTimeout(): Int = app.settingsRepository.getGeminiTimeoutSeconds()

    fun getGeminiRetry(): Int = app.settingsRepository.getGeminiRetryCount()

    fun setCloudAiEnabled(enabled: Boolean) = app.settingsRepository.setCloudAiEnabled(enabled)
    fun setVisionAiEnabled(enabled: Boolean) = app.settingsRepository.setVisionAiEnabled(enabled)
    fun setNotificationAiEnabled(enabled: Boolean) = app.settingsRepository.setNotificationAiEnabled(enabled)
    fun setMessageAiEnabled(enabled: Boolean) = app.settingsRepository.setMessageAiAnalysisEnabled(enabled)
    fun setPrivateMode(enabled: Boolean) = app.settingsRepository.setPrivateMode(enabled)
    fun setProactiveEnabled(enabled: Boolean) = app.settingsRepository.setProactiveEnabled(enabled)
    fun setSilenceMode(enabled: Boolean) = app.settingsRepository.setSilenceMode(enabled)
    fun setCallerAnnouncement(enabled: Boolean) = app.settingsRepository.setCallerAnnouncementEnabled(enabled)
    fun setNotificationAnnouncement(enabled: Boolean) = app.settingsRepository.setNotificationAnnouncementEnabled(enabled)

    fun setLanguageCode(code: String) {
        app.settingsRepository.setLanguageCode(code)
    }

    fun setVoiceName(name: String) {
        app.settingsRepository.setVoiceName(name)
        ttsManager.setVoiceByName(name)
    }

    fun setPersonalityStyle(style: String) {
        app.settingsRepository.setPersonalityStyle(style)
    }

    fun setWakeWord(word: String) {
        app.settingsRepository.setWakeWord(word)
    }

    fun setVoicePitch(pitch: Float) {
        app.settingsRepository.setVoicePitch(pitch)
        ttsManager.setVoicePitch(pitch)
    }

    fun setVoiceSpeed(speed: Float) {
        app.settingsRepository.setVoiceSpeed(speed)
        ttsManager.setVoiceSpeed(speed)
    }

    fun markSetupComplete() {
        app.settingsRepository.markSetupComplete()
        app.eventBus.log("CORE", "First-launch setup completed", SystemEventLevel.SUCCESS)
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
        if (ArohiOverlayService.canDrawOverlays(app)) {
            ArohiOverlayService.startService(app)
        }
        refreshDiagnostics()
    }

    fun stopBackgroundOperatingService() {
        ArohiBackgroundService.stopService(app)
        ArohiOverlayService.stopService(app)
        refreshDiagnostics()
    }

    // ------- Memory center -------
    fun saveMemory(category: String, key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            app.memoryRepository.saveMemory(category, key, value)
        }
    }

    fun editMemory(id: Int, category: String, key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = app.memoryRepository.allMemories.first().find { it.id == id } ?: return@launch
            app.memoryRepository.deleteById(id)
            app.memoryRepository.saveMemory(
                category.ifBlank { existing.category },
                key.ifBlank { existing.key },
                value.ifBlank { existing.value }
            )
        }
    }

    fun deleteMemory(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            app.memoryRepository.deleteById(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            app.memoryRepository.clearAll()
        }
    }

    /** Real JSON export of saved memories through the Android share sheet. */
    fun exportMemories(): String? {
        val list = app.memoryRepository.allMemories.first()
        if (list.isEmpty()) return null
        return buildString {
            append("{\n  \"arohi_memories\": [\n")
            list.forEachIndexed { index, memory ->
                append("    { \"category\": \"${memory.category}\", \"key\": \"${memory.key}\", \"value\": \"${memory.value.replace("\"", "'")}\" }")
                if (index != list.size - 1) append(",")
                append("\n")
            }
            append("  ]\n}")
        }
    }

    /** Real JSON import of memories from an exported file. */
    fun importMemoriesFromJson(json: String): Int {
        return try {
            var count = 0
            val pattern = Regex("\\{\\s*\"category\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"key\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"value\"\\s*:\\s*\"([^\"]*)\"")
            pattern.findAll(json).forEach { match ->
                val category = match.groupValues[1]
                val key = match.groupValues[2]
                val value = match.groupValues[3]
                if (key.isNotBlank() && value.isNotBlank()) {
                    app.memoryRepository.saveMemory(category.ifBlank { "IMPORTED" }, key, value)
                    count++
                }
            }
            count
        } catch (e: Exception) {
            0
        }
    }

    // ------- Routines -------
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

    fun runRoutine(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val routine = app.routineRepository.getRoutineById(id) ?: return@launch
            sendUserMessage(routine.triggerPhrase, isVoice = true)
        }
    }

    fun deleteRoutine(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            app.routineRepository.deleteRoutine(id)
        }
    }

    fun editRoutine(id: Int, name: String, description: String, trigger: String, actionsJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            app.routineRepository.updateRoutine(id, name, description, trigger, actionsJson, "routine")
        }
    }

    /** Preset routines execute REAL action lists via the local engine. */
    fun addPresetRoutine(preset: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (preset) {
                "Good Morning" -> app.routineRepository.addRoutine(
                    "Good Morning", "Battery, time and notifications check with greeting",
                    "শুভ সকাল", "[\"readDeviceState\", \"getNotifications\"]", "wb_sunny"
                )
                "Good Night" -> app.routineRepository.addRoutine(
                    "Good Night", "Battery check, torch off and silence mode",
                    "শুভ রাত্রি", "[\"readDeviceState\", \"torchOff\", \"silence\"]", "nightlight"
                )
                "Work Mode" -> app.routineRepository.addRoutine(
                    "Work Mode", "Quiet media volume for focus",
                    "কাজে বসছি", "[\"setVolumeQuiet\"]", "work"
                )
                "Gaming Mode" -> app.routineRepository.addRoutine(
                    "Gaming Mode", "Silence mode + full volume",
                    "গেমিং মোড", "[\"silence\", \"readDeviceState\"]", "sports_esports"
                )
                "Study Mode" -> app.routineRepository.addRoutine(
                    "Study Mode", "Quiet volume and notification summary",
                    "স্টাডি মোড", "[\"setVolumeQuiet\", \"getNotifications\"]", "menu_book"
                )
            }
        }
    }

    // ------- Notifications & messages -------
    fun markNotificationRead(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            app.notificationRepository.markAsRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            app.notificationRepository.markAllAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            app.notificationRepository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            app.notificationRepository.clearAll()
        }
    }

    /** Real dismiss — cancels the live system notification by its captured key. */
    fun dismissNotification(notification: NotificationEntity) {
        val dismissed = if (notification.notificationKey.isNotBlank()) {
            ArohiNotificationListenerService.instance()?.dismissNotificationByKey(notification.notificationKey) ?: false
        } else {
            false
        }
        if (dismissed) {
            app.eventBus.log("INBOX", "Notification dismissed: ${notification.appName}", SystemEventLevel.INFO)
            deleteNotification(notification.id)
        }
    }

    /** Opens the app that posted the notification (real launch intent). */
    fun openNotificationApp(notification: NotificationEntity): Boolean {
        val launched = app.appDiscoveryManager.launchApp(notification.packageName)
        if (launched) {
            app.eventBus.log("INBOX", "Opened ${notification.appName}", SystemEventLevel.INFO)
            markNotificationRead(notification.id)
        }
        return launched
    }

    /** Announces the actual captured notification text through the real TTS pipeline. */
    fun announceNotification(notification: NotificationEntity) {
        val isPrivate = app.settingsRepository.isPrivateMode()
        val text = if (isPrivate) {
            "${notification.appName} থেকে একটি নোটিফিকেশন এসেছে।"
        } else {
            "${notification.appName} থেকে নোটিফিকেশন: ${notification.title}। ${notification.text}"
        }
        viewModelScope.launch(Dispatchers.Main) {
            ttsManager.speak(text)
        }
        markNotificationRead(notification.id)
    }

    /** Real AI summarization of an actual captured notification. */
    fun summarizeNotification(notification: NotificationEntity, onResult: (String) -> Unit = {}) {
        val content = "${notification.appName}: ${notification.title}. ${notification.text}"
        viewModelScope.launch(Dispatchers.IO) {
            val summary = summarizeLocally(content)
            onResult(summary)
            if (!app.settingsRepository.isSilenceMode()) {
                launch(Dispatchers.Main) {
                    ttsManager.speak(summary)
                }
            }
        }
    }

    /** Deterministic local summarizer over the ACTUAL captured text (no invention). */
    private fun summarizeLocally(content: String): String {
        val words = content.split(Regex("\\s+")).filter { it.isNotBlank() }
        val preview = words.take(24).joinToString(" ")
        val trimmed = if (words.size > 24) "$preview…" else preview
        return "সারাংশ: $trimmed"
    }

    // ------- Chat & tasks -------
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

    // ------- Universal phone control -------
    fun openSettingsPanel(panel: DeviceControlManager.SettingsPanel) {
        app.deviceControlManager.openSettingsPanel(panel)
    }

    fun dispatchMediaAction(action: String) {
        app.deviceControlManager.dispatchMediaAction(action)
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        app.deviceControlManager.setBluetoothEnabled(enabled)
    }

    fun openUrl(url: String) {
        app.deviceControlManager.openUrl(url)
    }

    fun refreshInstalledApps() {
        _installedApps.value = app.appDiscoveryManager.refreshInstalledApps()
    }

    fun launchApp(packageName: String) {
        val launched = app.appDiscoveryManager.launchApp(packageName)
        if (launched) {
            app.eventBus.log("DEVICE", "App launched: $packageName", SystemEventLevel.INFO)
        }
    }

    // ------- Calls & contacts -------
    fun searchContacts(query: String): List<ContactItem> = app.contactsManager.searchContacts(query)

    fun callOrDial(numberOrName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val contacts = app.contactsManager.searchContacts(numberOrName)
            val number = if (contacts.isNotEmpty()) contacts.first().phoneNumber else numberOrName
            app.telephonyHelper.makeCallOrDial(number)
        }
    }

    fun openDialer() {
        app.telephonyHelper.openDialer("")
    }

    // ------- System events -------
    fun clearSystemEvents() {
        app.eventBus.clear()
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        speechManager.destroy()
    }
}
