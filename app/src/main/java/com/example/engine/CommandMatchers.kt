package com.example.engine

import java.util.Locale

/**
 * Pure command-matching logic for the local (offline) command engine.
 * No Android dependencies — fully unit-testable on the JVM.
 */
object CommandMatchers {

    // Phrases that genuinely mean "stop talking". Anything mentioning a
    // controllable device/service is excluded so real commands still work.
    private val SILENCE_PHRASES = listOf(
        "চুপ করো", "চুপ কর", "চুপ", "থামো", "থামা যাক", "আর বলো না", "ব্যাস", "বন্ধ করো কথা",
        "shut up", "be quiet", "stop talking", "stop speaking", "quiet", "silence", "stop"
    )

    private val SILENCE_EXCLUDED_KEYWORDS = listOf(
        "টর্চ", "ফ্ল্যাশ", "flashlight", "torch", "ভলিউম", "volume", "সাউন্ড", "sound",
        "মিউট", "mute", "মিউজিক", "music", "গান", "song", "ভিডিও", "video", "media",
        "অ্যালার্ম", "এলার্ম", "alarm", "টাইমার", "timer", "স্টপওয়াচ", "stopwatch",
        "রেকর্ড", "record", "কল", "call", "অ্যাপ", "app", "নোটিফিকেশন", "notification",
        "রুটিন", "routine", "টাস্ক", "task"
    )

    private val MEMORY_SAVE_TRIGGERS = listOf(
        "মনে রেখো", "মনে রাখো", "মনে রাখবে যে", "মনে রাখবে", "remember that", "remember this", "remember:"
    )

    private val MEMORY_RECALL_TRIGGERS = listOf(
        "কী মনে আছে", "কি মনে আছে", "মনে আছে কি", "মনে আছে কী", "তোমার মেমোরিতে কী আছে",
        "আমার সম্পর্কে কী জানো", "আমার কথা কী জানো", "what do you remember",
        "what do you know about me", "আমার মেমোরি দেখাও"
    )

    /** Strict silence-command matcher — device commands are never hijacked. */
    fun isSilenceCommand(text: String): Boolean {
        val t = text.trim().lowercase(Locale.ROOT)
        if (t.isEmpty()) return false
        if (t.split(Regex("\\s+")).size > 5) return false
        if (SILENCE_EXCLUDED_KEYWORDS.any { t.contains(it) }) return false
        return SILENCE_PHRASES.any { phrase ->
            t == phrase || t.startsWith("$phrase ") || t.startsWith("$phrase!") || t.startsWith("$phrase।")
        }
    }

    fun isMemorySaveQuery(text: String): Boolean {
        val t = text.trim().lowercase(Locale.ROOT)
        return MEMORY_SAVE_TRIGGERS.any { t.startsWith(it) }
    }

    fun isMemoryRecallQuery(text: String): Boolean {
        val t = text.trim().lowercase(Locale.ROOT)
        return MEMORY_RECALL_TRIGGERS.any { t.contains(it) }
    }

    fun extractMemoryFact(query: String): String {
        val lower = query.trim().lowercase(Locale.ROOT)
        for (trigger in MEMORY_SAVE_TRIGGERS) {
            if (lower.startsWith(trigger)) {
                return query.trim().substring(trigger.length).trim(' ', '।', '.', '!', ':')
            }
        }
        return ""
    }
}
