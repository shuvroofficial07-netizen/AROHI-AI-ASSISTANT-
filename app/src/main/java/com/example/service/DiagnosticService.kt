package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiConnectionState
import com.example.data.repository.SettingsRepository
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
    private val settingsRepository: SettingsRepository
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

    // Full Diagnostic Runner
    suspend fun runFullDiagnostics(): DiagnosticReport = withContext(Dispatchers.Default) {
        _isChecking.value = true
        try {
            val geminiItem = checkGeminiConnection()
            val bgItem = checkBackgroundService()
            val cameraItem = checkCameraAvailability()
            val micItem = checkMicrophone()
            val accessItem = checkAccessibility()
            val notifItem = checkNotificationListener()
            val contactsItem = checkContacts()
            val batteryItem = checkBatteryOptimization()

            val allItems = listOf(
                geminiItem,
                bgItem,
                cameraItem,
                micItem,
                accessItem,
                notifItem,
                contactsItem,
                batteryItem
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
