package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ArohiApplication
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.RoutineEntity
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiConnectionState
import com.example.data.remote.InlineData
import com.example.device.DeviceTelemetry
import com.example.engine.ArohiEmotion
import com.example.service.ArohiAccessibilityService
import com.example.service.ArohiBackgroundService
import com.example.service.ArohiNotificationListenerService
import com.example.service.DiagnosticCategory
import com.example.service.DiagnosticItem
import com.example.service.DiagnosticReport
import com.example.service.DiagnosticService
import com.example.service.DiagnosticStatusLevel
import com.example.voice.SpeechRecognitionManager
import com.example.voice.SpeechState
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
            while (true) {
                _telemetry.value = app.deviceStateManager.getTelemetry()
                refreshDiagnostics()
                delay(3000)
            }
        }

        // Apply TTS preferences
        ttsManager.setVoicePitch(app.settingsRepository.getVoicePitch())
        ttsManager.setVoiceSpeed(app.settingsRepository.getVoiceSpeed())
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

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        speechManager.destroy()
    }
}
