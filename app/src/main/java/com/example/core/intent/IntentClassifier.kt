package com.example.core.intent

import com.example.core.text.TextProcessor

/** The set of intents Arohi can recognize locally before reaching the cloud brain. */
enum class IntentType {
    // Voice control
    STOP_SPEAKING, CANCEL, REPEAT, WAKE,
    // Identity / meta
    IDENTITY, HELP, RUN_DIAGNOSTICS, RUN_SELF_TEST, OPEN_SETUP, LIST_CAPABILITIES,
    // Device telemetry & control
    BATTERY_STATUS, STORAGE_STATUS, NETWORK_STATUS, DEVICE_TELEMETRY,
    FLASHLIGHT, VOLUME_CONTROL, BRIGHTNESS, SOUND_MODE,
    // Navigation / global actions
    NAV_BACK, NAV_HOME, NAV_RECENTS, OPEN_NOTIFICATION_PANEL, OPEN_QUICK_SETTINGS,
    // Screen agent
    INSPECT_SCREEN, CLICK_ELEMENT, SCROLL, READ_SCREEN,
    // Apps & phone
    OPEN_APP, CALL_CONTACT, SEND_SMS, SEND_WHATSAPP,
    // Time engines
    SET_TIMER, SET_ALARM, CREATE_REMINDER, LIST_TIMERS, CANCEL_TIMER,
    // Notifications
    READ_NOTIFICATIONS, SUMMARIZE_NOTIFICATIONS,
    // Memory
    MEMORY_SAVE, MEMORY_SEARCH, MEMORY_FORGET,
    // Routines / custom commands / modes
    ROUTINE_LIST, ACTIVATE_MODE,
    // Web & translation
    WEB_SEARCH, TRANSLATE, OPEN_MAPS,
    // Safety
    EMERGENCY,
    // Nothing matched → defer to cloud AI
    CHAT
}

/** A recognized command with extracted slots. [confidence] is 0..1. */
data class RecognizedIntent(
    val type: IntentType,
    val rawText: String,
    val confidence: Float,
    val slots: Map<String, String> = emptyMap(),
    /** True for sensitive/destructive intents that must be confirmed. */
    val requiresConfirmation: Boolean = false
) {
    fun slot(key: String): String? = slots[key]
}

/**
 * Deterministic, multilingual intent classifier. It deliberately runs before
 * the network AI so that local commands (timer, torch, app launch, …) work
 * offline and instantly. Anything unmatched yields [IntentType.CHAT] and flows
 * to the AI provider. No result is fabricated — this only classifies.
 */
