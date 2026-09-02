package com.example.engine

import com.example.core.intent.IntentClassifier
import com.example.core.intent.IntentType
import com.example.core.intent.RecognizedIntent
import com.example.core.permissions.PermissionManager
import com.example.core.personality.EmotionalContext
import com.example.core.personality.PersonalityEngine
import com.example.core.result.ArohiResult
import com.example.data.repository.MemoryRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.SettingsRepository
import com.example.service.ArohiAccessibilityService
import com.example.system.ArohiDiagnostics
import com.example.time.AlarmClockEngine
import com.example.time.TimerEngine

/**
 * Result of running one command through the [CommandOrchestrator]. When
 * [handled] is false the caller should defer to the cloud brain. Permission /
 * confirmation needs are surfaced explicitly (never faked).
 */
data class CommandResult(
    val handled: Boolean,
    val text: String,
    val emotion: ArohiEmotion = ArohiEmotion.SPEAKING,
    val toolName: String? = null,
    val needsPermission: Boolean = false,
    val needsConfirmation: Boolean = false
)

/**
 * The real universal command pipeline for locally-executable actions (spec §4).
 * It classifies intent, checks capability + permission, executes the REAL
 * action, and verifies. Anything it cannot genuinely do is either reported with
 * the correct status or left unhandled for the cloud AI.
 */
