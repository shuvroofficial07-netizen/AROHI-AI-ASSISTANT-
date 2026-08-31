package com.example.engine

import com.example.data.repository.MemoryRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RoutineRepository
import com.example.device.AppDiscoveryManager
import com.example.device.ContactsManager
import com.example.device.DeviceStateManager
import com.example.device.TelephonyHelper
import com.example.service.ArohiAccessibilityService
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
    private val verificationEngine: VerificationEngine
) {

    suspend fun tryExecuteLocally(input: String): LocalExecutionResult {
        val query = input.trim()
        val lower = query.lowercase(Locale.ROOT)

        // 1. Silence / Stop / Interrupt command
        if (isSilenceCommand(lower)) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = "ঠিক আছে, আমি চুপ করলাম।",
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

        // 10. Check Trigger for Custom Routines
        val routine = routineRepository.findByTrigger(query)
        if (routine != null) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = "'${routine.name}' রুটিন কার্যকর করা হচ্ছে...",
                emotion = ArohiEmotion.EXECUTING,
                toolName = "run_routine"
            )
        }

        return LocalExecutionResult(isHandled = false, responseText = "")
    }

    private fun isSilenceCommand(text: String): Boolean {
        val triggers = listOf("চুপ", "চুপ করো", "থামো", "বন্ধ করো", "stop", "quiet", "shut up", "be quiet", "ব্যাস")
        return triggers.any { text.contains(it) }
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
