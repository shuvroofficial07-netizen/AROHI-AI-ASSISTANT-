package com.example.engine

import com.example.data.repository.MemoryRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RoutineRepository
import com.example.data.repository.SettingsRepository
import com.example.device.AppDiscoveryManager
import com.example.device.CallState
import com.example.device.ContactsManager
import com.example.device.DeviceControlManager
import com.example.device.DeviceStateManager
import com.example.device.PhoneStateManager
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
    private val deviceControlManager: DeviceControlManager,
    private val appDiscoveryManager: AppDiscoveryManager,
    private val contactsManager: ContactsManager,
    private val telephonyHelper: TelephonyHelper,
    private val phoneStateManager: PhoneStateManager,
    private val memoryRepository: MemoryRepository,
    private val notificationRepository: NotificationRepository,
    private val routineRepository: RoutineRepository,
    private val settingsRepository: SettingsRepository,
    private val verificationEngine: VerificationEngine,
    private val eventBus: SystemEventBus
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

        // 11. Settings panels — every control calls the real Android subsystem
        val settingsPanel = detectSettingsPanel(lower)
        if (settingsPanel != null) {
            val success = deviceControlManager.openSettingsPanel(settingsPanel)
            eventBus.log("DEVICE", "Settings panel: ${settingsPanel.label}", if (success) SystemEventLevel.INFO else SystemEventLevel.ERROR)
            return LocalExecutionResult(
                isHandled = true,
                responseText = if (success) "${settingsPanel.label} খোলা হয়েছে।" else "${settingsPanel.label} খোলা যায়নি।",
                emotion = if (success) ArohiEmotion.HAPPY else ArohiEmotion.ERROR,
                toolName = "open_settings"
            )
        }

        // 12. Open URL (explicit http/https requests)
        val urlMatch = Regex("(?i)(https?://[\\w\\-./?=&%+#]+)").find(query)
        if (urlMatch != null || (lower.contains("ওয়েবসাইট খোলো") || lower.contains("site খোলো"))) {
            val url = urlMatch?.groupValues?.get(1)
            val success = if (url != null) {
                deviceControlManager.openUrl(url)
            } else {
                deviceControlManager.openUrl("https://www.google.com")
            }
            return LocalExecutionResult(
                isHandled = true,
                responseText = if (success) "ওয়েবসাইটটি খোলা হয়েছে।" else "ওয়েবসাইট খোলা যায়নি।",
                emotion = if (success) ArohiEmotion.HAPPY else ArohiEmotion.ERROR,
                toolName = "open_url"
            )
        }

        // 13. Media playback control (real media key events)
        val mediaAction = detectMediaAction(lower)
        if (mediaAction != null) {
            val success = deviceControlManager.dispatchMediaAction(mediaAction)
            return LocalExecutionResult(
                isHandled = true,
                responseText = if (success) "মিডিয়া কন্ট্রোল ($mediaAction) পাঠানো হয়েছে।" else "মিডিয়া কন্ট্রোল পাঠানো যায়নি।",
                emotion = if (success) ArohiEmotion.HAPPY else ArohiEmotion.ERROR,
                toolName = "media_control"
            )
        }

        // 14. Bluetooth radio (only where the platform allows)
        if (lower.contains("ব্লুটুথ চালু") || lower.contains("bluetooth on") ||
            lower.contains("ব্লুটুথ বন্ধ") || lower.contains("bluetooth off")
        ) {
            val enable = lower.contains("চালু") || lower.contains("on")
            val success = deviceControlManager.setBluetoothEnabled(enable)
            return LocalExecutionResult(
                isHandled = true,
                responseText = if (success) "ব্লুটুথ ${if (enable) "চালু" else "বন্ধ"} করার নির্দেশ পাঠানো হয়েছে।" else "ব্লুটুথ নিয়ন্ত্রণ করা যায়নি — Android এটি অনুমোদন করছে না।",
                emotion = if (success) ArohiEmotion.HAPPY else ArohiEmotion.ERROR,
                toolName = "toggle_bluetooth"
            )
        }

        // 15. Current call state (real TelephonyManager data)
        if (lower.contains("কল স্টেট") || lower.contains("কে কল") || lower.contains("call state") ||
            lower.contains("কে ফোন") || lower.contains("who is calling")
        ) {
            val call = phoneStateManager.callInfo.value
            val text = when (call.state) {
                CallState.RINGING ->
                    if (call.callerName.isNotBlank()) "${call.callerName} কল করছে (${call.incomingNumber})।"
                    else "অজানা নম্বর (${call.incomingNumber}) থেকে কল আসছে।"
                CallState.OFFHOOK -> "বর্তমানে একটি কল চলছে।"
                CallState.IDLE -> "বর্তমানে কোনো কল নেই।"
                else -> "কল স্টেট পড়া যাচ্ছে না (READ_PHONE_STATE পারমিশন প্রয়োজন)।"
            }
            return LocalExecutionResult(
                isHandled = true,
                responseText = text,
                emotion = if (call.state == CallState.RINGING) ArohiEmotion.CONCERNED else ArohiEmotion.SPEAKING,
                toolName = "check_call_state"
            )
        }

        // 16. Current foreground app (real UsageStats when granted)
        if (lower.contains("কোন অ্যাপ খোলা") || lower.contains("current app") || lower.contains("কোন অ্যাপ চলছে")) {
            val label = deviceStateManager.getForegroundAppLabel()
            return LocalExecutionResult(
                isHandled = true,
                responseText = if (label != null) "বর্তমানে খোলা অ্যাপ: $label।" else "বর্তমান অ্যাপ পড়া যাচ্ছে না — Usage Access পারমিশন প্রয়োজন।",
                emotion = ArohiEmotion.FOCUSED,
                toolName = "get_current_app"
            )
        }

        return LocalExecutionResult(isHandled = false, responseText = "")
    }

    private fun detectSettingsPanel(lower: String): DeviceControlManager.SettingsPanel? {
        return when {
            lower.contains("ওয়াইফাই") || lower.contains("wifi") ->
                DeviceControlManager.SettingsPanel.WIFI
            lower.contains("ব্লুটুথ সেটিং") || lower.contains("bluetooth setting") ->
                DeviceControlManager.SettingsPanel.BLUETOOTH
            lower.contains("ডিসপ্লে") || lower.contains("display") || lower.contains("ব্রাইটনেস") ->
                DeviceControlManager.SettingsPanel.DISPLAY
            lower.contains("সাউন্ড সেটিং") || lower.contains("sound setting") || lower.contains("রিংটোন") ->
                DeviceControlManager.SettingsPanel.SOUND
            lower.contains("ব্যাটারি সেটিং") || lower.contains("battery setting") ->
                DeviceControlManager.SettingsPanel.BATTERY
            lower.contains("নোটিফিকেশন সেটিং") || lower.contains("notification setting") ->
                DeviceControlManager.SettingsPanel.NOTIFICATIONS
            lower.contains("অ্যাক্সেসিবিলিটি") || lower.contains("accessibility") ->
                DeviceControlManager.SettingsPanel.ACCESSIBILITY
            lower.contains("অ্যাপ সেটিং") || lower.contains("app setting") ->
                DeviceControlManager.SettingsPanel.APPS
            lower.contains("স্টোরেজ সেটিং") || lower.contains("storage setting") ->
                DeviceControlManager.SettingsPanel.STORAGE
            lower.contains("ডিভাইস ইনফো") || lower.contains("device info") || lower.contains("ফোন সম্পর্কে") ->
                DeviceControlManager.SettingsPanel.DEVICE_INFO
            lower.contains("লোকেশন") || lower.contains("location") ->
                DeviceControlManager.SettingsPanel.LOCATION
            lower.contains("সিকিউরিটি") || lower.contains("security") ->
                DeviceControlManager.SettingsPanel.SECURITY
            lower.contains("সেটিংস খোলো") || lower.contains("সেটিং খোলো") || lower.contains("open settings") ->
                DeviceControlManager.SettingsPanel.APPS
            else -> null
        }
    }

    private fun detectMediaAction(lower: String): String? {
        return when {
            lower.contains("গান চালাও") || lower.contains("প্লে") || lower.contains("play") -> "play"
            lower.contains("গান থামাও") || lower.contains("পজ") || lower.contains("pause") -> "pause"
            lower.contains("পরের গান") || lower.contains("next") -> "next"
            lower.contains("আগের গান") || lower.contains("previous") || lower.contains("prev") -> "previous"
            else -> null
        }
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