class CommandOrchestrator(
    private val classifier: IntentClassifier,
    private val permissionManager: PermissionManager,
    private val personalityEngine: PersonalityEngine,
    private val timerEngine: TimerEngine,
    private val alarmClockEngine: AlarmClockEngine,
    private val diagnostics: ArohiDiagnostics,
    private val memoryRepository: MemoryRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository
) {

    suspend fun handle(rawInput: String): CommandResult {
        val intent = classifier.classify(rawInput)
        if (intent.type == IntentType.CHAT || intent.confidence < 0.6f) {
            return CommandResult(false, "")
        }

        val emotionalContext = personalityEngine.detectContext(rawInput)
        val prefix = personalityEngine.adaptivePrefix(emotionalContext)

        return try {
            when (intent.type) {
                IntentType.SET_TIMER -> handleTimer(intent, prefix)
                IntentType.CANCEL_TIMER -> handleCancelTimer(prefix)
                IntentType.LIST_TIMERS -> handleListTimers(prefix)
                IntentType.SET_ALARM -> handleAlarm(intent, prefix)
                IntentType.RUN_DIAGNOSTICS, IntentType.RUN_SELF_TEST -> handleDiagnostics(prefix)
                IntentType.MEMORY_SAVE -> handleMemorySave(intent, prefix)
                IntentType.SUMMARIZE_NOTIFICATIONS, IntentType.READ_NOTIFICATIONS ->
                    handleNotifications(intent, prefix)
                IntentType.INSPECT_SCREEN, IntentType.READ_SCREEN, IntentType.CLICK_ELEMENT,
                IntentType.NAV_BACK, IntentType.NAV_HOME, IntentType.NAV_RECENTS,
                IntentType.SCROLL, IntentType.OPEN_NOTIFICATION_PANEL ->
                    handleAccessibility(intent, prefix)
                IntentType.LIST_CAPABILITIES, IntentType.HELP -> CommandResult(false, "")
                else -> CommandResult(false, "")
            }
        } catch (e: Exception) {
            CommandResult(
                handled = true,
                text = prefix + "কমান্ডটি চালাতে সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}।",
                emotion = ArohiEmotion.ERROR,
                toolName = "error"
            )
        }
    }

    private fun handleTimer(intent: RecognizedIntent, prefix: String): CommandResult {
        val result: ArohiResult<com.example.time.ActiveTimer> =
            timerEngine.startFromText(intent.rawText, label = "Timer")
        return when {
            result.succeeded -> CommandResult(true, prefix + result.message, ArohiEmotion.HAPPY, "set_timer")
            result.status == com.example.core.result.StatusCode.REQUIRES_CONFIRMATION ->
                CommandResult(true, prefix + result.message, ArohiEmotion.CONFUSED, "set_timer", needsConfirmation = true)
            else -> CommandResult(true, prefix + result.message, ArohiEmotion.CONFUSED, "set_timer")
        }
    }

    private fun handleCancelTimer(prefix: String): CommandResult {
        val cancelled = timerEngine.cancelAll()
        return if (cancelled > 0) {
            CommandResult(true, prefix + "সব টাইমার বাতিল করা হয়েছে ($cancelled টি)।", ArohiEmotion.CALM, "cancel_timer")
        } else {
            CommandResult(true, prefix + "চলমান কোনো টাইমার নেই।", ArohiEmotion.CONFUSED, "cancel_timer")
        }
    }

    private fun handleListTimers(prefix: String): CommandResult {
        val active = timerEngine.timers.value.filter { it.state == com.example.time.TimerState.RUNNING }
        if (active.isEmpty()) {
            return CommandResult(true, prefix + "এখন কোনো চলমান টাইমার নেই।", ArohiEmotion.CALM, "list_timers")
        }
        val list = active.joinToString("\n") {
            "• ${it.label}: ${TimerEngine.formatDuration(it.remainingMillis)} বাকি"
        }
        return CommandResult(true, prefix + "চলমান টাইমার:\n$list", ArohiEmotion.FOCUSED, "list_timers")
    }

    private fun handleAlarm(intent: RecognizedIntent, prefix: String): CommandResult {
        val clock = AlarmClockEngine.parseAlarmTime(intent.rawText)
        return if (clock == null) {
            CommandResult(
                true,
                prefix + "কয়টায় অ্যালার্ম দিতে হবে তা বুঝতে পারিনি। যেমন: 'সকাল ৭টায় অ্যালার্ম দাও'।",
                ArohiEmotion.CONFUSED,
                "set_alarm"
            )
        } else {
            val result = alarmClockEngine.setAlarm(clock.hour24, clock.minute, label = "Arohi alarm")
            CommandResult(
                true,
                prefix + result.message,
                if (result.succeeded) ArohiEmotion.HAPPY else ArohiEmotion.ERROR,
                "set_alarm"
            )
        }
    }

    private suspend fun handleDiagnostics(prefix: String): CommandResult {
        val report = diagnostics.runFullReport()
        val text = buildString {
            append(prefix)
            appendLine("সিস্টেম সেলফ-টেস্ট সম্পন্ন (${report.durationMs / 1000.0}s):")
            appendLine(report.summary())
            // Surface the most important actionable items.
            report.failed.take(2).forEach { appendLine("• ${it.component.displayName}: সমস্যা — ${it.detail}") }
            report.warnings.take(3).forEach { appendLine("• ${it.component.displayName}: ${it.detail}") }
        }.trim()
        val emotion = when (report.overall) {
            com.example.core.diagnostics.DiagnosticStatus.PASS -> ArohiEmotion.HAPPY
            com.example.core.diagnostics.DiagnosticStatus.WARNING -> ArohiEmotion.CONCERNED
            else -> ArohiEmotion.ERROR
        }
        return CommandResult(true, text, emotion, "run_self_test")
    }

    private suspend fun handleMemorySave(intent: RecognizedIntent, prefix: String): CommandResult {
        // Extract the fact text after the "remember" cue.
        var fact = intent.rawText
        for (w in listOf("মনে রাখো", "মনে রাখ", "remember that", "remember this", "remember",
            "নোট করো", "লিখে রাখো", "জেনে রাখো", "যে", "that", ":")) {
            fact = fact.replace(w, " ", ignoreCase = true)
        }
        fact = fact.trim()
        if (fact.length < 2) {
            return CommandResult(true, prefix + "কী মনে রাখব তা বলুন।", ArohiEmotion.CONFUSED, "memory_save")
        }
        memoryRepository.saveMemory("FACTS", "fact_${System.currentTimeMillis()}", fact)
        return CommandResult(true, prefix + "মনে রেখেছি: \"$fact\"", ArohiEmotion.HAPPY, "save_user_memory")
    }

    private suspend fun handleNotifications(intent: RecognizedIntent, prefix: String): CommandResult {
        // Notification reading requires notification listener access.
        val accessEnabled = permissionManager.requirementById("notification_access")?.let {
            permissionManager.isGranted(it)
        } == true
        val recent = notificationRepository.getRecentUnread(8)
        return if (recent.isEmpty()) {
            if (!accessEnabled) {
                CommandResult(
                    true,
                    prefix + "নোটিফিকেশন অ্যাক্সেস সক্রিয় নেই। সেটআপ থেকে Notification Access দিন, তাহলে নোটিফিকেশন পড়তে ও summarize করতে পারব।",
                    ArohiEmotion.CONCERNED, "read_notifications", needsPermission = true
                )
            } else {
                CommandResult(true, prefix + "কোনো সাম্প্রতিক নোটিফিকেশন নেই।", ArohiEmotion.CALM, "read_notifications")
            }
        } else {
            val summary = recent.joinToString("\n") { "• ${it.appName}: ${it.title} — ${it.text}" }
            CommandResult(true, prefix + "সাম্প্রতিক নোটিফিকেশন:\n$summary", ArohiEmotion.FOCUSED, "read_notifications")
        }
    }

    private fun handleAccessibility(intent: RecognizedIntent, prefix: String): CommandResult {
        val service = ArohiAccessibilityService.instance
        if (service == null) {
            return CommandResult(
                true,
                prefix + "এই কাজের জন্য Accessibility Service দরকার। সেটআপ/সেটিংস থেকে Arohi Accessibility চালু করুন।",
                ArohiEmotion.CONCERNED,
                intent.type.name.lowercase(),
                needsPermission = true
            )
        }
        val response = when (intent.type) {
            IntentType.NAV_BACK -> { service.goBack(); "পিছনে যাওয়া হয়েছে।" }
            IntentType.NAV_HOME -> { service.goHome(); "হোম স্ক্রিনে যাওয়া হয়েছে।" }
            IntentType.NAV_RECENTS -> { service.openRecents(); "সাম্প্রতিক অ্যাপ খোলা হয়েছে।" }
            IntentType.OPEN_NOTIFICATION_PANEL -> { service.openNotifications(); "নোটিফিকেশন প্যানেল খোলা হয়েছে।" }
            IntentType.SCROLL -> {
                val down = intent.rawText.contains("নিচে") || intent.rawText.contains("down")
                if (down) service.scrollDown() else service.scrollUp()
                "স্ক্রল করা হয়েছে।"
            }
            IntentType.INSPECT_SCREEN, IntentType.READ_SCREEN ->
                "স্ক্রিনে যা দেখা যাচ্ছে:\n" + service.inspectCurrentScreen()
            IntentType.CLICK_ELEMENT -> {
                val query = intent.slot("query").orEmpty()
                val clicked = service.clickByText(query)
                if (clicked) "'$query' চাপা হয়েছে।" else "'$query' খুঁজে পাওয়া যায়নি বা চাপা যায়নি।"
            }
            else -> ""
        }
        return CommandResult(true, prefix + response, ArohiEmotion.FOCUSED, intent.type.name.lowercase())
    }
}
