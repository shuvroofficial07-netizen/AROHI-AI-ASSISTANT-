package com.example.engine

import java.util.Locale

/**
 * Offline translation helper. Covers the everyday phrases people actually use with AROHI and
 * falls back to word-by-word lookup. Anything outside this book is handed to the cloud brain.
 */
object PhraseBook {

    private val phrases = listOf(
        Triple("কেমন আছো", "how are you", "bn-en"),
        Triple("আমি ভালো আছি", "i am fine", "bn-en"),
        Triple("ধন্যবাদ", "thank you", "bn-en"),
        Triple("শুভ সকাল", "good morning", "bn-en"),
        Triple("শুভ রাত্রি", "good night", "bn-en"),
        Triple("দুঃখিত", "sorry", "bn-en"),
        Triple("তোমার নাম কি", "what is your name", "bn-en"),
        Triple("আমার নাম", "my name is", "bn-en"),
        Triple("আজকের আবহাওয়া কেমন", "how is the weather today", "bn-en"),
        Triple("আমাকে সাহায্য করো", "help me", "bn-en"),
        Triple("আমি তোমাকে ভালোবাসি", "i love you", "bn-en"),
        Triple("আবার বলো", "say it again", "bn-en"),
        Triple("বুঝতে পারিনি", "i did not understand", "bn-en"),
        Triple("কত টাকা", "how much money", "bn-en"),
        Triple("কয়টা বাজে", "what time is it", "bn-en"),
        Triple("আমার সাহায্য দরকার", "i need help", "bn-en"),
        Triple("খাবার খেয়েছো", "have you eaten", "bn-en"),
        Triple("আমি ব্যস্ত আছি", "i am busy", "bn-en"),
        Triple("কল করো", "call me", "bn-en"),
        Triple("ঘরে যাচ্ছি", "i am going home", "bn-en")
    )

    private val dictionary = mapOf(
        "আমি" to "i", "তুমি" to "you", "আপনি" to "you", "সে" to "he", "আমরা" to "we",
        "ভালো" to "good", "খারাপ" to "bad", "বড়" to "big", "ছোট" to "small",
        "আজ" to "today", "কাল" to "tomorrow", "গতকাল" to "yesterday", "এখন" to "now",
        "পানি" to "water", "খাবার" to "food", "বাড়ি" to "home", "স্কুল" to "school",
        "অফিস" to "office", "বন্ধু" to "friend", "পরিবার" to "family", "সময়" to "time",
        "টাকা" to "money", "বাজার" to "market", "বই" to "book", "ফোন" to "phone",
        "কাজ" to "work", "ঘুম" to "sleep", "খুব" to "very", "একটু" to "a little",
        "কেমন" to "how", "কি" to "what", "কেন" to "why", "কোথায়" to "where", "কত" to "how much",
        "ধন্যবাদ" to "thanks", "দুঃখিত" to "sorry", "হ্যাঁ" to "yes", "না" to "no",
        "ভালোবাসা" to "love", "সাহায্য" to "help", "আবহাওয়া" to "weather", "বৃষ্টি" to "rain"
    )

    /** Returns the translation, or null when the phrase is outside the offline book. */
    fun translate(text: String, targetLanguage: String): String? {
        val clean = text.trim().trim('।', '.', '?', '!', ',')
        if (clean.isBlank()) return null
        val lower = clean.lowercase(Locale.ROOT)
        val targetIsEnglish = targetLanguage.contains("en", ignoreCase = true)

        val direct = phrases.firstOrNull { (bengali, english, _) ->
            lower.contains(bengali) || lower.contains(english)
        }
        if (direct != null) {
            return if (targetIsEnglish) direct.second else direct.first
        }

        if (targetIsEnglish) {
            val words = clean.split(" ").filter { it.isNotBlank() }
            if (words.size > 7) return null
            val translated = words.map { word ->
                dictionary[word] ?: dictionary[word.trim('।', ',', '?')]
            }
            val known = translated.count { it != null }
            if (translated.isNotEmpty() && known.toDouble() / translated.size >= 0.6) {
                return translated.joinToString(" ") { it ?: "…" }
            }
        }
        return null
    }

    fun looksLikeTranslation(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("অনুবাদ") || lower.contains("translate") ||
            lower.contains("ইংরেজিতে বলো") || lower.contains("বাংলায় বলো") ||
            lower.contains("ইংলিশে বলো") || lower.contains("in english") || lower.contains("in bengali")
    }

    fun extractSourceText(text: String): String {
        return text
            .replace(Regex("(?i)(অনুবাদ করো|অনুবাদ কর|অনুবাদ|ইংরেজিতে বলো|ইংরেজিতে|বাংলায় বলো|বাংলায়|translate to english|translate to bengali|translate|in english|in bengali|to english|to bengali)"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ':', '-', '"', '\'')
    }
}
