package com.example.engine

import com.example.data.repository.MemoryRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RoutineRepository
import com.example.data.repository.SettingsRepository
import com.example.device.AppDiscoveryManager
import com.example.device.ContactsManager
import com.example.device.DeviceStateManager
import com.example.device.TelephonyHelper
import com.example.service.ArohiAccessibilityService
import com.example.service.ArohiNotificationListenerService
import java.util.Locale

data class LocalExecutionResult(
    val isHandled: Boolean,
    val responseText: String,
    val emotion: ArohiEmotion = ArohiEmotion.SPEAKING,
    val toolName: String? = null
)

class LocalCommandEngine(
    private val deviceStateManager: DeviceStateManager,
    private val appDiscoveryManager: AppDiscoveryManager,
    private val contactsManager: ContactsManager,
    private val telephonyHelper: TelephonyHelper,
    private val memoryRepository: MemoryRepository,
    private val notificationRepository: NotificationRepository,
    private val routineRepository: RoutineRepository,
    private val settingsRepository: SettingsRepository,
    private val verificationEngine: VerificationEngine
) {

    suspend fun tryExecuteLocally(input: String): LocalExecutionResult {
        val query = input.trim()
        val lower = query.lowercase(Locale.ROOT)

        // 1. Silence / Stop / Interrupt command (strict match only — must not
        // hijack device commands like "টর্চ বন্ধ করো" or "stop the music")
        if (isSilenceCommand(lower)) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = "আচ্ছা বস, চুপ থাকছি। যখন ডাকবেন তখন আসব।",
                emotion = ArohiEmotion.CALM,
                toolName = "silence"
            )
        }

        // 2. Identity / Creator
        if (isIdentityQuery(lower)) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = "আমি আরোহী (Arohi), Shù Vrô-র তৈরি আপনার ব্যক্তিগত AI অপারেটিং লেয়ার। আমি আপনার ফোনের সর্বোচ্চ নিয়ন্ত্রণ ও বুদ্ধিমত্তা নিয়ে প্রস্তুত!",
                emotion = ArohiEmotion.HAPPY,
                toolName = "identity"
            )
        }

        // 2.5 Real long-term memory: save ("মনে রেখো ...") and recall
        // ("কী মনে আছে?") — backed by the actual Room database.
        if (CommandMatchers.isMemorySaveQuery(lower)) {
            return handleMemorySave(query)
        }
        if (CommandMatchers.isMemoryRecallQuery(lower)) {
            return handleMemoryRecall()
        }

        // 3. Battery status
        if (lower.contains("ব্যাটারি") || lower.contains("চার্জ") || lower.contains("battery")) {
            val (percent, isCharging, chargeType) = deviceStateManager.getBatteryInfo()
            val chargeStatus = if (isCharging) " এবং এটি $chargeType দিয়ে চার্জ হচ্ছে" else " (চার্জে নেই)"
            val speech = "আপনার ফোনের ব্যাটারি বর্তমানে $percent%$chargeStatus।"
            return LocalExecutionResult(
                isHandled = true,
                responseText = speech,
                emotion = if (percent < 20 && !isCharging) ArohiEmotion.CONCERNED else ArohiEmotion.SPEAKING,
                toolName = "read_device_telemetry"
            )
        }

        // 4. Storage & RAM status
        if (lower.contains("র‍্যাম") || lower.contains("স্টোরেজ") || lower.contains("মেমোরি") || lower.contains("ram") || lower.contains("storage")) {
            val (freeRam, totalRam) = deviceStateManager.getRamInfo()
            val (freeStorage, totalStorage) = deviceStateManager.getStorageInfo()
            val speech = "বর্তমানে $totalRam MB র‍্যামের মধ্যে $freeRam MB খালি আছে, এবং $totalStorage GB স্টোরেজের মধ্যে $freeStorage GB ফাঁকা রয়েছে।"
            return LocalExecutionResult(
                isHandled = true,
                responseText = speech,
                emotion = ArohiEmotion.FOCUSED,
                toolName = "read_device_telemetry"
            )
        }

        // 5. Flashlight / Torch
        if (lower.contains("টর্চ") || lower.contains("ফ্ল্যাশ") || lower.contains("flashlight") || lower.contains("torch")) {
            val enable = !lower.contains("বন্ধ") && !lower.contains("অফ") && !lower.contains("off")
            val success = deviceStateManager.toggleFlashlight(enable)
            val verify = verificationEngine.verifyTorch(success, enable)
            return LocalExecutionResult(
                isHandled = true,
                responseText = verify.summary,
                emotion = if (success) ArohiEmotion.HAPPY else ArohiEmotion.ERROR,
                toolName = "toggle_flashlight"
            )
        }

        // 6. Volume Control
        if (lower.contains("ভলিউম") || lower.contains("সাউন্ড") || lower.contains("volume") || lower.contains("sound")) {
            val percent = when {
                lower.contains("মিউট") || lower.contains("mute") || lower.contains("শূন্য") || lower.contains("0") -> 0
                lower.contains("ফুল") || lower.contains("সর্বোচ্চ") || lower.contains("100") || lower.contains("max") -> 100
                lower.contains("অর্ধেক") || lower.contains("৫০") || lower.contains("50") -> 50
                lower.contains("বাড়াও") || lower.contains("up") -> {
                    val (current, _) = deviceStateManager.getVolumeInfo()
                    (current + 25).coerceAtMost(100)
                }
                lower.contains("কমাও") || lower.contains("down") -> {
                    val (current, _) = deviceStateManager.getVolumeInfo()
                    (current - 25).coerceAtLeast(0)
                }
                else -> 70
            }
            val success = deviceStateManager.setMediaVolume(percent)
            val verify = verificationEngine.verifyVolume(success, percent)
            return LocalExecutionResult(
                isHandled = true,
                responseText = verify.summary,
                emotion = ArohiEmotion.SPEAKING,
                toolName = "set_media_volume"
            )
        }

        // 7. Global Navigation Gestures
        val accessService = ArohiAccessibilityService.instance
        if (accessService != null) {
            if (lower.contains("পিছনে যাও") || lower.contains("ব্যাক করো") || lower.contains("go back") || lower.contains("back")) {
                accessService.goBack()
                return LocalExecutionResult(
                    isHandled = true,
                    responseText = "পিছনে যাওয়া হয়েছে।",
                    emotion = ArohiEmotion.CALM,
                    toolName = "navigate_global"
                )
            }
            if (lower.contains("হোমে যাও") || lower.contains("হোম স্ক্রিন") || lower.contains("go home")) {
                accessService.goHome()
                return LocalExecutionResult(
                    isHandled = true,
                    responseText = "হোম স্ক্রিনে যাওয়া হয়েছে।",
                    emotion = ArohiEmotion.CALM,
                    toolName = "navigate_global"
                )
            }
            if (lower.contains("নোটিফিকেশন বার") || lower.contains("নোটিফিকেশন নামাও") || lower.contains("open notifications")) {
                accessService.openNotifications()
                return LocalExecutionResult(
                    isHandled = true,
                    responseText = "নোটিফিকেশন প্যানেল খোলা হয়েছে।",
                    emotion = ArohiEmotion.CALM,
                    toolName = "navigate_global"
                )
            }
            if (lower.contains("স্ক্রিন পড়ো") || lower.contains("স্ক্রিনে কি আছে") || lower.contains("read screen")) {
                val content = accessService.inspectCurrentScreen()
                return LocalExecutionResult(
                    isHandled = true,
                    responseText = "স্ক্রিনে যা দেখা যাচ্ছে:\n$content",
                    emotion = ArohiEmotion.FOCUSED,
                    toolName = "inspect_screen"
                )
            }
        }

        // 8. Call Contact / Dial
        if (lower.contains("কল দাও") || lower.contains("কল করো") || lower.startsWith("call ") || lower.contains("ডায়াল করো") || lower.contains("ডায়াল করো")) {
            val target = extractCallTarget(query)
            if (target.isNotBlank()) {
                val contacts = contactsManager.searchContacts(target)
                val targetNumber = if (contacts.isNotEmpty()) contacts.first().phoneNumber else target
                val targetDisplayName = if (contacts.isNotEmpty()) contacts.first().name else target

                val hasCallPermission = telephonyHelper.hasCallPermission()
                val success = telephonyHelper.makeCallOrDial(targetNumber)
                val verify = verificationEngine.verifyCall(success, targetDisplayName, hasCallPermission)
                return LocalExecutionResult(
                    isHandled = true,
                    responseText = verify.summary,
                    emotion = if (success) ArohiEmotion.HAPPY else ArohiEmotion.ERROR,
                    toolName = "make_phone_call"
                )
            }
        }

        // 9. Open Installed Apps
        if (isOpenAppQuery(lower)) {
            val appQuery = extractAppNameFromQuery(query)
            val foundApp = appDiscoveryManager.findApp(appQuery)
            if (foundApp != null) {
                val launched = appDiscoveryManager.launchApp(foundApp.packageName)
                val verify = verificationEngine.verifyAppLaunch(launched, foundApp.label)
                return LocalExecutionResult(
                    isHandled = true,
                    responseText = verify.summary,
                    emotion = if (launched) ArohiEmotion.HAPPY else ArohiEmotion.ERROR,
                    toolName = "open_app"
                )
            }
        }

        // 10. Check Trigger for Custom Routines — execute the real actions
        val routine = routineRepository.findByTrigger(query)
        if (routine != null) {
            val summary = executeRoutineActions(routine.actionsJson)
            return LocalExecutionResult(
                isHandled = true,
                responseText = "'${routine.name}' রুটিন সম্পন্ন হয়েছে:\n$summary",
                emotion = ArohiEmotion.EXECUTING,
                toolName = "run_routine"
            )
        }

        return LocalExecutionResult(isHandled = false, responseText = "")
    }

    /**
     * Really executes the actions of a routine (JSON array like
     * ["readDeviceState","getNotifications"]) using live device managers.
     */
    private suspend fun executeRoutineActions(actionsJson: String): String {
        val actions = Regex("\"([a-zA-Z_]+)\"").findAll(actionsJson)
            .map { it.groupValues[1] }
            .toList()
            .ifEmpty { listOf("readDeviceState") }

        val results = mutableListOf<String>()
        for (action in actions) {
            when (action.lowercase(Locale.ROOT)) {
                "readdevicestate" -> {
                    val (percent, isCharging, _) = deviceStateManager.getBatteryInfo()
                    val (freeRam, totalRam) = deviceStateManager.getRamInfo()
                    val (freeStorage, _) = deviceStateManager.getStorageInfo()
                    results.add(
                        "• ব্যাটারি $percent%${if (isCharging) " (চার্জিং)" else ""}, ফ্রি র‍্যাম ${freeRam}/${totalRam}MB, ফ্রি স্টোরেজ $freeStorage GB"
                    )
                }
                "getnotifications" -> {
                    val unread = notificationRepository.getRecentUnread(5)
                    if (unread.isEmpty()) {
                        results.add("• কোনো অপঠিত নোটিফিকেশন নেই")
                    } else {
                        results.add("• অপঠিত নোটিফিকেশন: " + unread.joinToString("; ") { "${it.appName}: ${it.title}" })
                    }
                }
                "setvolumequiet" -> {
                    val ok = deviceStateManager.setMediaVolume(20)
                    results.add(if (ok) "• মিডিয়া ভলিউম শান্ত মোডে (20%) সেট হয়েছে" else "• ভলিউম সেট করা যায়নি")
                }
                "silence" -> {
                    settingsRepository.setSilenceMode(true)
                    results.add("• সাইলেন্স মোড চালু হয়েছে — এখন থেকে AROHI চুপ থাকবে")
                }
                "torchoff" -> {
                    val ok = deviceStateManager.toggleFlashlight(false)
                    results.add(if (ok) "• টর্চ বন্ধ করা হয়েছে" else "• টর্চ বন্ধ করা যায়নি")
                }
                "torchon" -> {
                    val ok = deviceStateManager.toggleFlashlight(true)
                    results.add(if (ok) "• টর্চ চালু করা হয়েছে" else "• টর্চ চালু করা যায়নি")
                }
                "diagnostics" -> {
                    val isAccess = ArohiAccessibilityService.isServiceRunning()
                    val isNotif = ArohiNotificationListenerService.isConnected
                    val (percent, isCharging, _) = deviceStateManager.getBatteryInfo()
                    results.add(
                        "• ডায়াগনস্টিকস: অ্যাকসেসিবিলিটি ${if (isAccess) "সক্রিয়" else "নিষ্ক্রিয়"}, নোটিফিকেশন লিসেনার ${if (isNotif) "সংযুক্ত" else "বিচ্ছিন্ন"}, ব্যাটারি $percent%"
                    )
                }
                else -> results.add("• অজানা অ্যাকশন বাদ দেওয়া হয়েছে: $action")
            }
        }
        return results.joinToString("\n")
    }

    private fun isSilenceCommand(text: String): Boolean {
        return CommandMatchers.isSilenceCommand(text)
    }

    /**
     * Really saves a fact to the Room memories table and verifies the insert
     * by the returned row id. Returns a real success/failure message.
     */
    private suspend fun handleMemorySave(query: String): LocalExecutionResult {
        val fact = CommandMatchers.extractMemoryFact(query)
        if (fact.isBlank()) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = "কী বিষয়টি মনে রাখব বস, বলুন না! যেমন: \"মনে রেখো আমার পছন্দের রং নীল।\"",
                emotion = ArohiEmotion.CONFUSED,
                toolName = "save_user_memory"
            )
        }
        val key = fact.split(Regex("\\s+")).take(4).joinToString(" ")
        val savedId = memoryRepository.saveMemory("IMPORTANT_FACTS", key, fact)
        val verify = verificationEngine.verifyMemory(savedId, key)
        return LocalExecutionResult(
            isHandled = true,
            responseText = verify.summary + " (মনে রেখেছি: $fact)",
            emotion = if (savedId > 0) ArohiEmotion.HAPPY else ArohiEmotion.ERROR,
            toolName = "save_user_memory"
        )
    }

    /** Really reads the memories table and reports what is actually stored. */
    private suspend fun handleMemoryRecall(): LocalExecutionResult {
        val all = memoryRepository.search("")
        val response = if (all.isEmpty()) {
            "এখনো আমার মেমোরিতে কিছু সংরক্ষিত নেই বস। \"মনে রেখো ...\" বলে যা খুশি মনে রাখতে পারি।"
        } else {
            val items = all.take(15).joinToString("\n") { "• ${it.key}: ${it.value}" }
            "আমার মেমোরিতে এখন ${all.size}টি তথ্য আছে:\n$items"
        }
        return LocalExecutionResult(
            isHandled = true,
            responseText = response,
            emotion = if (all.isEmpty()) ArohiEmotion.CALM else ArohiEmotion.FOCUSED,
            toolName = "search_memory"
        )
    }

    private fun isIdentityQuery(text: String): Boolean {
        val triggers = listOf("তুমি কে", "তোমার নাম কি", "তোমার পরিচয়", "who are you", "who made you", "কার তৈরি", "who created you")
        return triggers.any { text.contains(it) }
    }

    private fun isOpenAppQuery(text: String): Boolean {
        val triggers = listOf("খোলো", "ওপেন করো", "চালু করো", "open", "launch", "start")
        return triggers.any { text.contains(it) }
    }

    private fun extractAppNameFromQuery(text: String): String {
        return text
            .replace(Regex("(?i)(খোলো|খুলুন|ওপেন করো|ওপেন কর|চালু করো|অ্যাপটি|open|launch|the app|app)"), "")
            .trim()
    }

    private fun extractCallTarget(text: String): String {
        return text
            .replace(Regex("(?i)(কল দাও|কল করো|ডায়াল করো|ডায়াল করো|call|dial|to)"), "")
            .replace(Regex("(?i)(কে|-কে)"), "")
            .trim()
    }
}
