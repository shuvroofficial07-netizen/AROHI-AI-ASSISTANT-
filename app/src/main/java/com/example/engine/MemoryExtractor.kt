package com.example.engine

import com.example.data.repository.MemoryRepository
import com.example.data.repository.SettingsRepository
import java.util.Locale

/**
 * Turns natural sentences into persistent local memories, e.g.
 *  - "আমার নাম শুভ্র"
 *  - "আমার প্রিয় রং নীল"
 *  - "আমি রাতে ১১টার পরে ঘুমাই"
 *
 * Everything is written into the Room `memories` table and is therefore available to every
 * future conversation — the app never depends on the cloud for this.
 */
class MemoryExtractor(
    private val memoryRepository: MemoryRepository,
    private val settingsRepository: SettingsRepository
) {

    data class ExtractedFact(val category: String, val key: String, val value: String, val display: String)

    private val stopWords = listOf("।", ".", ",", ";", "\n", " আর ", " এবং ", " কিন্তু ", " তবে ")

    fun detect(input: String): List<ExtractedFact> {
        val text = input.trim()
        if (text.length < 4) return emptyList()
        val lower = text.lowercase(Locale.ROOT)
        val facts = mutableListOf<ExtractedFact>()

        capture(lower, Regex("আমার\\s*নাম\\s*(?:হলো|হচ্চে|হচ্ছে)?\\s*"))?.let { value ->
            facts.add(ExtractedFact("PROFILE", "user_name", value, "তোমার নাম"))
        }

        // English fallback: "my name is X"
        if (facts.isEmpty()) {
            Regex("my name is\\s+([a-zA-Z\\u0980-\\u09FF ]{2,40})").find(text)?.let { match ->
                val value = cleanValue(match.groupValues[1])
                if (value.isNotBlank()) {
                    facts.add(ExtractedFact("PROFILE", "user_name", value, "তোমার নাম"))
                }
            }
        }

        capture(lower, Regex("আমার\\s*ডাক?নাম\\s*"))?.let { value ->
            facts.add(ExtractedFact("PROFILE", "nickname", value, "তোমার ডাকনাম"))
        }

        capture(lower, Regex("আমার\\s*জন্মদিন\\s*"))?.let { value ->
            facts.add(ExtractedFact("PROFILE", "birthday", value, "তোমার জন্মদিন"))
        }

        capture(lower, Regex("আমার\\s*(?:মোবাইল|ফোন)\\s*নম্বর\\s*(?:হলো)?\\s*"))?.let { value ->
            facts.add(ExtractedFact("PROFILE", "phone", value, "তোমার ফোন নম্বর"))
        }

        capture(lower, Regex("আমি\\s*(?:থাকি|বসবাস করি)\\s*"))?.let { value ->
            facts.add(ExtractedFact("PROFILE", "location", value, "তোমার লোকেশন"))
        }

        val favoriteCategories = mapOf(
            "রং" to "favorite_color",
            "খাবার" to "favorite_food",
            "গান" to "favorite_song",
            "বই" to "favorite_book",
            "অ্যাপ" to "favorite_app",
            "খেলা" to "favorite_game",
            "সিনেমা" to "favorite_movie"
        )
        for ((bengali, key) in favoriteCategories) {
            capture(lower, Regex("আমার\\s*প্রিয়\\s*$bengali\\s*(?:হলো)?\\s*"))?.let { value ->
                facts.add(ExtractedFact("PREFERENCES", key, value, "তোমার প্রিয় $bengali"))
            }
        }

        capture(lower, Regex("আমার\\s*পড়ার\\s*সময়\\s*"))?.let { value ->
            facts.add(ExtractedFact("PREFERENCES", "study_time", value, "তোমার পড়ার সময়"))
        }
        capture(lower, Regex("আমি\\s*প্রতিদিন\\s*(?:সকালে)?\\s*(?:ঘুম থেকে)?\\s*উঠি\\s*"))?.let { value ->
            facts.add(ExtractedFact("PREFERENCES", "wake_time", value, "তোমার ঘুম থেকে ওঠার সময়"))
        }

        // De-duplicate identical keys inside one sentence.
        return facts.distinctBy { it.key }
    }

    /** Detects + persists in one call; returns human friendly confirmations. */
    suspend fun extractAndSave(input: String): List<String> {
        if (!settingsRepository.isAutoMemoryEnabled()) return emptyList()
        val facts = detect(input)
        if (facts.isEmpty()) return emptyList()

        val confirmations = mutableListOf<String>()
        for (fact in facts) {
            memoryRepository.saveMemory(fact.category, fact.key, fact.value)
            if (fact.key == "user_name") {
                settingsRepository.setUserName(fact.value)
            }
            if (fact.key == "nickname") {
                settingsRepository.setUserNickname(fact.value)
            }
            confirmations.add("${fact.display}: ${fact.value}")
        }
        return confirmations
    }

    private fun capture(lower: String, pattern: Regex): String? {
        val match = pattern.find(lower) ?: return null
        val tail = lower.substring(match.range.last + 1)
        if (tail.isBlank()) return null
        val value = cleanValue(tail)
        return value.ifBlank { null }
    }

    private fun cleanValue(raw: String): String {
        var value = raw.trim()
        for (stop in stopWords) {
            val index = value.indexOf(stop)
            if (index > 0) value = value.substring(0, index)
        }
        value = value.trim().trim('।', '.', ',', '?', '!', '"', '\'')
        return value.take(60).trim()
    }
}
