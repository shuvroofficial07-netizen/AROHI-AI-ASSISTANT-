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
            GeminiConnectionState.MODEL_UNAVAILABLE -> {
                DiagnosticItem(
                    id = "gemini_ai",
                    name = "Gemini Cloud AI ইঞ্জিন",
                    category = DiagnosticCategory.AI_CORE,
                    status = DiagnosticStatusLevel.ERROR,
                    summary = "Model Unavailable",
                    details = "কনফিগার করা মডেলে সংযোগ করা যায়নি: $message",
                    latencyMs = latency,
                    actionText = "Fix Model"
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
        val lastError = ArohiBackgroundService.lastError
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
        } else if (lastError != null) {
            // The service really failed to start — surface the genuine reason.
            DiagnosticItem(
                id = "background_service",
                name = "Arohi Background Operating Layer",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.ERROR,
                summary = "Start Failed",
                details = "সার্ভিস চালু করা যায়নি। আসল কারণ: $lastError",
                actionText = "Retry"
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

    // 2.1 Real network / internet connectivity check (Wi-Fi / mobile data /
    // offline) — read live from ConnectivityManager, never assumed.
    fun checkNetworkConnectivity(): DiagnosticItem {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val network = cm?.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }

        return if (caps == null) {
            DiagnosticItem(
                id = "network_connectivity",
                name = "নেটওয়ার্ক ও ইন্টারনেট সংযোগ",
                category = DiagnosticCategory.HARDWARE,
                status = DiagnosticStatusLevel.ERROR,
                summary = "Offline",
                details = "কোনো নেটওয়ার্কে সংযুক্ত নেই। ক্লাউড AI ও ওয়েব ফিচার বন্ধ থাকবে; লোকাল ইঞ্জিন কাজ করবে।",
                actionText = "Settings"
            )
        } else {
            val transport = when {
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Unknown"
            }
            // NET_CAPABILITY_VALIDATED is set by the OS after a REAL probe.
            val validated = caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val internet = caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val (status, summary, extra) = if (internet && validated) {
                Triple(DiagnosticStatusLevel.READY, "Connected ($transport)",
                    "ইন্টারনেট সংযোগ সক্রিয় ও যাচাইকৃত (OS-validated)।")
            } else if (internet) {
                Triple(DiagnosticStatusLevel.LIMITED, "Connected ($transport, unvalidated)",
                    "নেটওয়ার্কে সংযুক্ত আছেন কিন্তু ইন্টারনেট এখনো যাচাই হয়নি — ক্যাপটিভ পোর্টাল বা সীমিত নেটওয়ার্ক হতে পারে।")
            } else {
                Triple(DiagnosticStatusLevel.LIMITED, "No Internet ($transport)",
                    "নেটওয়ার্ক আছে কিন্তু ইন্টারনেট ক্যাপাবিলিটি নেই।")
            }
            DiagnosticItem(
                id = "network_connectivity",
                name = "নেটওয়ার্ক ও ইন্টারনেট সংযোগ",
                category = DiagnosticCategory.HARDWARE,
                status = status,
                summary = summary,
                details = "$extra (সংযোগের ধরন: $transport)",
                actionText = "Settings"
            )
        }
    }

    // 2.2 REAL database check: writes a probe row, reads it back and deletes it.
    suspend fun checkDatabase(): DiagnosticItem = withContext(Dispatchers.IO) {
        val appName = try { context.applicationContext as? com.example.ArohiApplication } catch (e: Exception) { null }
        val database = appName?.database
        if (database == null) {
            return@withContext DiagnosticItem(
                id = "database",
                name = "ডাটাবেস (Room Storage)",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.ERROR,
                summary = "Unavailable",
                details = "Room ডাটাবেস চালু করা যায়নি। মেমোরি/টাস্ক/চ্যাট সংরক্ষণ বন্ধ থাকবে।",
                actionText = null
            )
        }
        try {
            val probeKey = "__diag_probe__"
            val memoryDao = database.memoryDao()
            val inserted = memoryDao.insertMemory(
                com.example.data.local.entity.MemoryEntity(
                    category = "DIAGNOSTICS",
                    key = probeKey,
                    value = "probe-${System.currentTimeMillis()}"
                )
            )
            val readBack = memoryDao.getMemoryByKey(probeKey)
            memoryDao.deleteMemoryByKey(probeKey)
            if (inserted > 0 && readBack != null) {
                DiagnosticItem(
                    id = "database",
                    name = "ডাটাবেস (Room Storage)",
                    category = DiagnosticCategory.BACKGROUND_LAYER,
                    status = DiagnosticStatusLevel.READY,
                    summary = "Read/Write OK",
                    details = "সত্যিকারের ডাটাবেস পরীক্ষা সফল: লেখা → পড়া → মুছে ফেলা (probe row #${readBack.id}) সবই কাজ করেছে।",
                    actionText = null
                )
            } else {
                DiagnosticItem(
                    id = "database",
                    name = "ডাটাবেস (Room Storage)",
                    category = DiagnosticCategory.BACKGROUND_LAYER,
                    status = DiagnosticStatusLevel.ERROR,
                    summary = "Verification Failed",
                    details = "ডাটাবেস লেখা হয়েছে কিন্তু পড়ে যাচাই করা যায়নি।",
                    actionText = null
                )
            }
        } catch (e: Exception) {
            DiagnosticItem(
                id = "database",
                name = "ডাটাবেস (Room Storage)",
                category = DiagnosticCategory.BACKGROUND_LAYER,
                status = DiagnosticStatusLevel.ERROR,
                summary = "Error",
                details = "ডাটাবেস অপারেশন ব্যর্থ: ${e.javaClass.simpleName}: ${e.localizedMessage ?: "unknown"}",
                actionText = null
            )
        }
    }

    // 2.3 REAL storage check: live free space from StatFs on app storage.
    fun checkStorage(): DiagnosticItem {
        return try {
            val filesDir = context.filesDir
            val stat = android.os.StatFs(filesDir.absolutePath)
            val freeGb = stat.availableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val totalGb = stat.totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val writable = try {
                val probe = java.io.File(filesDir, ".arohi_write_probe")
                probe.writeText("ok")
                val content = probe.readText()
                probe.delete()
                content == "ok"
            } catch (e: Exception) {
                false
            }
            when {
                !writable -> DiagnosticItem(
                    id = "storage",
                    name = "স্টোরেজ ও ফাইল অ্যাক্সেস",
                    category = DiagnosticCategory.HARDWARE,
                    status = DiagnosticStatusLevel.ERROR,
                    summary = "Not Writable",
                    details = "অ্যাপ স্টোরেজে লেখা যাচ্ছে না — ডাটা সংরক্ষণ ব্যর্থ হবে।",
                    actionText = null
                )
                freeGb < 0.2 -> DiagnosticItem(
                    id = "storage",
                    name = "স্টোরেজ ও ফাইল অ্যাক্সেস",
                    category = DiagnosticCategory.HARDWARE,
                    status = DiagnosticStatusLevel.LIMITED,
                    summary = "Low Space (${String.format("%.1f", freeGb)} GB free)",
                    details = "ফ্রি স্টোরেজ খুব কম (${String.format("%.1f", freeGb)} GB / ${String.format("%.1f", totalGb)} GB)। কিছু ফিচার সীমিত হতে পারে।",
                    actionText = "Settings"
                )
                else -> DiagnosticItem(
                    id = "storage",
                    name = "স্টোরেজ ও ফাইল অ্যাক্সেস",
                    category = DiagnosticCategory.HARDWARE,
                    status = DiagnosticStatusLevel.READY,
                    summary = "OK (${String.format("%.1f", freeGb)} GB free)",
                    details = "অ্যাপ স্টোরেজ লেখার যোগ্য এবং পর্যাপ্ত জায়গা আছে (মোট ${String.format("%.1f", totalGb)} GB)।",
                    actionText = null
                )
            }
        } catch (e: Exception) {
            DiagnosticItem(
                id = "storage",
                name = "স্টোরেজ ও ফাইল অ্যাক্সেস",
                category = DiagnosticCategory.HARDWARE,
                status = DiagnosticStatusLevel.ERROR,
                summary = "Error",
                details = "স্টোরেজ পরীক্ষা ব্যর্থ: ${e.javaClass.simpleName}",
                actionText = null
            )
        }
    }

    // 2.4 REAL TTS engine check: query the actual installed TTS services.
    fun checkTtsEngine(): DiagnosticItem {
        return try {
            val intent = android.content.Intent("android.intent.action.TTS_SERVICE")
            val services = context.packageManager.queryIntentServices(intent, 0)
            if (services.isNullOrEmpty()) {
                DiagnosticItem(
                    id = "tts_engine",
                    name = "টেক্সট-টু-স্পিচ (TTS) ইঞ্জিন",
                    category = DiagnosticCategory.HARDWARE,
                    status = DiagnosticStatusLevel.ERROR,
                    summary = "No TTS Engine",
                    details = "ডিভাইসে কোনো TTS ইঞ্জিন ইনস্টল নেই — আরোহী কথা বলতে পারবে না। Google TTS ইনস্টল করুন।",
                    actionText = "Settings"
                )
            } else {
                val engineNames = services.mapNotNull { it.serviceInfo?.applicationInfo?.let { ai ->
                    try { context.packageManager.getApplicationLabel(ai).toString() } catch (e: Exception) { null }
                } }.distinct()
                DiagnosticItem(
                    id = "tts_engine",
                    name = "টেক্সট-টু-স্পিচ (TTS) ইঞ্জিন",
                    category = DiagnosticCategory.HARDWARE,
                    status = DiagnosticStatusLevel.READY,
                    summary = "Available (${engineNames.size} engine${if (engineNames.size > 1) "s" else ""})",
                    details = "ইনস্টল করা TTS ইঞ্জিন: ${engineNames.joinToString(", ").ifEmpty { "system" }}। বাংলা ভয়েস ডাউনলোড করলে উচ্চারণ আরও ভালো হবে।",
                    actionText = "Settings"
                )
            }
        } catch (e: Exception) {
            DiagnosticItem(
                id = "tts_engine",
                name = "টেক্সট-টু-স্পিচ (TTS) ইঞ্জিন",
                category = DiagnosticCategory.HARDWARE,
                status = DiagnosticStatusLevel.LIMITED,
                summary = "Check Failed",
                details = "TTS ইঞ্জিন যাচাই করা যায়নি: ${e.javaClass.simpleName}",
                actionText = null
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
            val networkItem = checkNetworkConnectivity()
            val databaseItem = checkDatabase()
            val storageItem = checkStorage()
            val ttsItem = checkTtsEngine()
            val bgItem = checkBackgroundService()
            val cameraItem = checkCameraAvailability()
            val micItem = checkMicrophone()
            val accessItem = checkAccessibility()
            val notifItem = checkNotificationListener()
            val contactsItem = checkContacts()
            val batteryItem = checkBatteryOptimization()

            val allItems = listOf(
                geminiItem,
                networkItem,
                databaseItem,
                storageItem,
                ttsItem,
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
