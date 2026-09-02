package com.example.core.text

/**
 * Detected input language. Arohi understands Bengali (বাংলা), Banglish
 * (Bengali written in Latin letters) and English, and supports mixed input.
 */
enum class InputLanguage { BENGALI, BANGLISH, ENGLISH, MIXED, UNKNOWN }

/**
 * Pure-Kotlin text utilities used by the command pipeline: normalization,
 * language detection, wake-word stripping, and Bengali digit/word → number
 * conversion. Kept free of Android dependencies so it is fully unit-testable.
 */
object TextProcessor {

    private val BENGALI_DIGITS = '০'..'৯'
    private val BENGALI_RANGE = 'ঀ'..'৿'

    // Wake words / names Arohi responds to (spec §5).
    private val WAKE_WORDS = listOf(
        "অরোহী", "আরোহী", "অরহী", "আরহী", "অরোহি", "আরোহি", "ওরোহী",
        "arohi", "myraa", "myra", "mera", "hey arohi", "ok arohi", "এই যে"
    )

    // Banglish cues: Latin-letter tokens that strongly indicate romanized Bengali.
    private val BANGLISH_CUES = setOf(
        "koro", "koroh", "koro?", "khulo", "khulo?", "kholo", "bolo", "dao", "dekhao",
        "dekhau", "balo", "bujhi", "bujhte", "paro", "parba", "kichu", "ekta", "akta",
        "amar", "amake", "amr", "tumi", "tui", "apni", "kemon", "ache", "acha", "accha",
        "na", "han", "baire", "ghore", "jabo", "jacchi", "jacchi", "boss", "bos",
        "shono", "shunno", "bandho", "bondho", "chalu", "on", "off", "min", "minute",
        "ghonta", "konta", "kon", "ki", "ke", "kothay", "kothae", "tai", "oi", "ei",
        "seta", "eta", "ota", "khujhe", "khujo", "pao", "pawa", "hobe", "hoy", "goto",
        "agami", "kal", "aj", "aaj", "rohing", "routine", "schedule", "poro", "pora"
    )

    fun normalize(raw: String): String {
        var t = raw.trim().replace(Regex("\\s+"), " ")
        // Strip surrounding quotes / punctuation noise but keep meaningful chars.
        t = t.trim('"', '\'', '“', '”', '‘', '’', ' ')
        return t
    }

    fun detectLanguage(input: String): InputLanguage {
        val t = input.trim()
        if (t.isEmpty()) return InputLanguage.UNKNOWN

        var bengaliChars = 0
        var latinChars = 0
        for (ch in t) {
            when (ch) {
                in BENGALI_RANGE -> bengaliChars++
                in 'a'..'z', in 'A'..'Z' -> latinChars++
            }
        }

        val hasBengali = bengaliChars > 0
        val hasLatin = latinChars > 0
        if (hasBengali && hasLatin) return InputLanguage.MIXED
        if (hasBengali) return InputLanguage.BENGALI
        if (!hasLatin) return InputLanguage.UNKNOWN

        // Latin only: decide Banglish vs English via cue words.
        val lower = t.lowercase()
        val tokens = Regex("[a-z]+").findAll(lower).map { it.value }.toList()
        val cueHits = tokens.count { it in BANGLISH_CUES }
        // A handful of Banglish function words is a strong signal.
        return if (cueHits >= 1 && (cueHits >= 2 || tokens.size <= 6)) InputLanguage.BANGLISH
        else if (cueHits >= 2) InputLanguage.BANGLISH
        else InputLanguage.ENGLISH
    }

    /** Removes leading wake-word/address so intent matching sees the real command. */
    fun stripWakeWord(input: String): String {
        var t = normalize(input)
        val lower = t.lowercase()
        for (wake in WAKE_WORDS) {
            if (lower.startsWith(wake)) {
                t = t.substring(wake.length).trimStart(',', ' ', '-', '!', '।', '.')
                break
            }
        }
        return normalize(t)
    }

    fun containsWakeWord(input: String): Boolean {
        val lower = input.lowercase()
        return WAKE_WORDS.any { lower.contains(it) }
    }

    /** Converts Bengali digits (০-৯) in a string to ASCII digits. */
    fun bengaliDigitsToAscii(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(if (ch in BENGALI_DIGITS) ('0' + (ch - '০')) else ch)
        }
        return sb.toString()
    }
}