class IntentClassifier(
    /** App alias map (lowercase alias → real app label) supplied by AppDiscoveryManager. */
    private val appAliases: Map<String, String> = emptyMap()
) {

    private fun rules(): List<Rule> = listOf(
        // ---- Voice control ----
        Rule(IntentType.STOP_SPEAKING, 0.97f,
            kw("চুপ কর", "চুপ থাক", "থামো", "থাম", "stop talking", "be quiet", "silence", "শান্ত হও")),
        Rule(IntentType.CANCEL, 0.95f,
            kw("বাতিল কর", "cancel", "না কর", "don't do", "বন্ধ করে দাও")),
        Rule(IntentType.REPEAT, 0.9f,
            kw("আবার বল", "repeat", "say that again", "শেষ কথাটা", "আবার কর", "do it again")),

        // ---- Meta ----
        Rule(IntentType.IDENTITY, 0.9f,
            kw("তুমি কে", "তোর নাম", "who are you", "what is your name", "তুমি কি করতে পার", "what can you do")),
        Rule(IntentType.HELP, 0.9f,
            kw("সাহায্য", "help me", "help", "কীভাবে", "how do i")),
        Rule(IntentType.RUN_SELF_TEST, 0.95f,
            kw("self test", "full test", "system test", "পুরো টেস্ট", "সিস্টেম টেস্ট", "নিজেকে যাচাই")),
        Rule(IntentType.RUN_DIAGNOSTICS, 0.9f,
            kw("diagnostic", "health check", "সিস্টেম হেলথ", "ডায়াগনস্টিক", "কেমন আছিস", "তুমি ঠিক আছ")),
        Rule(IntentType.OPEN_SETUP, 0.9f,
            kw("setup arohi", "fix arohi", "সেটআপ", "ঠিক কর", "repair", "মেরামত")),
        Rule(IntentType.LIST_CAPABILITIES, 0.85f,
            kw("what can you do", "capabilit", "কী কী করতে পার", "ফিচার", "feature list")),

        // ---- Device telemetry ----
        Rule(IntentType.BATTERY_STATUS, 0.9f,
            kw("ব্যাটারি", "চার্জ কত", "battery", "charging", "কত পারসেন্ট")),
        Rule(IntentType.STORAGE_STATUS, 0.9f,
            kw("স্টোরেজ", "স্পেস", "জায়গা", "storage", "free space", "র‍্যাম", "ram", "মেমোরি কত")),
        Rule(IntentType.NETWORK_STATUS, 0.9f,
            kw("নেটওয়ার্ক", "ইন্টারনেট", "wifi", "wi-fi", "network", "internet", "ডাটা চালু")),
        Rule(IntentType.DEVICE_TELEMETRY, 0.8f,
            kw("ফোনের অবস্থা", "device status", "সব দেখাও", "system status", "টেলিমেট্রি")),

        // ---- Device control ----
        Rule(IntentType.FLASHLIGHT, 0.92f,
            kw("টর্চ", "ফ্ল্যাশলাইট", "flashlight", "torch", "flash"),
            extractor = { mapOf("enable" to if (it.contains("বন্ধ") || it.contains("off") || it.contains("অফ")) "false" else "true") }),
        Rule(IntentType.VOLUME_CONTROL, 0.9f,
            kw("ভলিউম", "ভল্যুম", "শব্দ", "সাউন্ড", "volume", "sound", "audio", "মিউট", "mute")),
        Rule(IntentType.BRIGHTNESS, 0.88f,
            kw("ব্রাইটনেস", "উজ্জ্বলতা", "brightness", "screen light")),
        Rule(IntentType.SOUND_MODE, 0.85f,
            kw("সাইলেন্ট", "vibrate", "ভাইব্রেট", "do not disturb", "dnd", "ডিএনডি", "সাইলেন্ট মোড")),

        // ---- Global navigation ----
        Rule(IntentType.NAV_BACK, 0.92f, kw("পিছনে যাও", "পেছনে", "ব্যাক", "go back", "back")),
        Rule(IntentType.NAV_HOME, 0.92f, kw("হোমে যাও", "হোম স্ক্রিন", "go home", "home screen")),
        Rule(IntentType.NAV_RECENTS, 0.9f, kw("রিসেন্ট", "সাম্প্রতিক অ্যাপ", "recent apps", "recent", "ওপেন অ্যাপ")),
        Rule(IntentType.OPEN_NOTIFICATION_PANEL, 0.9f, kw("নোটিফিকেশন বার", "নোটিফিকেশন নামাও", "notification panel", "open notifications")),
        Rule(IntentType.OPEN_QUICK_SETTINGS, 0.9f, kw("কুইক সেটিংস", "quick settings", "কন্ট্রোল প্যানেল")),

        // ---- Screen agent ----
        Rule(IntentType.READ_SCREEN, 0.9f, kw("স্ক্রিনে কি", "স্ক্রিন পড়ো", "স্ক্রিনে কী", "read screen", "what is on screen", "what's on screen")),
        Rule(IntentType.INSPECT_SCREEN, 0.75f, kw("inspect screen", "ui tree", "স্ক্রিন inspect")),
        Rule(IntentType.CLICK_ELEMENT, 0.9f,
            kw("চাপো", "চাপ দাও", "ট্যাপ", "ক্লিক", "click", "tap", "press", "খুঁজে চাপ", "button চাপ"),
            extractor = { mapOf("query" to extractClickTarget(it)) },
            needsService = true),
        Rule(IntentType.SCROLL, 0.8f,
            kw("স্ক্রল", "scroll", "নিচে নামাও", "উপরে ওঠাও", "swipe up", "swipe down")),

        // ---- Phone ----
        Rule(IntentType.CALL_CONTACT, 0.9f,
            kw("কল দাও", "কল কর", "call ", "ডায়াল", "dial", "ফোন কর", "ring "),
            extractor = { mapOf("target" to extractCallTarget(it)) },
            needsConfirm = true),
        Rule(IntentType.SEND_SMS, 0.88f,
            kw("sms", "এসএমএস", "message পাঠা", "মেসেজ পাঠা", "text ", "টেক্সট পাঠা"),
            extractor = { mapOf("target" to "", "message" to it) },
            needsConfirm = true),
        Rule(IntentType.SEND_WHATSAPP, 0.85f,
            kw("whatsapp", "হোয়াটসঅ্যাপ", "ওয়াটসঅ্যাপ", "মেসেঞ্জার না"),
            extractor = { mapOf("message" to it) },
            needsConfirm = true),

        // ---- Time engines ----
        // Note: cancel/list must rank ABOVE set_timer so phrases that also contain
        // the word "timer" are routed correctly (specificity over generic match).
        Rule(IntentType.CANCEL_TIMER, 0.97f, kw("timer বন্ধ", "timer cancel", "টাইমার বন্ধ", "stop the timer", "timer stop", "timer off")),
        Rule(IntentType.LIST_TIMERS, 0.96f, kw("timer কয়টা", "list timers", "টাইমারগুলো", "active timers", "চলমান টাইমার", "timers আছে")),
        Rule(IntentType.SET_TIMER, 0.93f, kw("timer", "টাইমার"), isTimer = true),
        Rule(IntentType.SET_ALARM, 0.9f, kw("alarm", "অ্যালার্ম", "অ্যালার্ম দাও", "ঘুম থেকে", "wake me up"), isAlarm = true),
        Rule(IntentType.CREATE_REMINDER, 0.9f,
            kw("remind me", "reminder", "মনে করিয়ে", "মনে করাই", "স্মরণ", "খেয়াল রেখ", "remember to"),
            needsConfirm = false),

        // ---- Notifications ----
        Rule(IntentType.SUMMARIZE_NOTIFICATIONS, 0.9f,
            kw("notification summarize", "নোটিফিকেশন সামারি", "নোটিফিকেশনগুলো সার", "summarize notification", "নোটিফিকেশন summarize")),
        Rule(IntentType.READ_NOTIFICATIONS, 0.85f,
            kw("notification", "নোটিফিকেশন", "কে মেসেজ দিয়েছে", "কে মেসেজ দিলো", "any messages")),

        // ---- Memory ----
        Rule(IntentType.MEMORY_SAVE, 0.88f,
            kw("মনে রাখ", "remember this", "remember that", "save this", "নোট কর", "লিখে রাখ", "জেনে রাখ")),
        Rule(IntentType.MEMORY_FORGET, 0.9f,
            kw("ভুলে যাও", "forget", "মুছে ফেল", "delete memory", "মনে রাখা বাদ"),
            needsConfirm = true),
        Rule(IntentType.MEMORY_SEARCH, 0.7f,
            kw("আমার সম্পর্কে", "what do you remember", "তুমি কি জানো", "আমি কী বলেছিলাম", "আমি কি বলেছিলাম")),

        // ---- Routines / modes ----
        Rule(IntentType.ROUTINE_LIST, 0.85f,
            kw("routine দেখাও", "routines", "রুটিনগুলো", "saved routine", "রুটিন লিস্ট", "custom command")),
        Rule(IntentType.ACTIVATE_MODE, 0.82f,
            kw("study mode", "স্টাডি মোড", "sleep mode", "ঘুম মোড", "driving mode", "ড্রাইভিং মোড",
                "meeting mode", "মিটিং মোড", "travel mode", "ট্রাভেল মোড", "productivity", "emergency mode")),

        // ---- Web / translation / maps ----
        Rule(IntentType.WEB_SEARCH, 0.8f,
            kw("search ", "গুগলে খুঁজ", "google", "খুঁজে দাও", "what is", "who is", "research", "রিসার্চ কর",
                "weather", "আবহাওয়া", "news", "খবর")),
        Rule(IntentType.TRANSLATE, 0.88f,
            kw("translate", "অনুবাদ", "ইংরেজিতে", "english এ", "বাংলায়", "বাংলা কর", "ইংরেজি কর")),
        Rule(IntentType.OPEN_MAPS, 0.85f,
            kw("maps", "ম্যাপ", "নেভিগেশন", "navigation", "দিক নির্দেশনা", "কীভাবে যাব", "বাসায় যাচ্ছি", "route", "নিকটবর্তী")),

        // ---- Safety ----
        Rule(IntentType.EMERGENCY, 0.95f,
            kw("emergency", "জরুরি", "আপদকালীন", "sos", "help me now", "বিপদ"),
            needsConfirm = true),

        // ---- Open app (broad, low priority) ----
        Rule(IntentType.OPEN_APP, 0.7f,
            kw("খোলো", "খুলে দাও", "ওপেন", "open ", "launch", "চালু কর", "start app"),
            extractor = { mapOf("app" to extractAppName(it)) })
    )

    fun classify(rawInput: String): RecognizedIntent {
        val normalized = TextProcessor.normalize(rawInput)
        val body = TextProcessor.stripWakeWord(normalized)
        val lower = body.lowercase()

        var best: Rule? = null
        for (rule in rules()) {
            if (rule.matches(lower, body)) {
                if (best == null || rule.priority > best.priority) best = rule
            }
        }

        val rule = best
        if (rule == null) {
            return RecognizedIntent(IntentType.CHAT, body, 0.1f)
        }

        var slots = rule.extractor?.invoke(body) ?: emptyMap()
        // Timer/alarm get structured slots from the duration/time parsers.
        slots = when (rule.type) {
            IntentType.SET_TIMER -> slots + mapOf("raw" to body)
            IntentType.SET_ALARM -> slots + mapOf("raw" to body)
            IntentType.CREATE_REMINDER -> slots + mapOf("raw" to body)
            else -> slots
        }

        return RecognizedIntent(
            type = rule.type,
            rawText = body,
            confidence = rule.priority,
            slots = slots,
            requiresConfirmation = rule.needsConfirm
        )
    }

    private class Rule(
        val type: IntentType,
        val priority: Float,
        val keywords: List<String>,
        val extractor: ((String) -> Map<String, String>)? = null,
        val needsConfirm: Boolean = false,
        val needsService: Boolean = false,
        val isTimer: Boolean = false,
        val isAlarm: Boolean = false
    ) {
        fun matches(lower: String, body: String): Boolean {
            // Timer/alarm keywords shouldn't double-fire the broader "open app" rule.
            return keywords.any { kw -> lower.contains(kw) }
        }
    }

    private fun kw(vararg words: String): List<String> = words.toList()

    // --- Slot extractors -----------------------------------------------------

    private fun extractAppName(text: String): String {
        val t = text.lowercase()
        // Remove action words, keep the app target.
        var name = t
        for (w in listOf("খোলো", "খুলে দাও", "ওপেন", "open", "launch", "চালু করো", "চালু কর",
            "start", "অ্যাপ", "app", "অরোহী", "আরোহী", "arohi", ",", "please", "করো", "করে দাও")) {
            name = name.replace(w, " ")
        }
        return TextProcessor.normalize(name)
    }

    private fun extractCallTarget(text: String): String {
        val t = text.lowercase()
        var target = t
        for (w in listOf("কল দাও", "কল করো", "কল কর", "ফোন করো", "ফোন কর", "call", "dial",
            "ডায়াল করো", "করার জন্য", "কে", "to", "please", "অরোহী", "আরোহী", "arohi")) {
            target = target.replace(w, " ")
        }
        return TextProcessor.normalize(target)
    }

    private fun extractClickTarget(text: String): String {
        val t = text.lowercase()
        var q = t
        for (w in listOf("চাপো", "চাপ দাও", "ট্যাপ করো", "ক্লিক করো", "click", "tap", "press",
            "খুঁজে", "বাটন", "button", "the", "অরোহী", "আরোহী", "arohi")) {
            q = q.replace(w, " ")
        }
        return TextProcessor.normalize(q)
    }
}
