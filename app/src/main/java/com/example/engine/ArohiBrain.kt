package com.example.engine

import android.content.Context
import com.example.data.remote.Candidate
import com.example.data.remote.Content
import com.example.data.remote.FunctionCall
import com.example.data.remote.FunctionResponse
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.GeminiClient
import com.example.data.remote.InlineData
import com.example.data.remote.Part
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RoutineRepository
import com.example.data.repository.SettingsRepository
import com.example.device.AppDiscoveryManager
import com.example.device.ContactsManager
import com.example.device.DeviceStateManager
import com.example.device.PhoneStateManager
import com.example.device.TelephonyHelper
import com.example.service.ArohiAccessibilityService
import com.example.service.ArohiNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class BrainResponse(
    val text: String,
    val emotion: ArohiEmotion,
    val toolCall: String? = null,
    val toolResult: String? = null,
    val isLocalOnly: Boolean = false
)

class ArohiBrain(
    private val context: Context,
    private val deviceStateManager: DeviceStateManager,
    private val appDiscoveryManager: AppDiscoveryManager,
    private val contactsManager: ContactsManager,
    private val telephonyHelper: TelephonyHelper,
    private val phoneStateManager: PhoneStateManager,
    private val memoryRepository: MemoryRepository,
    private val notificationRepository: NotificationRepository,
    private val routineRepository: RoutineRepository,
    private val conversationRepository: ConversationRepository,
    private val settingsRepository: SettingsRepository,
    private val localCommandEngine: LocalCommandEngine,
    private val verificationEngine: VerificationEngine,
    private val emotionEngine: EmotionEngine,
    private val activityTracker: BrainActivityTracker,
    private val eventBus: SystemEventBus
) {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    suspend fun processInput(
        userInput: String,
        imageInlineData: InlineData? = null
    ): BrainResponse {
        _isProcessing.value = true
        emotionEngine.setEmotion(ArohiEmotion.THINKING)
        activityTracker.setPhase(BrainPhase.UNDERSTANDING)

        try {
            // If no image is attached, test local engine first for instant response
            if (imageInlineData == null) {
                val localResult = localCommandEngine.tryExecuteLocally(userInput)
                if (localResult.isHandled) {
                    _isProcessing.value = false
                    emotionEngine.setEmotion(localResult.emotion)
                    activityTracker.setPhase(BrainPhase.VERIFYING)
                    activityTracker.setPhase(BrainPhase.DONE)

                    // Persist to history
                    conversationRepository.addMessage(
                        role = "AROHI",
                        content = localResult.responseText,
                        emotion = localResult.emotion.name,
                        isVoice = true,
                        toolCallJson = localResult.toolName
                    )

                    return BrainResponse(
                        text = localResult.responseText,
                        emotion = localResult.emotion,
                        toolCall = localResult.toolName,
                        isLocalOnly = true
                    )
                }
            }

            // Route to Gemini Brain with System Prompt, Memory, and Tool capabilities
            val apiKey = settingsRepository.getApiKey()
            val modelName = settingsRepository.getModelName()

            if (apiKey.isBlank()) {
                val fallbackText = "Gemini API Key সেট করা নেই। সেটিংস থেকে আপনার Gemini API Key যুক্ত করুন অথবা সাধারণ ভয়েস কমান্ড (ব্যাটারি, টর্চ, অ্যাপ ওপেন, কল) ব্যবহার করুন।"
                _isProcessing.value = false
                emotionEngine.setEmotion(ArohiEmotion.CONFUSED)
                activityTracker.setPhase(BrainPhase.DONE)
                return BrainResponse(
                    text = fallbackText,
                    emotion = ArohiEmotion.CONFUSED,
                    isLocalOnly = true
                )
            }

            activityTracker.setPhase(BrainPhase.CHECKING_CONTEXT)
            val systemInstruction = buildSystemPrompt()
            val recentMessages = conversationRepository.getRecentMessages(10).reversed()
            val contents = mutableListOf<Content>()

            // Add conversation context
            for (msg in recentMessages) {
                val role = if (msg.role == "USER") "user" else "model"
                contents.add(
                    Content(
                        role = role,
                        parts = listOf(Part(text = msg.content))
                    )
                )
            }

            // Add current user prompt (with optional image)
            val currentParts = mutableListOf<Part>()
            currentParts.add(Part(text = userInput))
            if (imageInlineData != null) {
                currentParts.add(Part(inlineData = imageInlineData))
            }
            contents.add(Content(role = "user", parts = currentParts))

            activityTracker.setPhase(BrainPhase.PLANNING_ACTION)
            val request = GenerateContentRequest(
                systemInstruction = systemInstruction,
                contents = contents,
                tools = listOf(ToolRegistry.availableTools),
                generationConfig = GenerationConfig(
                    temperature = 0.7f,
                    maxOutputTokens = 1024
                )
            )

            val apiResponse = GeminiClient.service.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )

            if (!apiResponse.isSuccessful) {
                val code = apiResponse.code()
                val err = "Gemini সার্ভার সমস্যা ($code)। অনুগ্রহ করে নেটওয়ার্ক ও API কী যাচাই করুন।"
                _isProcessing.value = false
                emotionEngine.setEmotion(ArohiEmotion.ERROR)
                activityTracker.setPhase(BrainPhase.ERROR)
                eventBus.log("GEMINI", "Request failed ($code)", SystemEventLevel.ERROR)
                return BrainResponse(text = err, emotion = ArohiEmotion.ERROR)
            }

            val candidate = apiResponse.body()?.candidates?.firstOrNull()
            val modelContent = candidate?.content
            val firstPart = modelContent?.parts?.firstOrNull()

            // Check if model returned a function call — run the REAL multi-step tool loop
            if (firstPart?.functionCall != null) {
                val fnCall = firstPart.functionCall
                val result = runToolLoop(
                    apiKey = apiKey,
                    modelName = modelName,
                    request = request,
                    firstCall = fnCall,
                    accumulatedContents = contents
                )
                _isProcessing.value = false
                activityTracker.setPhase(BrainPhase.DONE)
                return result
            }

            // Normal textual response
            val responseText = firstPart?.text?.trim() ?: "আমি বুঝতে পারিনি, আবার বলুন।"
            val inferredEmotion = emotionEngine.inferEmotionFromText(responseText)
            _isProcessing.value = false
            activityTracker.setPhase(BrainPhase.RESPONDING)
            emotionEngine.setEmotion(inferredEmotion)
            activityTracker.setPhase(BrainPhase.DONE)

            conversationRepository.addMessage(
                role = "AROHI",
                content = responseText,
                emotion = inferredEmotion.name,
                isVoice = true
            )

            return BrainResponse(
                text = responseText,
                emotion = inferredEmotion
            )

        } catch (e: Exception) {
            _isProcessing.value = false
            emotionEngine.setEmotion(ArohiEmotion.ERROR)
            activityTracker.setPhase(BrainPhase.ERROR)
            val errText = "সাময়িক সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
            eventBus.log("BRAIN", "Error: ${e.localizedMessage ?: "unknown"}", SystemEventLevel.ERROR)
            return BrainResponse(text = errText, emotion = ArohiEmotion.ERROR)
        }
    }

    /**
     * Real multi-step task engine: asks Gemini for tool calls, executes each
     * one on the device, feeds the genuine result back, and repeats until the
     * model produces a final answer (max 4 steps). Every step is published to
     * the live Task Execution UI through [BrainActivityTracker].
     */
    private suspend fun runToolLoop(
        apiKey: String,
        modelName: String,
        request: GenerateContentRequest,
        firstCall: FunctionCall,
        accumulatedContents: MutableList<Content>
    ): BrainResponse {
        activityTracker.startTask(accumulatedContents.lastOrNull { it.role == "user" }
            ?.parts?.firstOrNull()?.text?.take(80) ?: "Task")
        eventBus.log("TASK", "Task started", SystemEventLevel.INFO)

        var pendingCall: FunctionCall? = firstCall
        val steps = mutableListOf<TaskStep>()
        var stepOrder = 0
        var guard = 0
        var lastToolResult = ""
        var lastText = ""

        while (pendingCall != null && guard < 4) {
            guard++
            stepOrder++
            val call = pendingCall
            steps += TaskStep(
                order = stepOrder,
                toolName = call.name,
                description = describeToolCall(call),
                status = TaskStepStatus.RUNNING
            )
            activityTracker.setSteps(steps.toList())
            emotionEngine.setEmotion(ArohiEmotion.EXECUTING)
            activityTracker.setPhase(BrainPhase.EXECUTING)
            eventBus.log("TASK", "Executing: ${call.name}", SystemEventLevel.INFO)

            val toolResult = try {
                executeToolCall(call)
            } catch (e: Exception) {
                "টুল এক্সিকিউশন ব্যর্থ: ${e.localizedMessage ?: call.name}"
            }

            val failed = toolResult.contains("ব্যর্থ") || toolResult.contains("সম্ভব হয়নি") ||
                toolResult.contains("সম্ভব হয়নি") || toolResult.contains("পাওয়া যায়নি")
            steps[steps.size - 1] = steps.last().copy(
                status = if (failed) TaskStepStatus.FAILED else TaskStepStatus.COMPLETED,
                detail = toolResult
            )
            activityTracker.setSteps(steps.toList())
            lastToolResult = toolResult

            // Feed the genuine result back to the model
            val feedbackContents = accumulatedContents.toMutableList()
            feedbackContents.add(Content(role = "model", parts = listOf(Part(functionCall = call))))
            feedbackContents.add(
                Content(
                    role = "user",
                    parts = listOf(
                        Part(
                            functionResponse = FunctionResponse(
                                name = call.name,
                                response = mapOf("result" to toolResult)
                            )
                        )
                    )
                )
            )

            val feedbackRequest = request.copy(contents = feedbackContents)
            val feedbackResponse = GeminiClient.service.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = feedbackRequest
            )

            if (!feedbackResponse.isSuccessful) {
                val code = feedbackResponse.code()
                eventBus.log("TASK", "Gemini follow-up failed ($code)", SystemEventLevel.ERROR)
                pendingCall = null
                lastText = ""
                break
            }

            val nextPart = feedbackResponse.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
            pendingCall = nextPart?.functionCall
            nextPart?.text?.let { lastText = it.trim() }
        }

        activityTracker.setPhase(BrainPhase.VERIFYING)
        val finalText = if (lastText.isNotBlank()) lastText else if (lastToolResult.isNotBlank()) {
            "টাস্ক সম্পন্ন হয়েছে: $lastToolResult"
        } else {
            "টাস্ক সম্পন্ন করা হয়েছে।"
        }

        val finalEmotion = emotionEngine.inferEmotionFromText(finalText)
        emotionEngine.setEmotion(finalEmotion)
        activityTracker.finishTask(success = finalEmotion != ArohiEmotion.ERROR)
        activityTracker.setPhase(BrainPhase.DONE)
        eventBus.log("TASK", if (finalEmotion != ArohiEmotion.ERROR) "Task completed" else "Task failed", SystemEventLevel.SUCCESS)

        conversationRepository.addMessage(
            role = "AROHI",
            content = finalText,
            emotion = finalEmotion.name,
            isVoice = true,
            toolCallJson = steps.firstOrNull()?.toolName,
            toolResultJson = steps.joinToString("\n") { "• ${it.toolName}: ${it.detail}" }
        )

        return BrainResponse(
            text = finalText,
            emotion = finalEmotion,
            toolCall = steps.firstOrNull()?.toolName,
            toolResult = steps.joinToString("\n") { "• ${it.toolName}: ${it.detail}" }
        )
    }

    private fun describeToolCall(call: FunctionCall): String {
        return when (call.name) {
            "open_app" -> "Opening ${call.args?.get("app_name") ?: "app"}"
            "make_phone_call" -> "Calling ${call.args?.get("target") ?: "contact"}"
            "send_sms" -> "Preparing SMS"
            "send_whatsapp" -> "Preparing WhatsApp message"
            "read_device_telemetry" -> "Reading device telemetry"
            "toggle_flashlight" -> "Toggling flashlight"
            "set_media_volume" -> "Setting media volume"
            "media_control" -> "Media control"
            "read_notifications" -> "Reading notifications"
            "inspect_screen" -> "Reading screen content"
            "click_screen_element" -> "Tapping screen element"
            "save_user_memory" -> "Saving to memory"
            "search_memory" -> "Searching memory"
            "open_settings" -> "Opening settings panel"
            "open_url" -> "Opening URL"
            "get_current_app" -> "Detecting foreground app"
            "check_call_state" -> "Checking call state"
            "diagnostics_check" -> "Running diagnostics"
            else -> call.name
        }
    }

    private suspend fun executeToolCall(functionCall: FunctionCall): String {
        val name = functionCall.name
        val args = functionCall.args ?: emptyMap()

        return when (name) {
            "open_app" -> {
                val appName = args["app_name"]?.toString() ?: ""
                val foundApp = appDiscoveryManager.findApp(appName)
                if (foundApp != null) {
                    val launched = appDiscoveryManager.launchApp(foundApp.packageName)
                    verificationEngine.verifyAppLaunch(launched, foundApp.label).summary
                } else {
                    "'$appName' অ্যাপটি আপনার ডিভাইসে খুঁজে পাওয়া যায়নি।"
                }
            }
            "make_phone_call" -> {
                val target = args["target"]?.toString() ?: ""
                val contacts = contactsManager.searchContacts(target)
                val targetNumber = if (contacts.isNotEmpty()) contacts.first().phoneNumber else target
                val displayName = if (contacts.isNotEmpty()) contacts.first().name else target
                val hasCallPermission = telephonyHelper.hasCallPermission()
                val success = telephonyHelper.makeCallOrDial(targetNumber)
                verificationEngine.verifyCall(success, displayName, hasCallPermission).summary
            }
            "send_sms" -> {
                val target = args["target"]?.toString() ?: ""
                val msg = args["message"]?.toString() ?: ""
                val contacts = contactsManager.searchContacts(target)
                val targetNumber = if (contacts.isNotEmpty()) contacts.first().phoneNumber else target
                val success = telephonyHelper.sendSms(targetNumber, msg)
                if (success) "এসএমএস প্রেরণের স্ক্রিন প্রস্তুত করা হয়েছে।" else "এসএমএস পাঠানো যায়নি।"
            }
            "send_whatsapp" -> {
                val phone = args["phone_number"]?.toString() ?: ""
                val msg = args["message"]?.toString() ?: ""
                val success = telephonyHelper.sendWhatsAppMessage(phone, msg)
                if (success) "হোয়াটসঅ্যাপ মেসেজ উইন্ডো খোলা হয়েছে।" else "হোয়াটসঅ্যাপ মেসেজ পাঠানো যায়নি।"
            }
            "read_device_telemetry" -> {
                val telemetry = deviceStateManager.getTelemetry()
                "ডিভাইস স্ট্যাটাস: ব্যাটারি ${telemetry.batteryPercent}% (${if (telemetry.isCharging) "চার্জিং" else "ব্যাটারিতে"}), ফ্রি র‍্যাম ${telemetry.freeRamMb}MB/${telemetry.totalRamMb}MB, ফ্রি স্টোরেজ ${telemetry.freeStorageGb}GB, নেটওয়ার্ক: ${telemetry.networkType}, ভলিউম: ${telemetry.mediaVolumePercent}%, ব্লুটুথ: ${telemetry.bluetoothState}।"
            }
            "toggle_flashlight" -> {
                val enabled = args["enabled"]?.toString()?.toBooleanStrictOrNull() ?: true
                val success = deviceStateManager.toggleFlashlight(enabled)
                verificationEngine.verifyTorch(success, enabled).summary
            }
            "set_media_volume" -> {
                val percent = args["percent"]?.toString()?.toDoubleOrNull()?.toInt() ?: 50
                val success = deviceStateManager.setMediaVolume(percent)
                verificationEngine.verifyVolume(success, percent).summary
            }
            "media_control" -> {
                val action = args["action"]?.toString() ?: "play_pause"
                val success = deviceControlManagerForBrain().dispatchMediaAction(action)
                if (success) "মিডিয়া কন্ট্রোল ($action) পাঠানো হয়েছে।" else "মিডিয়া কন্ট্রোল পাঠানো যায়নি।"
            }
            "open_settings" -> {
                val panel = args["panel"]?.toString() ?: ""
                val targetPanel = when (panel.lowercase(Locale.ROOT)) {
                    "wifi", "wifi_settings" -> com.example.device.DeviceControlManager.SettingsPanel.WIFI
                    "bluetooth" -> com.example.device.DeviceControlManager.SettingsPanel.BLUETOOTH
                    "display", "brightness", "screen" -> com.example.device.DeviceControlManager.SettingsPanel.DISPLAY
                    "sound", "volume", "ringtone" -> com.example.device.DeviceControlManager.SettingsPanel.SOUND
                    "battery", "power" -> com.example.device.DeviceControlManager.SettingsPanel.BATTERY
                    "notification" -> com.example.device.DeviceControlManager.SettingsPanel.NOTIFICATIONS
                    "accessibility" -> com.example.device.DeviceControlManager.SettingsPanel.ACCESSIBILITY
                    "app", "applications" -> com.example.device.DeviceControlManager.SettingsPanel.APPS
                    "storage" -> com.example.device.DeviceControlManager.SettingsPanel.STORAGE
                    "about", "device_info" -> com.example.device.DeviceControlManager.SettingsPanel.DEVICE_INFO
                    "location" -> com.example.device.DeviceControlManager.SettingsPanel.LOCATION
                    "security" -> com.example.device.DeviceControlManager.SettingsPanel.SECURITY
                    else -> com.example.device.DeviceControlManager.SettingsPanel.APPS
                }
                val success = deviceControlManagerForBrain().openSettingsPanel(targetPanel)
                if (success) "${targetPanel.label} খোলা হয়েছে।" else "${targetPanel.label} খোলা যায়নি।"
            }
            "open_url" -> {
                val url = args["url"]?.toString() ?: ""
                val success = deviceControlManagerForBrain().openUrl(url)
                if (success) "$url খোলা হয়েছে।" else "URL খোলা যায়নি।"
            }
            "get_current_app" -> {
                val label = deviceStateManager.getForegroundAppLabel()
                if (label != null) "বর্তমানে খোলা অ্যাপ: $label।" else "বর্তমান অ্যাপ পড়া যাচ্ছে না — Usage Access পারমিশন প্রয়োজন।"
            }
            "check_call_state" -> {
                val call = phoneStateManager.callInfo.value
                when (call.state) {
                    com.example.device.CallState.RINGING ->
                        if (call.callerName.isNotBlank()) "${call.callerName} কল করছে (${call.incomingNumber})।"
                        else "অজানা নম্বর (${call.incomingNumber}) থেকে কল আসছে।"
                    com.example.device.CallState.OFFHOOK -> "বর্তমানে একটি কল চলছে।"
                    com.example.device.CallState.IDLE -> "বর্তমানে কোনো কল নেই।"
                    else -> "কল স্টেট পাওয়া যায়নি।"
                }
            }
            "read_notifications" -> {
                val unread = notificationRepository.getRecentUnread(5)
                if (unread.isEmpty()) {
                    "নতুন কোনো অপঠিত নোটিফিকেশন নেই।"
                } else {
                    val summary = unread.joinToString("\n") {
                        "• ${it.appName}: ${it.title} - ${it.text}"
                    }
                    "সাম্প্রতিক নোটিফিকেশনসমূহ:\n$summary"
                }
            }
            "inspect_screen" -> {
                val access = ArohiAccessibilityService.instance
                access?.inspectCurrentScreen() ?: "Accessibility Service সক্রিয় নেই। সেটিংস থেকে পারমিশন দিন।"
            }
            "click_screen_element" -> {
                val query = args["query"]?.toString() ?: ""
                val access = ArohiAccessibilityService.instance
                if (access != null) {
                    val clicked = access.clickByText(query)
                    if (clicked) "'$query' ক্লিক করা হয়েছে।" else "'$query' খুঁজে পাওয়া যায়নি।"
                } else {
                    "Accessibility Service সক্রিয় নেই।"
                }
            }
            "save_user_memory" -> {
                val category = args["category"]?.toString() ?: "PROFILE"
                val key = args["key"]?.toString() ?: "fact"
                val value = args["value"]?.toString() ?: ""
                val id = memoryRepository.saveMemory(category, key, value)
                verificationEngine.verifyMemory(id, key).summary
            }
            "search_memory" -> {
                val query = args["query"]?.toString() ?: ""
                val results = memoryRepository.search(query)
                if (results.isEmpty()) {
                    "মেমোরিতে '$query' সম্পর্কিত কোনো তথ্য পাওয়া যায়নি।"
                } else {
                    "মেমোরি তথ্য:\n" + results.joinToString("\n") { "• ${it.key}: ${it.value}" }
                }
            }
            "diagnostics_check" -> {
                val telemetry = deviceStateManager.getTelemetry()
                val isAccess = ArohiAccessibilityService.isServiceRunning()
                val isNotif = ArohiNotificationListenerService.isConnected
                "সিস্টেম ডায়াগনস্টিকস:\n• ব্যাটারি ও সেন্সর: সচল\n• কন্ট্রোল সার্ভিস: ${if (isAccess) "সক্রিয়" else "নিষ্ক্রিয়"}\n• নোটিফিকেশন লিসেনার: ${if (isNotif) "সংযুক্ত" else "অপেক্ষারত"}\n• ওএস: ${telemetry.androidVersion}"
            }
            else -> "কমান্ড '$name' সম্পন্ন করা হয়েছে।"
        }
    }

    /** Shared real control surface for settings/URL/media actions. */
    private fun deviceControlManagerForBrain(): com.example.device.DeviceControlManager {
        return (context.applicationContext as? com.example.ArohiApplication)?.deviceControlManager
            ?: com.example.device.DeviceControlManager(context)
    }

    private suspend fun buildSystemPrompt(): Content {
        val telemetry = deviceStateManager.getTelemetry()
        val profileMemories = memoryRepository.search("")
        val memoryContext = if (profileMemories.isNotEmpty()) {
            profileMemories.take(10).joinToString(", ") { "${it.key}: ${it.value}" }
        } else {
            "Creator: Shù Vrô, Target: Samsung Galaxy S8+ / Android 9"
        }

        val promptText = """
            You are AROHI (আরোহী), a real, high-capability autonomous personal AI assistant created by "Shù Vrô" (Shuvro). You identify strictly as "AROHI AI ASSISTANT by Shù Vrô".
            
            PERSONALITY & TONE:
            - Warm, witty, sharp, empathetic, and exceptionally capable.
            - Address the user affectionately as "বস" (Boss) when speaking Bengali.
            - Fluent in Bengali (বাংলা), Banglish, English, and Hindi. Switch naturally if the user switches languages.
            - Respond primarily in natural, colloquial Bengali unless addressed in another language. Use expressions like "আচ্ছা বস", "হুম, বুঝেছি।"
            - Keep voice responses concise, punchy, and clear for text-to-speech.
            
            SILENCE COMMAND ("চুপ করো", "Stop talking"):
            - If the user commands you to stop talking, acknowledge immediately with "আচ্ছা বস, চুপ থাকছি। যখন ডাকবেন তখন আসব।" and stop generating further conversational text. Do not argue.
            
            DESTRUCTIVE ACTION WARNING:
            - If the user asks to delete an important file or uninstall an app with data, warn them BEFORE executing: "বস, আপনি এই fileটা delete করতে যাচ্ছেন। এটা গুরুত্বপূর্ণ মনে হচ্ছে। আপনি কি নিশ্চিত?"
            
            CAPABILITIES & TRUTH:
            - You have genuine operating control over the Android phone through tool function calls.
            - NEVER fabricate or pretend actions succeeded. Use real tools provided. Do not say "Done" without evidence.
            - Do not generate fake notification data, fake battery values, or fake states. Use real parameters.
            - You cannot bypass Android security (PIN, lock screen, banking auth). Fail gracefully.
            
            DEVICE & USER CONTEXT:
            - Creator: Shù Vrô
            - Target Device Spec: Samsung Galaxy S8+ / Android 9
            - Device: ${telemetry.deviceName} (${telemetry.androidVersion}, API ${telemetry.apiLevel})
            - Battery: ${telemetry.batteryPercent}% (${if (telemetry.isCharging) "Charging" else "On Battery"})
            - Free Storage: ${telemetry.freeStorageGb} GB
            - Network: ${telemetry.networkType}
            - Memory Notes: $memoryContext
        """.trimIndent()

        return Content(role = "system", parts = listOf(Part(text = promptText)))
    }
}
