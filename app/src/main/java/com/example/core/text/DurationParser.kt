package com.example.core.text

/**
 * Parses human duration / clock-time expressions from Bengali, Banglish and
 * English commands, e.g. "২০ মিনিটের timer", "30 seconds", "পাঁচ মিনিট",
 * "৭টায় alarm", "timer dao 10 min", "at 7:30".
 *
 * Pure Kotlin, fully unit-testable, and side-effect free.
 */
object DurationParser {

    data class Duration(val totalMillis: Long, val seconds: Long, val description: String)

    private val BENGALI_WORD_DIGITS = mapOf(
        "এক" to 1, "দুই" to 2, "তিন" to 3, "চার" to 4, "পাঁচ" to 5, "ছয়" to 6,
        "সাত" to 7, "আট" to 8, "নয়" to 9, "দশ" to 10,
        "এগারো" to 11, "বারো" to 12, "পনেরো" to 15, "বিশ" to 20, "ত্রিশ" to 30,
        "পঁয়তাল্লিশ" to 45, "ষাট" to 60, "এক ঘন্টা" to 60
    )

    private val ENGLISH_WORD_DIGITS = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "fifteen" to 15, "twenty" to 20, "thirty" to 30, "forty five" to 45
    )

    /** Extracts a count-up duration (for timers): returns null if none found. */
    fun parseDuration(input: String): Duration? {
        val ascii = TextProcessor.bengaliDigitsToAscii(input).lowercase()

        // Hours: "2 ghonta", "২ ঘণ্টা", "1 hour", "এক ঘন্টা"
        findValue(ascii, listOf("ঘণ্টা", "ঘন্টা", "ghonta", "ghonta?", "hour", "hours", "hr"))
            ?.let { return buildDuration(it * 3_600_000L, it, "hour") }

        // Minutes: "20 minute", "মিনিট", "min"
        findValue(ascii, listOf("minute", "minutes", "min", "মিনিট", "minit", "m"))
            ?.let { return buildDuration(it * 60_000L, it, "minute") }

        // Seconds: "30 second", "সেকেন্ড", "sec"
        findValue(ascii, listOf("second", "seconds", "sec", "সেকেন্ড", "secs"))
            ?.let { return buildDuration(it * 1_000L, it, "second") }

        return null
    }

    /**
     * Extracts a clock time (for alarms/reminders): returns hour & minute in
     * 24h format when possible. [assumePm] hints Banglish "7 ta" as evening.
     */
    data class ClockTime(val hour24: Int, val minute: Int)

    fun parseClockTime(input: String, assumePm: Boolean = true): ClockTime? {
        val ascii = TextProcessor.bengaliDigitsToAscii(input)

        // h:mm or h.mm pattern (Bengali "৭:৩০" → "7:30")
        Regex("(\\d{1,2})\\s*[:.]\\s*(\\d{2})").find(ascii)?.let { m ->
            val h = m.groupValues[1].toInt().coerceIn(0, 23)
            val min = m.groupValues[2].toInt().coerceIn(0, 59)
            return ClockTime(normalizeHour(h, ascii, assumePm), min)
        }

        // "7 ta", "৭টা", "at 8"
        Regex("(\\d{1,2})\\s*(?:টা|টায়|ta|tay|ta\\b|o'?clock)?").find(ascii)?.let { m ->
            // Only accept when the phrase actually references a clock hour.
            val around = ascii.substring(0, minOf(ascii.length, m.range.last + 6))
            if (Regex("(টা|টায়|ta|tay|o'?clock|সকাল|বিকাল|রাত|সন্ধ্য|morning|night|pm|am|at )")
                    .containsMatchIn(ascii)) {
                val h = m.groupValues[1].toInt().coerceIn(0, 23)
                return ClockTime(normalizeHour(h, ascii, assumePm), 0)
            }
        }
        return null
    }

    private fun normalizeHour(h: Int, context: String, assumePm: Boolean): Int {
        val pm = context.contains("pm") || context.contains("বিকাল") ||
            context.contains("রাত") || context.contains("সন্ধ্য") || context.contains("night") ||
            context.contains("evening") || context.contains("দুপুর")
        val am = context.contains("am") || context.contains("সকাল") ||
            context.contains("ভোর") || context.contains("morning")
        return when {
            h in 1..12 && pm && h != 12 -> h + 12
            h == 12 && am -> 0
            h in 1..11 && !pm && !am && assumePm && h < 6 -> h // early hours ambiguous; leave
            else -> h
        }
    }

    private fun buildDuration(millis: Long, value: Int, unit: String): Duration {
        val desc = "$value $unit" + if (value == 1) "" else "s"
        return Duration(millis, millis / 1000, desc)
    }

    private fun findValue(asciiLower: String, units: List<String>): Int? {
        for (unit in units) {
            val regex = Regex("(\\d+)\\s*${Regex.escape(unit)}")
            regex.find(asciiLower)?.let { return it.groupValues[1].toInt().coerceAtLeast(1) }
        }
        // Word-number forms, e.g. "পাঁচ মিনিট", "five minutes"
        for ((word, num) in BENGALI_WORD_DIGITS) {
            if (units.any { asciiLower.contains("$word $it") || asciiLower.contains("$word${it}") }) {
                return num
            }
        }
        for ((word, num) in ENGLISH_WORD_DIGITS) {
            if (units.any { asciiLower.contains("$word $it") }) return num
        }
        return null
    }
}
