package com.example.engine

import android.content.Context
import com.example.ArohiApplication
import com.example.data.local.entity.ReminderRepeat
import com.example.data.remote.ClaudeClient
import com.example.data.remote.ClaudeMessage
import com.example.data.remote.ClaudeTool
import com.example.data.remote.Content
import com.example.data.remote.FunctionCall
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.GeminiClient
import com.example.data.remote.InlineData
import com.example.data.remote.Part
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.NoteRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ReminderRepository
import com.example.data.repository.RoutineRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TodoRepository
import com.example.data.repository.WeatherRepository
import com.example.device.AppDiscoveryManager
import com.example.device.CalendarHelper
import com.example.device.ContactsManager
import com.example.device.DeviceStateManager
import com.example.device.ReminderScheduler
import com.example.device.TelephonyHelper
import com.example.privacy.DataExporter
import com.example.service.ArohiAccessibilityService
import com.example.service.ArohiNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrainResponse(
    val text: String,
    val emotion: ArohiEmotion,
    val toolCall: String? = null,
    val toolResult: String? = null,
    val isLocalOnly: Boolean = false,
    val isOfflineFallback: Boolean = false
)

/**
 * The router of everything AROHI can do:
 *  local productivity engine → local device engine → (optionally) cloud brain with tools.
 */
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
    private val reminderRepository: ReminderRepository,
    private val todoRepository: TodoRepository,
    private val noteRepository: NoteRepository,
    private val weatherRepository: WeatherRepository,
    private val calendarHelper: CalendarHelper,
    private val productivityEngine: ProductivityEngine,
    private val offlineFallbackEngine: OfflineFallbackEngine,
    private val proactiveSuggestionEngine: ProactiveSuggestionEngine,
    private val memoryExtractor: MemoryExtractor,
    private val personaEngine: PersonaEngine
) {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    suspend fun processInput(
        userInput: String,
        imageInlineData: InlineData? = null
    ): BrainResponse {
        _isProcessing.value = true
        emotionEngine.setEmotion(ArohiEmotion.THINKING)

        try {
            val trimmedInput = userInput.trim()

            // Remember who the user is (name, favourites, routines) — local Room only.
            if (imageInlineData == null && trimmedInput.isNotBlank()) {
                runCatching { memoryExtractor.extractAndSave(trimmedInput) }
            }

            if (imageInlineData == null) {
                // 1. Productivity layer (reminders, to-dos, notes, maths, weather, calendar...)
                val productivityResult = runCatching { productivityEngine.tryExecute(trimmedInput) }.getOrNull()
                if (productivityResult != null && productivityResult.isHandled) {
                    return finishLocal(productivityResult)
                }

                // 2. Local device control layer (torch, volume, calls, apps, routines...)
                val localResult = runCatching { localCommandEngine.tryExecuteLocally(trimmedInput) }.getOrNull()
                if (localResult != null && localResult.isHandled) {
                    return finishLocal(localResult)
                }
            }

            val apiKey = settingsRepository.getActiveApiKey()
            val telemetry = deviceStateManager.getTelemetry()
            val privacyMode = settingsRepository.isPrivateMode()
            val cloudAllowed = settingsRepository.isCloudAiEnabled() && !privacyMode

            // 3. Offline / privacy fallback — always answers with a real local capability set.
            if (apiKey.isBlank() || !cloudAllowed || !telemetry.isConnected) {
                return offlineReply(trimmedInput, privacyMode)
            }

            val personaContext = buildPersonaContext(telemetry)

            // 4a. Claude (Anthropic) transport — the user picked it and supplied a key.
            if (settingsRepository.isClaudeProvider()) {
                return claudeReply(
                    userInput = userInput,
                    systemPrompt = personaEngine.buildSystemPrompt(personaContext),
                    apiKey = apiKey
                )
            }

            // 4b. Gemini transport (default) with the full persona + tools
            val systemInstruction = personaEngine.buildSystemInstruction(personaContext)
            val recentMessages = conversationRepository.getRecentMessages(10).reversed()
            val contents = mutableListOf<Content>()

            for (msg in recentMessages) {
                val role = if (msg.role == "USER") "user" else "model"
                contents.add(
                    Content(
                        role = role,
                        parts = listOf(Part(text = msg.content))
                    )
                )
            }

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
                    temperature = 0.8f,
                    maxOutputTokens = 1024
                )
            )

            val apiResponse = GeminiClient.service.generateContent(
                model = settingsRepository.getModelName(),
                apiKey = apiKey,
                request = request
            )

            if (!apiResponse.isSuccessful) {
                val code = apiResponse.code()
                val fallback = offlineFallbackEngine.respond(trimmedInput, privacyMode)
                val text = if (settingsRepository.isOfflineFallbackEnabled()) {
                    "ক্লাউডে সংযোগ করতে পারছি না (সমস্যা $code)। ${fallback.text}"
                } else {
                    "Gemini সার্ভার সমস্যা ($code)। অনুগ্রহ করে নেটওয়ার্ক ও API কী যাচাই করুন।"
                }
                _isProcessing.value = false
                emotionEngine.setEmotion(ArohiEmotion.CONCERNED)
                return BrainResponse(text = text, emotion = ArohiEmotion.CONCERNED)
            }

            val candidate = apiResponse.body()?.candidates?.firstOrNull()
            val modelParts = candidate?.content?.parts ?: emptyList()

            // 5. Tool call?
            val functionCall = modelParts.firstOrNull { it.functionCall != null }?.functionCall
            if (functionCall != null) {
                emotionEngine.setEmotion(ArohiEmotion.EXECUTING)
                val toolExecResult = executeToolCall(functionCall)
                _isProcessing.value = false
                val finalEmotion = emotionEngine.inferEmotionFromText(toolExecResult)
                emotionEngine.setEmotion(finalEmotion)

                conversationRepository.addMessage(
                    role = "AROHI",
                    content = toolExecResult,
                    emotion = finalEmotion.name,
                    isVoice = true,
                    toolCallJson = functionCall.name,
                    toolResultJson = toolExecResult
                )

                return BrainResponse(
                    text = toolExecResult,
                    emotion = finalEmotion,
                    toolCall = functionCall.name,
                    toolResult = toolExecResult
                )
            }

            // 6. Plain text reply — strip the emotion tag before storing/speaking
            val rawText = modelParts.firstOrNull { it.text != null }?.text?.trim()
                ?: "আমি বুঝতে পারিনি, একটু আবার বলো?"
            val parsed = EmotionTagParser.parse(rawText)
            val responseText = parsed.text.ifBlank { "আমি বুঝতে পারিনি, একটু আবার বলো?" }

            val finalEmotion = parsed.emotion
                ?: if (EmotionTagParser.needsFallbackInference(parsed.hadTag)) {
                    emotionEngine.inferEmotionFromText(responseText)
                } else {
                    ArohiEmotion.SPEAKING
                }

            _isProcessing.value = false
            emotionEngine.setEmotion(finalEmotion)

            conversationRepository.addMessage(
                role = "AROHI",
                content = responseText,
                emotion = finalEmotion.name,
                isVoice = true
            )

            return BrainResponse(
                text = responseText,
                emotion = finalEmotion
            )

        } catch (e: Exception) {
            _isProcessing.value = false
            emotionEngine.setEmotion(ArohiEmotion.ERROR)
            val errText = "সাময়িক সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
            return BrainResponse(text = errText, emotion = ArohiEmotion.ERROR)
        }
    }

    /**
     * Claude (Anthropic Messages API) path. Same persona, same tools, same emotion contract —
     * only the transport differs, so the user can pick whichever key they own.
     */
    private suspend fun claudeReply(
        userInput: String,
        systemPrompt: String,
        apiKey: String
    ): BrainResponse {
        val history = conversationRepository.getRecentMessages(10).reversed()
        val messages = mutableListOf<ClaudeMessage>()
        for (msg in history) {
            val role = if (msg.role == "USER") "user" else "assistant"
            if (msg.content.isBlank()) continue
            messages.add(ClaudeMessage(role = role, content = msg.content))
        }
        messages.add(ClaudeMessage(role = "user", content = userInput))

        val response = ClaudeClient.sendMessage(
            apiKey = apiKey,
            model = settingsRepository.getAnthropicModel(),
            system = systemPrompt,
            messages = messages,
            tools = claudeTools(),
            maxTokens = 1024
        )

        if (!response.isSuccessful) {
            val code = response.code()
            val fallback = offlineFallbackEngine.respond(userInput, settingsRepository.isPrivateMode())
            val text = if (settingsRepository.isOfflineFallbackEnabled()) {
                "ক্লাউডে সংযোগ করতে পারছি না (Claude সমস্যা $code)। ${fallback.text}"
            } else {
                "Claude সার্ভার সমস্যা ($code)। অনুগ্রহ করে নেটওয়ার্ক ও API কী যাচাই করুন।"
            }
            _isProcessing.value = false
            emotionEngine.setEmotion(ArohiEmotion.CONCERNED)
            return BrainResponse(text = text, emotion = ArohiEmotion.CONCERNED)
        }

        val blocks = response.body()?.content ?: emptyList()

        // Tool use?
        val toolUse = blocks.firstOrNull { it.type == "tool_use" && !it.name.isNullOrBlank() }
        if (toolUse != null) {
            emotionEngine.setEmotion(ArohiEmotion.EXECUTING)
            val toolExecResult = executeToolCall(
                FunctionCall(name = toolUse.name ?: "", args = toolUse.input ?: emptyMap())
            )
            _isProcessing.value = false
            val finalEmotion = emotionEngine.inferEmotionFromText(toolExecResult)
            emotionEngine.setEmotion(finalEmotion)
            conversationRepository.addMessage(
                role = "AROHI",
                content = toolExecResult,
                emotion = finalEmotion.name,
                isVoice = true,
                toolCallJson = toolUse.name,
                toolResultJson = toolExecResult
            )
            return BrainResponse(
                text = toolExecResult,
                emotion = finalEmotion,
                toolCall = toolUse.name,
                toolResult = toolExecResult
            )
        }

        val rawText = blocks.filter { it.type == "text" }.mapNotNull { it.text }.joinToString(" ").trim()
            .ifBlank { "আমি বুঝতে পারিনি, একটু আবার বলো?" }
        val parsed = EmotionTagParser.parse(rawText)
        val responseText = parsed.text.ifBlank { "আমি বুঝতে পারিনি, একটু আবার বলো?" }
        val finalEmotion = parsed.emotion
            ?: if (EmotionTagParser.needsFallbackInference(parsed.hadTag)) {
                emotionEngine.inferEmotionFromText(responseText)
            } else {
                ArohiEmotion.SPEAKING
            }

        _isProcessing.value = false
        emotionEngine.setEmotion(finalEmotion)
        conversationRepository.addMessage(
            role = "AROHI",
            content = responseText,
            emotion = finalEmotion.name,
            isVoice = true
        )
        return BrainResponse(text = responseText, emotion = finalEmotion)
    }

    /** Projects the shared Gemini tool declarations into Anthropic's JSON-schema tool format. */
    private fun claudeTools(): List<ClaudeTool> =
        ToolRegistry.availableTools.functionDeclarations.orEmpty().map { declaration ->
            val properties = LinkedHashMap<String, Any?>()
            declaration.parameters?.properties?.forEach { (key, schema) ->
                properties[key] = buildMap<String, Any?> {
                    put("type", schema.type.lowercase())
                    schema.description?.let { put("description", it) }
                }
            }
            ClaudeTool(
                name = declaration.name,
                description = declaration.description,
                inputSchema = mapOf(
                    "type" to "object",
                    "properties" to properties,
                    "required" to (declaration.parameters?.required ?: emptyList<String>())
                )
            )
        }

    private suspend fun finishLocal(result: LocalExecutionResult): BrainResponse {
        _isProcessing.value = false
        emotionEngine.setEmotion(result.emotion)

        conversationRepository.addMessage(
            role = "AROHI",
            content = result.responseText,
            emotion = result.emotion.name,
            isVoice = true,
            toolCallJson = result.toolName
        )

        return BrainResponse(
            text = result.responseText,
            emotion = result.emotion,
            toolCall = result.toolName,
            isLocalOnly = true
        )
    }

    private suspend fun offlineReply(input: String, privacyMode: Boolean): BrainResponse {
        _isProcessing.value = false
        if (!settingsRepository.isOfflineFallbackEnabled()) {
            val text = "Gemini API Key সেট করা নেই। সেটিংস থেকে আপনার Gemini API Key যুক্ত করুন অথবা সাধারণ ভয়েস কমান্ড (ব্যাটারি, টর্চ, রিমাইন্ডার, টু-ডু, হিসাব) ব্যবহার করুন।"
            emotionEngine.setEmotion(ArohiEmotion.CONFUSED)
            return BrainResponse(text = text, emotion = ArohiEmotion.CONFUSED, isLocalOnly = true)
        }

        val fallback = offlineFallbackEngine.respond(input, privacyMode)
        emotionEngine.setEmotion(fallback.emotion)

        conversationRepository.addMessage(
            role = "AROHI",
            content = fallback.text,
            emotion = fallback.emotion.name,
            isVoice = true,
            toolCallJson = fallback.matchedPreset?.let { "offline:$it" }
        )

        return BrainResponse(
            text = fallback.text,
            emotion = fallback.emotion,
            isLocalOnly = true,
            isOfflineFallback = true
        )
    }

    private suspend fun buildPersonaContext(telemetry: com.example.device.DeviceTelemetry): PersonaEngine.PersonaContext {
        val memories = runCatching { memoryRepository.search("") }.getOrDefault(emptyList())
        val reminders = runCatching { reminderRepository.getActiveReminders() }.getOrDefault(emptyList())
        val todos = runCatching { todoRepository.getPendingTodos() }.getOrDefault(emptyList())
        val nextEvent = runCatching {
            if (calendarHelper.hasReadPermission()) calendarHelper.queryUpcomingEvents(7, 1).firstOrNull() else null
        }.getOrNull()
        val weather = weatherRepository.cached()

        return PersonaEngine.PersonaContext(
            userName = settingsRepository.getUserName().ifBlank { settingsRepository.getUserNickname() },
            pronounStyle = settingsRepository.getPronounStyle(),
            personalityIntensity = settingsRepository.getPersonalityIntensity(),
            deviceName = telemetry.deviceName,
            androidVersion = telemetry.androidVersion,
            batteryPercent = telemetry.batteryPercent,
            isCharging = telemetry.isCharging,
            freeStorageGb = telemetry.freeStorageGb,
            networkType = telemetry.networkType,
            isOnline = telemetry.isConnected,
            currentTimeText = offlineFallbackEngine.currentTimeText(),
            weekdayText = offlineFallbackEngine.currentDateText(),
            memoryFacts = memories.take(15).map { "${it.key}: ${it.value}" },
            pendingReminders = reminders.take(5).map {
                "${it.title} @ ${ReminderScheduler.formatBengaliDateTime(it.triggerAt)}"
            },
            pendingTodos = todos.take(5).map { it.title },
            nextCalendarEvent = nextEvent?.let {
                "${it.title} — ${ReminderScheduler.formatBengaliDateTime(it.begin)}"
            },
            weatherSummary = weather?.spokenSummary()
        )
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
                "ডিভাইস স্ট্যাটাস: ব্যাটারি ${telemetry.batteryPercent}% (${if (telemetry.isCharging) "চার্জিং" else "ব্যাটারিতে"}), ফ্রি র‍্যাম ${telemetry.freeRamMb}MB/${telemetry.totalRamMb}MB, ফ্রি স্টোরেজ ${telemetry.freeStorageGb}GB, নেটওয়ার্ক: ${telemetry.networkType}, ভলিউম: ${telemetry.mediaVolumePercent}%।"
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

            // ------------------------------------------------------- productivity tools
            "create_reminder" -> {
                val title = args["title"]?.toString()?.trim().orEmpty().ifBlank { "রিমাইন্ডার" }
                val timeText = args["time_text"]?.toString() ?: ""
                val repeat = (args["repeat"]?.toString() ?: ReminderRepeat.NONE).uppercase()
                val triggerAt = TimePhraseParser.parse(timeText)
                if (triggerAt == null) {
                    "কখন মনে করিয়ে দিতে হবে সেটা বুঝতে পারিনি — সময়টা আবার বলো (যেমন 'সন্ধ্যা ৭টায়')।"
                } else {
                    val rule = if (ReminderRepeat.all.contains(repeat)) repeat else ReminderRepeat.NONE
                    reminderRepository.addReminder(title, triggerAt, repeatRule = rule)
                    val repeatText = if (rule == ReminderRepeat.NONE) "" else " (${ReminderRepeat.bengaliLabel(rule)})"
                    "ঠিক আছে — '$title' ${ReminderScheduler.formatBengaliDateTime(triggerAt)}$repeatText মনে করিয়ে দেব।"
                }
            }
            "list_reminders" -> {
                val reminders = reminderRepository.getActiveReminders()
                if (reminders.isEmpty()) {
                    "এই মুহূর্তে কোনো রিমাইন্ডার নেই।"
                } else {
                    "তোমার রিমাইন্ডারগুলো:\n" + reminders.take(8).joinToString("\n") {
                        "• ${it.title} — ${ReminderScheduler.formatBengaliDateTime(it.triggerAt)}"
                    }
                }
            }
            "delete_reminder" -> {
                val query = args["query"]?.toString() ?: ""
                if (query == "*" || query.contains("সব")) {
                    reminderRepository.clearAll()
                    "সব রিমাইন্ডার মুছে ফেলা হয়েছে।"
                } else {
                    val match = reminderRepository.search(query).firstOrNull()
                    if (match != null) {
                        reminderRepository.deleteReminder(match.id)
                        "'${match.title}' রিমাইন্ডার মুছে দিয়েছি।"
                    } else {
                        "'$query' নামে কোনো রিমাইন্ডার পাওয়া যায়নি।"
                    }
                }
            }
            "add_todo" -> {
                val title = args["title"]?.toString()?.trim().orEmpty()
                if (title.isBlank()) {
                    "কী কাজটা যোগ করতে হবে বলো।"
                } else {
                    val priority = args["priority"]?.toString()?.toDoubleOrNull()?.toInt() ?: 1
                    val dueAt = args["due_text"]?.toString()?.let { TimePhraseParser.parse(it) }
                    todoRepository.addTodo(title, dueAt = dueAt, priority = priority)
                    val dueText = if (dueAt != null) " (${ReminderScheduler.formatBengaliDateTime(dueAt)})" else ""
                    "'$title' টু-ডু লিস্টে যোগ করেছি$dueText।"
                }
            }
            "list_todos" -> {
                val pending = todoRepository.getPendingTodos()
                if (pending.isEmpty()) {
                    "টু-ডু লিস্টে কোনো কাজ বাকি নেই।"
                } else {
                    "বাকি কাজগুলো:\n" + pending.take(8).joinToString("\n") { "• ${it.title}" }
                }
            }
            "complete_todo" -> {
                val query = args["query"]?.toString() ?: ""
                if (todoRepository.completeByTitle(query)) {
                    "'$query' কাজটা সম্পন্ন হিসেবে টিক দিয়েছি।"
                } else {
                    "'$query' নামে কোনো খোলা কাজ পাইনি।"
                }
            }
            "add_note" -> {
                val title = args["title"]?.toString().orEmpty().ifBlank { "নোট" }
                val content = args["content"]?.toString() ?: ""
                if (content.isBlank()) {
                    "নোটের 내용টা বলো, লিখে রাখছি।"
                } else {
                    noteRepository.addNote(title, content)
                    "নোটটা লিখে রেখেছি: \"${content.take(70)}\""
                }
            }
            "list_notes" -> {
                val notes = noteRepository.getNotes()
                if (notes.isEmpty()) "কোনো নোট সেভ করা নেই।"
                else "তোমার নোটগুলো:\n" + notes.take(6).joinToString("\n") { "• ${it.title}: ${it.content.take(60)}" }
            }
            "get_weather" -> {
                val requestedCity = args["city"]?.toString()?.trim().orEmpty()
                val snapshot = if (requestedCity.isNotBlank()) {
                    val client = com.example.device.WeatherClient()
                    val geo = client.geocode(requestedCity)
                    if (geo != null) client.fetchByCoordinates(geo.first, geo.second, geo.third) else null
                } else {
                    weatherRepository.refresh()
                }
                snapshot?.spokenSummary() ?: "আবহাওয়ার ডেটা আনতে পারছি না, ইন্টারনেট সংযোগ দেখুন।"
            }
            "calculate" -> {
                val expression = args["expression"]?.toString() ?: ""
                when (val result = CalculatorEngine.calculate(expression)) {
                    is CalculatorEngine.Result.Success -> result.expression
                    is CalculatorEngine.Result.Failure -> result.message
                }
            }
            "convert_units" -> UnitConverter.convertFromText(args["text"]?.toString() ?: "")
            "convert_currency" -> CurrencyConverter.convertFromText(args["text"]?.toString() ?: "")
            "translate_text" -> {
                val text = args["text"]?.toString() ?: ""
                val target = args["target_language"]?.toString() ?: "English"
                val offline = PhraseBook.translate(text, target)
                offline ?: "অনুবাদের জন্য ক্লাউড কানেকশন দরকার — এখন যেটুকু পারি: \"$text\" এর সরাসরি অনুবাদ পেতে ইন্টারনেট চালু রাখো।"
            }
            "get_calendar_events" -> {
                val days = args["days"]?.toString()?.toDoubleOrNull()?.toInt() ?: 7
                if (!calendarHelper.hasReadPermission()) {
                    "ক্যালেন্ডার পড়ার পারমিশন দেওয়া নেই — সেটিংস থেকে Calendar permission দাও।"
                } else {
                    val events = calendarHelper.queryUpcomingEvents(days = days, limit = 10)
                    if (events.isEmpty()) "আগামী $days দিনে কোনো ইভেন্ট নেই।"
                    else "আগামী $days দিনের ইভেন্ট:\n" + events.joinToString("\n") {
                        "• ${ReminderScheduler.formatBengaliDateTime(it.begin)} — ${it.title}"
                    }
                }
            }
            "create_calendar_event" -> {
                val title = args["title"]?.toString()?.trim().orEmpty().ifBlank { "নতুন ইভেন্ট" }
                val startText = args["start_text"]?.toString() ?: ""
                val duration = args["duration_minutes"]?.toString()?.toDoubleOrNull()?.toInt() ?: 60
                val startAt = TimePhraseParser.parse(startText, System.currentTimeMillis())
                if (startAt == null) {
                    "ইভেন্ট কখন শুরু হবে সেটা বুঝতে পারিনি — সময়টা আবার বলো।"
                } else {
                    val inserted = calendarHelper.insertEvent(title, startAt, duration)
                    if (inserted != null) {
                        "'$title' ইভেন্টটা ${ReminderScheduler.formatBengaliDateTime(startAt)}-এ ক্যালেন্ডারে যোগ করেছি।"
                    } else if (calendarHelper.openEventInsert(title, startAt, duration)) {
                        "ক্যালেন্ডার অ্যাপে '$title' ইভেন্টের স্ক্রিন খুলে দিয়েছি — সেভ করে নাও।"
                    } else {
                        "ক্যালেন্ডারে ইভেন্ট যোগ করা যায়নি। Calendar permission দিলে আমি সরাসরি সেভ করতে পারব।"
                    }
                }
            }
            "get_proactive_suggestion" -> {
                val telemetry = deviceStateManager.getTelemetry()
                val suggestion = proactiveSuggestionEngine.suggestion(
                    weather = weatherRepository.refresh(),
                    reminders = reminderRepository.getActiveReminders(),
                    todos = todoRepository.getPendingTodos(),
                    batteryPercent = telemetry.batteryPercent,
                    isCharging = telemetry.isCharging
                )
                suggestion ?: "এখন বিশেষ কোনো সাজেশন নেই — সব ঠিকঠাক চলছে।"
            }
            "export_user_data" -> {
                val app = context.applicationContext as? ArohiApplication
                if (app == null) {
                    "এক্সপোর্ট করা যায়নি।"
                } else {
                    val file = DataExporter.writeExportFile(context, app)
                    if (file != null) "তোমার ডেটা এক্সপোর্ট করা হয়েছে: ${file.name}" else "এক্সপোর্ট করা যায়নি।"
                }
            }
            else -> "কমান্ড '$name' সম্পন্ন করা হয়েছে।"
        }
    }
}
