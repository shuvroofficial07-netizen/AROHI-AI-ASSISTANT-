package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiConnectionState
import com.example.data.repository.SettingsRepository
import com.example.device.DeviceStateManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class DiagnosticStatusLevel {
    READY,
    LIMITED,
    ERROR
}

enum class DiagnosticCategory {
    AI_CORE,
    BACKGROUND_LAYER,
    HARDWARE,
    PERMISSIONS_ACCESS
}

data class DiagnosticItem(
    val id: String,
    val name: String,
    val category: DiagnosticCategory,
    val status: DiagnosticStatusLevel,
    val summary: String,
    val details: String,
    val latencyMs: Long? = null,
    val actionText: String? = null,
    val lastCheckedTimestamp: Long = System.currentTimeMillis()
)

data class DiagnosticReport(
    val overallStatus: DiagnosticStatusLevel,
    val readyCount: Int,
    val limitedCount: Int,
    val errorCount: Int,
    val items: List<DiagnosticItem>,
    val timestamp: Long = System.currentTimeMillis()
)

class DiagnosticService(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val deviceStateManager: DeviceStateManager
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val _report = MutableStateFlow(
        DiagnosticReport(
            overallStatus = DiagnosticStatusLevel.LIMITED,
            readyCount = 0,
            limitedCount = 0,
            errorCount = 0,
            items = emptyList()
        )
    )
    val report: StateFlow<DiagnosticReport> = _report.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    // 1. Real-time check on Gemini Connection
    suspend fun checkGeminiConnection(): DiagnosticItem = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.getApiKey()
        val modelName = settingsRepository.getModelName()

        if (apiKey.isBlank()) {
            return@withContext DiagnosticItem(
                id = "gemini_ai",
                name = "Gemini Cloud AI ইঞ্জিন",
                category = DiagnosticCategory.AI_CORE,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "API Key Not Set",
                details = "Gemini API key is not configured. Local command & offline intelligence active.",
                actionText = "Configure Key"
            )
        }

        val startTime = System.currentTimeMillis()
        val (state, message) = GeminiClient.testConnection(apiKey, modelName)
        val latency = System.currentTimeMillis() - startTime

        when (state) {
            GeminiConnectionState.CONNECTED -> {
                DiagnosticItem(
                    id = "gemini_ai",
                    name = "Gemini Cloud AI ইঞ্জিন",
                    category = DiagnosticCategory.AI_CORE,
                    status = DiagnosticStatusLevel.READY,
                    summary = "Connected (${latency}ms)",
                    details = "Active connection to $modelName. Multimodal and reasoning features operational.",
                    latencyMs = latency,
                    actionText = "Test Ping"
                )
            }
            GeminiConnectionState.RATE_LIMITED -> {
                DiagnosticItem(
                    id = "gemini_ai",
                    name = "Gemini Cloud AI ইঞ্জিন",
                    category = DiagnosticCategory.AI_CORE,
                    status = DiagnosticStatusLevel.LIMITED,
                    summary = "Rate Limited (429)",
                    details = "Quota limit reached. Automatically using local rule engine until quota resets.",
                    latencyMs = latency,
                    actionText = "Retry"
                )
            }
            GeminiConnectionState.INVALID_KEY -> {
                DiagnosticItem(
                    id = "gemini_ai",
                    name = "Gemini Cloud AI ইঞ্জিন",
                    category = DiagnosticCategory.AI_CORE,
                    status = DiagnosticStatusLevel.ERROR,
                    summary = "Invalid API Key",
                    details = "The configured Gemini API key is invalid or unauthorized: $message",
                    actionText = "Fix Key"
                )
            }
            else -> {
                DiagnosticItem(
                    id = "gemini_ai",
                    name = "Gemini Cloud AI ইঞ্জিন",
                    category = DiagnosticCategory.AI_CORE,
                    status = DiagnosticStatusLevel.ERROR,
                    summary = "Connection Failed",
                    details = "Unable to reach Gemini endpoints: $message",
                    latencyMs = latency,
                    actionText = "Retry"
                )
            }
        }
    }

    // 2. Real-time check on Background Service State
    fun checkBackgroundService(): DiagnosticItem {
        val isRunning = ArohiBackgroundService.isRunning
        return if (isRunning) {
            DiagnosticItem(
                id = "background_service",
                name = "Arohi Background Operating Layer",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.READY,
                summary = "Foreground Active",
                details = "Foreground service is running with persistent notification. 24/7 assistant capabilities enabled.",
                actionText = "Stop"
            )
        } else {
            DiagnosticItem(
                id = "background_service",
                name = "Arohi Background Operating Layer",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Service Stopped",
                details = "Background assistant is inactive. Arohi will pause when app is closed.",
                actionText = "Start"
            )
        }
    }

    // 3. Real-time check on Camera Availability
    fun checkCameraAvailability(): DiagnosticItem {
        val cameraIds = try {
            cameraManager?.cameraIdList ?: emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }

        if (cameraIds.isEmpty()) {
            return DiagnosticItem(
                id = "camera_vision",
                name = "Camera & Vision AI হার্ডওয়্যার",
                category = DiagnosticCategory.HARDWARE,
                status = DiagnosticStatusLevel.ERROR,
                summary = "No Camera Hardware",
                details = "No camera sensor found on this device. Vision AI is unavailable.",
                actionText = null
            )
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return DiagnosticItem(
                id = "camera_vision",
                name = "Camera & Vision AI হার্ডওয়্যার",
                category = DiagnosticCategory.HARDWARE,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Permission Required",
                details = "${cameraIds.size} Camera(s) available on device, but Camera permission is not granted.",
                actionText = "Grant"
            )
        }

        var backCam = false
        var frontCam = false
        try {
            cameraIds.forEach { id ->
                val char = cameraManager?.getCameraCharacteristics(id)
                val facing = char?.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) backCam = true
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) frontCam = true
            }
        } catch (e: Exception) {
            // Ignored
        }

        val facingSummary = buildString {
            if (backCam) append("Rear Camera")
            if (frontCam) {
                if (isNotEmpty()) append(" + ")
                append("Front Camera")
            }
        }.ifEmpty { "${cameraIds.size} Camera(s)" }

        return DiagnosticItem(
            id = "camera_vision",
            name = "Camera & Vision AI হার্ডওয়্যার",
            category = DiagnosticCategory.HARDWARE,
            status = DiagnosticStatusLevel.READY,
            summary = "Ready ($facingSummary)",
            details = "Camera hardware ($facingSummary) and Vision AI capture pipeline are fully operational.",
            actionText = "Open Vision"
        )
    }

    // 4. Check Accessibility Service
    fun checkAccessibility(): DiagnosticItem {
        val isGranted = ArohiAccessibilityService.isAccessibilityPermissionGranted(context)
        return if (isGranted) {
            DiagnosticItem(
                id = "accessibility",
                name = "Accessibility Controller অটোমেশন",
                category = DiagnosticCategory.PERMISSIONS_ACCESS,
                status = DiagnosticStatusLevel.READY,
                summary = "Enabled",
                details = "Accessibility node tree inspection and voice-directed UI automation active.",
                actionText = "Settings"
            )
        } else {
            DiagnosticItem(
                id = "accessibility",
                name = "Accessibility Controller অটোমেশন",
                category = DiagnosticCategory.PERMISSIONS_ACCESS,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Disabled in Settings",
                details = "Accessibility permission needed for Arohi to inspect screens and tap buttons automatically.",
                actionText = "Enable"
            )
        }
    }

    // 5. Check Notification Listener
    fun checkNotificationListener(): DiagnosticItem {
        val isGranted = ArohiNotificationListenerService.isNotificationAccessGranted(context)
        return if (isGranted) {
            DiagnosticItem(
                id = "notification_listener",
                name = "Notification Intelligence লিসেনার",
                category = DiagnosticCategory.PERMISSIONS_ACCESS,
                status = DiagnosticStatusLevel.READY,
                summary = "Access Granted",
                details = "Active notification interception and intelligent AI summarization enabled.",
                actionText = "Settings"
            )
        } else {
            DiagnosticItem(
                id = "notification_listener",
                name = "Notification Intelligence লিসেনার",
                category = DiagnosticCategory.PERMISSIONS_ACCESS,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Access Required",
                details = "Notification access permission required to read and announce incoming alerts.",
                actionText = "Grant"
            )
        }
    }

    // 6. Check Microphone
    fun checkMicrophone(): DiagnosticItem {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasPermission) {
            DiagnosticItem(
                id = "microphone",
                name = "ভয়েস ইঞ্জিন ও মাইক্রোফোন (STT)",
                category = DiagnosticCategory.HARDWARE,
                status = DiagnosticStatusLevel.READY,
                summary = "Mic Active",
                details = "Microphone hardware and speech recognition manager are ready for voice input.",
                actionText = "Settings"
            )
        } else {
            DiagnosticItem(
                id = "microphone",
                name = "ভয়েস ইঞ্জিন ও মাইক্রোফোন (STT)",
                category = DiagnosticCategory.HARDWARE,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Permission Required",
                details = "Microphone permission required for real-time speech recognition and wake commands.",
                actionText = "Grant"
            )
        }
    }

    // 7. Check Contacts
    fun checkContacts(): DiagnosticItem {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasPermission) {
            DiagnosticItem(
                id = "contacts",
                name = "কন্টাক্টস ও ডায়ালিং সার্ভিস",
                category = DiagnosticCategory.PERMISSIONS_ACCESS,
                status = DiagnosticStatusLevel.READY,
                summary = "Contacts Ready",
                details = "Direct contact name matching and phone calling pipeline operational.",
                actionText = "Settings"
            )
        } else {
            DiagnosticItem(
                id = "contacts",
                name = "কন্টাক্টস ও ডায়ালিং সার্ভিস",
                category = DiagnosticCategory.PERMISSIONS_ACCESS,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Permission Required",
                details = "Contacts permission needed to resolve names like 'Rahim ko call karo'.",
                actionText = "Grant"
            )
        }
    }

    // 8. Check Battery Optimization Bypass
    fun checkBatteryOptimization(): DiagnosticItem {
        val isIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true

        return if (isIgnoring) {
            DiagnosticItem(
                id = "battery_opt",
                name = "ব্যাটারি অপ্টিমাইজেশন বাইপাস",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.READY,
                summary = "Unrestricted",
                details = "App is exempted from Android power saving limits for uninterrupted background execution.",
                actionText = "Settings"
            )
        } else {
            DiagnosticItem(
                id = "battery_opt",
                name = "ব্যাটারি অপ্টিমাইজেশন বাইপাস",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Optimized (Restricted)",
                details = "Android may kill background processes under memory/battery pressure. Tap to exempt.",
                actionText = "Optimize"
            )
        }
    }

    // 9. Voice output engine (real TTS engine presence on this device)
    fun checkVoiceOutput(): DiagnosticItem {
        return if (TextToSpeechManager.isTtsEngineAvailable(context)) {
            DiagnosticItem(
                id = "voice_output",
                name = "ভয়েস আউটপুট (TTS Engine)",
                category = DiagnosticCategory.HARDWARE,
                status = DiagnosticStatusLevel.READY,
                summary = "Engine Present",
                details = "A text-to-speech engine is installed and available for AROHI voice responses.",
                actionText = "Settings"
            )
        } else {
            DiagnosticItem(
                id = "voice_output",
                name = "ভয়েস আউটপুট (TTS Engine)",
                category = DiagnosticCategory.HARDWARE,
                status = DiagnosticStatusLevel.ERROR,
                summary = "No TTS Engine",
                details = "No text-to-speech engine found. Install Google TTS from Play Store to hear AROHI.",
                actionText = "Settings"
            )
        }
    }

    // 10. Local database (Room)
    fun checkDatabase(): DiagnosticItem {
        val dbReady = try {
            com.example.ArohiApplication.instance.database != null
        } catch (e: Exception) {
            false
        }
        return if (dbReady) {
            DiagnosticItem(
                id = "database",
                name = "লোকাল ডেটাবেস (Room Memory)",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.READY,
                summary = "Ready",
                details = "Room database initialized — memories, messages, routines, tasks and notifications are persisted locally.",
                actionText = null
            )
        } else {
            DiagnosticItem(
                id = "database",
                name = "লোকাল ডেটাবেস (Room Memory)",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.ERROR,
                summary = "Unavailable",
                details = "Local database could not be verified.",
                actionText = null
            )
        }
    }

    // 11. Network reachability (real ConnectivityManager state)
    fun checkNetwork(): DiagnosticItem {
        val (networkType, isConnected) = deviceStateManager.getNetworkInfo()
        return if (isConnected) {
            DiagnosticItem(
                id = "network",
                name = "নেটওয়ার্ক কানেকশন",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.READY,
                summary = networkType,
                details = "Active network connection available for Gemini cloud AI.",
                actionText = null
            )
        } else {
            DiagnosticItem(
                id = "network",
                name = "নেটওয়ার্ক কানেকশন",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.ERROR,
                summary = "Offline",
                details = "No active network. Cloud AI is unavailable; the local command engine still works.",
                actionText = null
            )
        }
    }

    // 12. Storage (real filesystem stats)
    fun checkStorage(): DiagnosticItem {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeGb = (stat.availableBlocksLong * stat.blockSizeLong).toDouble() / (1024.0 * 1024.0 * 1024.0)
            if (freeGb < 0.2) {
                DiagnosticItem(
                    id = "storage",
                    name = "স্টোরেজ স্পেস",
                    category = DiagnosticCategory.HARDWARE,
                    status = DiagnosticStatusLevel.LIMITED,
                    summary = "Low (${"%.2f".format(freeGb)} GB free)",
                    details = "Very little free storage remains — notifications and memory writes may be affected.",
                    actionText = "Storage Settings"
                )
            } else {
                DiagnosticItem(
                    id = "storage",
                    name = "স্টোরেজ স্পেস",
                    category = DiagnosticCategory.HARDWARE,
                    status = DiagnosticStatusLevel.READY,
                    summary = "${"%.1f".format(freeGb)} GB free",
                    details = "Sufficient storage for local memory and notification history.",
                    actionText = null
                )
            }
        } catch (e: Exception) {
            DiagnosticItem(
                id = "storage",
                name = "স্টোরেজ স্পেস",
                category = DiagnosticCategory.HARDWARE,
                status = DiagnosticStatusLevel.ERROR,
                summary = "Unavailable",
                details = "Storage statistics could not be read.",
                actionText = null
            )
        }
    }

    // 13. Overlay (floating indicator) permission
    fun checkOverlay(): DiagnosticItem {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        return if (granted) {
            DiagnosticItem(
                id = "overlay",
                name = "ফ্লোটিং ইন্ডিকেটর (Overlay)",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.READY,
                summary = "Allowed",
                details = "The floating AROHI indicator can appear while the background service runs.",
                actionText = "Settings"
            )
        } else {
            DiagnosticItem(
                id = "overlay",
                name = "ফ্লোটিং ইন্ডিকেটর (Overlay)",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Not Allowed",
                details = "Overlay permission needed to show the small floating AROHI status pill.",
                actionText = "Grant"
            )
        }
    }

    // 14. POST_NOTIFICATIONS (Android 13+) and phone state
    fun checkNotificationPermission(): DiagnosticItem {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) {
            DiagnosticItem(
                id = "notification_permission",
                name = "নোটিফিকেশন পারমিশন (POST)",
                category = DiagnosticCategory.PERMISSIONS_ACCESS,
                status = DiagnosticStatusLevel.READY,
                summary = "Granted",
                details = "AROHI can post its persistent background-service notification.",
                actionText = "Settings"
            )
        } else {
            DiagnosticItem(
                id = "notification_permission",
                name = "নোটিফিকেশন পারমিশন (POST)",
                category = DiagnosticCategory.PERMISSIONS_ACCESS,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Not Granted",
                details = "Android 13+ requires notification permission for the background service notice.",
                actionText = "Grant"
            )
        }
    }

    fun checkPhoneState(): DiagnosticItem {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) {
            DiagnosticItem(
                id = "phone_state",
                name = "কল ইন্টেলিজেন্স (Phone State)",
                category = DiagnosticCategory.PERMISSIONS_ACCESS,
                status = DiagnosticStatusLevel.READY,
                summary = "Granted",
                details = "Incoming call detection and caller announcements are available.",
                actionText = "Settings"
            )
        } else {
            DiagnosticItem(
                id = "phone_state",
                name = "কল ইন্টেলিজেন্স (Phone State)",
                category = DiagnosticCategory.PERMISSIONS_ACCESS,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Permission Required",
                details = "READ_PHONE_STATE needed to detect incoming calls and announce callers.",
                actionText = "Grant"
            )
        }
    }

    // Full Diagnostic Runner
    suspend fun runFullDiagnostics(): DiagnosticReport = withContext(Dispatchers.Default) {
        _isChecking.value = true
        try {
            val geminiItem = checkGeminiConnection()
            val bgItem = checkBackgroundService()
            val cameraItem = checkCameraAvailability()
            val micItem = checkMicrophone()
            val voiceItem = checkVoiceOutput()
            val accessItem = checkAccessibility()
            val notifItem = checkNotificationListener()
            val contactsItem = checkContacts()
            val batteryItem = checkBatteryOptimization()
            val dbItem = checkDatabase()
            val networkItem = checkNetwork()
            val storageItem = checkStorage()
            val overlayItem = checkOverlay()
            val notifPermItem = checkNotificationPermission()
            val phoneItem = checkPhoneState()

            val allItems = listOf(
                geminiItem,
                bgItem,
                cameraItem,
                micItem,
                voiceItem,
                accessItem,
                notifItem,
                contactsItem,
                batteryItem,
                dbItem,
                networkItem,
                storageItem,
                overlayItem,
                notifPermItem,
                phoneItem
            )

            val ready = allItems.count { it.status == DiagnosticStatusLevel.READY }
            val limited = allItems.count { it.status == DiagnosticStatusLevel.LIMITED }
            val error = allItems.count { it.status == DiagnosticStatusLevel.ERROR }

            val overall = when {
                error > 0 -> DiagnosticStatusLevel.ERROR
                limited > 0 -> DiagnosticStatusLevel.LIMITED
                else -> DiagnosticStatusLevel.READY
            }

            val report = DiagnosticReport(
                overallStatus = overall,
                readyCount = ready,
                limitedCount = limited,
                errorCount = error,
                items = allItems
            )
            _report.value = report
            report
        } finally {
            _isChecking.value = false
        }
    }
}
