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
    private val memoryRepository: MemoryRepository,
    private val notificationRepository: NotificationRepository,
    private val routineRepository: RoutineRepository,
    private val conversationRepository: ConversationRepository,
    private val settingsRepository: SettingsRepository,
    private val localCommandEngine: LocalCommandEngine,
    private val verificationEngine: VerificationEngine,
    private val emotionEngine: EmotionEngine,
    private val commandOrchestrator: CommandOrchestrator,
    private val diagnosticsProvider: suspend () -> String,
    private val timerActions: TimerActions? = null,
    private val alarmActions: AlarmActions? = null
) {
    /** Real timer callbacks, provided by the application layer. */
    interface TimerActions {
        fun startSeconds(seconds: Long, label: String): String
        fun list(): String
    }

    /** Real alarm callback, provided by the application layer. */
    interface AlarmActions {
        fun setAlarm(hour24: Int, minute: Int): String
    }

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    suspend fun processInput(
        userInput: String,
        imageInlineData: InlineData? = null
    ): BrainResponse {
        _isProcessing.value = true
        emotionEngine.setEmotion(ArohiEmotion.THINKING)

        try {
            // If no image is attached, run the real universal command pipeline first
            // (intent → capability/permission check → execute → verify), then the
            // legacy local command engine for instant offline handling.
            if (imageInlineData == null) {
                val orchestrated = commandOrchestrator.handle(userInput)
                if (orchestrated.handled) {
                    _isProcessing.value = false
                    emotionEngine.setEmotion(orchestrated.emotion)
                    conversationRepository.addMessage(
                        role = "AROHI",
                        content = orchestrated.text,
                        emotion = orchestrated.emotion.name,
                        isVoice = true,
                        toolCallJson = orchestrated.toolName
                    )
                    return BrainResponse(
                        text = orchestrated.text,
                        emotion = orchestrated.emotion,
                        toolCall = orchestrated.toolName,
                        isLocalOnly = true
                    )
                }

                val localResult = localCommandEngine.tryExecuteLocally(userInput)
                if (localResult.isHandled) {
                    _isProcessing.value = false
                    emotionEngine.setEmotion(localResult.emotion)

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
                return BrainResponse(
                    text = fallbackText,
                    emotion = ArohiEmotion.CONFUSED,
                    isLocalOnly = true
                )
            }

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
                val err = "Gemini সার্ভার সমস্যা ($code)। অনুগ্রহ করে নেটওয়ার্ক ও API কী যাচাই করুন।"
                _isProcessing.value = false
                emotionEngine.setEmotion(ArohiEmotion.ERROR)
                return BrainResponse(text = err, emotion = ArohiEmotion.ERROR)
            }

            val candidate = apiResponse.body()?.candidates?.firstOrNull()
            val modelContent = candidate?.content
            val firstPart = modelContent?.parts?.firstOrNull()

            // Check if model returned a function call
            if (firstPart?.functionCall != null) {
                emotionEngine.setEmotion(ArohiEmotion.EXECUTING)
                val fnCall = firstPart.functionCall
                val toolExecResult = executeToolCall(fnCall)

                // Return tool execution response
                _isProcessing.value = false
                val finalEmotion = emotionEngine.inferEmotionFromText(toolExecResult)
                emotionEngine.setEmotion(finalEmotion)

                conversationRepository.addMessage(
                    role = "AROHI",
                    content = toolExecResult,
                    emotion = finalEmotion.name,
                    isVoice = true,
                    toolCallJson = fnCall.name,
                    toolResultJson = toolExecResult
                )

                return BrainResponse(
                    text = toolExecResult,
                    emotion = finalEmotion,
                    toolCall = fnCall.name,
                    toolResult = toolExecResult
                )
            }

            // Normal textual response
            val responseText = firstPart?.text?.trim() ?: "আমি বুঝতে পারিনি, আবার বলুন।"
            val inferredEmotion = emotionEngine.inferEmotionFromText(responseText)
            _isProcessing.value = false
            emotionEngine.setEmotion(inferredEmotion)

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
            val errText = "সাময়িক সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
            return BrainResponse(text = errText, emotion = ArohiEmotion.ERROR)
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
                    "'$appName' অ্যাপটি আপনার ডিভাইসে খুঁজে পাওয়া যায়নি।"
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
                "ডিভাইস স্ট্যাটাস: ব্যাটারি ${telemetry.batteryPercent}% (${if (telemetry.isCharging) "চার্জিং" else "ব্যাটারিতে"}), ফ্রি র‍্যাম ${telemetry.freeRamMb}MB/${telemetry.totalRamMb}MB, ফ্রি স্টোরেজ ${telemetry.freeStorageGb}GB, নেটওয়ার্ক: ${telemetry.networkType}, ভলিউম: ${telemetry.mediaVolumePercent}%।"
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
                access?.inspectCurrentScreen() ?: "Accessibility Service সক্রিয় নেই। সেটিংস থেকে পারমিশন দিন।"
            }
            "click_screen_element" -> {
                val query = args["query"]?.toString() ?: ""
                val access = ArohiAccessibilityService.instance
                if (access != null) {
                    val clicked = access.clickByText(query)
                    if (clicked) "'$query' ক্লিক করা হয়েছে।" else "'$query' খুঁজে পাওয়া যায়নি।"
                } else {
                    "Accessibility Service সক্রিয় নেই।"
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
                    "মেমোরিতে '$query' সম্পর্কিত কোনো তথ্য পাওয়া যায়নি।"
                } else {
                    "মেমোরি তথ্য:\n" + results.joinToString("\n") { "• ${it.key}: ${it.value}" }
                }
            }
            "set_timer" -> {
                val seconds = args["seconds"]?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                val label = args["label"]?.toString() ?: "Timer"
                timerActions?.startSeconds(seconds, label)
                    ?: "টাইমার ইঞ্জিন প্রস্তুত নয়।"
            }
            "list_timers" -> {
                timerActions?.list() ?: "চলমান কোনো টাইমার নেই।"
            }
            "set_alarm" -> {
                val hour = args["hour"]?.toString()?.toDoubleOrNull()?.toInt() ?: -1
                val minute = args["minute"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                if (hour in 0..23) {
                    alarmActions?.setAlarm(hour, minute) ?: "অ্যালার্ম ইঞ্জিন প্রস্তুত নয়।"
                } else {
                    "অ্যালার্মের সময়টি সঠিক নয়।"
                }
            }
            "diagnostics_check" -> {
                // Real diagnostic report — no hardcoded/fake health (spec §58).
                diagnosticsProvider()
            }
            else -> "কমান্ড '$name' সম্পন্ন করা হয়েছে।"
        }
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
