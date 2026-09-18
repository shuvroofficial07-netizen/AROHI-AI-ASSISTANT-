package com.example.engine

import com.example.data.local.entity.ReminderRepeat
import java.util.Calendar
import java.util.Locale

/**
 * Understands real Bengali time phrases so "আগামীকাল সকাল ৭টায় মনে করিয়ে দিও" becomes an
 * exact AlarmManager timestamp:  *  - "৫ মিনিট পরে", "দেড় ঘন্টা পরে", "in 20 minutes"
 *  - "সকাল ৭টায়", "সন্ধ্যা ৬:৩০", "রাত ৯টা", "10:15 pm"
 *  - "আজ / কাল / আগামীকাল / পরশু / প্রতি সোমবার"
 */
object TimePhraseParser {

    private val bengaliDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private val delayMinutesRegex = Regex("(\\d{1,4})\\s*(মিনিট|minutes|minute|mins|min|মিনিটে)")
    private val delayHoursRegex = Regex("(\\d{1,3})\\s*(ঘন্টা|ঘণ্টা|hours|hour|hrs|hr)")
    private val clockWithDigitsRegex = Regex("(\\d{1,2}):(\\d{2})")
    private val clockHourOnlyRegex = Regex("(\\d{1,2})\\s*(টা|টায়|টার|o'clock|oclock|টা\\b)")
    private val periodRegex = Regex("(সকাল|ভোর|দুপুর|বিকাল|বিকেল|সন্ধ্যা|রাত|রাত্রি|am|pm|এএম|পিএম)")

    fun normalizeDigits(input: String): String {
        val builder = StringBuilder(input.length)
        for (char in input) {
            builder.append(bengaliDigits[char] ?: char)
        }
        return builder.toString()
    }

    fun toBengaliDigits(number: String): String {
        val reverse = bengaliDigits.entries.associate { (k, v) -> v to k }
        val builder = StringBuilder(number.length)
        for (char in number) {
            builder.append(reverse[char] ?: char)
        }
        return builder.toString()
    }

    /** Parses repeat intent such as "প্রতিদিন", "প্রতি সপ্তাহে", "every weekday". */
    fun parseRepeat(text: String): String {
        val lower = normalizeDigits(text).lowercase(Locale.ROOT)
        return when {
            lower.contains("প্রতিদিন") || lower.contains("roজ") || lower.contains("রোজ") ||
                lower.contains("every day") || lower.contains("daily") -> ReminderRepeat.DAILY
            lower.contains("প্রতি সপ্তাহ") || lower.contains("every week") || lower.contains("weekly") ->
                ReminderRepeat.WEEKLY
            lower.contains("প্রতি মাস") || lower.contains("every month") || lower.contains("monthly") ->
                ReminderRepeat.MONTHLY
            lower.contains("কর্মদিবস") || lower.contains("weekday") || lower.contains("সপ্তাহের দিনে") ->
                ReminderRepeat.WEEKDAYS
            else -> ReminderRepeat.NONE
        }
    }

    /**
     * Resolves a phrase to an absolute epoch-millis timestamp, or null when no time information
     * could be found (the caller then asks the user for a time).
     */
    fun parse(text: String, now: Long = System.currentTimeMillis()): Long? {
        val normalized = normalizeDigits(text)
        val lower = normalized.lowercase(Locale.ROOT)

        // 1. Pure relative offsets ("১০ মিনিট পরে", "2 hours later")
        val relative = parseRelative(lower, now)
        if (relative != null) return relative

        // 2. Clock time, optionally with a Bengali period word ("সন্ধ্যা ৬:৩০", "সকাল ৭টায়")
        val clock = parseClock(lower, now)
        if (clock != null) return clock

        // 3. Day words without a clock time ("আগামীকাল" → 9am next day)
        val dayOffset = resolveDayOffset(lower)
        if (dayOffset != null) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (dayOffset == 0 && calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        return null
    }

    private fun parseRelative(lower: String, now: Long): Long? {
        val minuteMatch = delayMinutesRegex.find(lower)
        if (minuteMatch != null) {
            val minutes = minuteMatch.groupValues[1].toIntOrNull() ?: return null
            return now + minutes * 60_000L
        }
        val hourMatch = delayHoursRegex.find(lower)
        if (hourMatch != null) {
            val hours = hourMatch.groupValues[1].toIntOrNull() ?: return null
            return now + hours * 3_600_000L
        }
        return when {
            lower.contains("আধা ঘন্টা") || lower.contains("আধা ঘণ্টা") || lower.contains("হাফ আওয়ার") ||
                lower.contains("সাড়ে ঘন্টা") -> now + 30 * 60_000L
            lower.contains("একটু পরে") || lower.contains("কিছুক্ষণ পরে") -> now + 10 * 60_000L
            else -> null
        }
    }

    private fun parseClock(lower: String, now: Long): Long? {
        var hour: Int? = null
        var minute = 0

        val withMinutes = clockWithDigitsRegex.find(lower)
        if (withMinutes != null) {
            hour = withMinutes.groupValues[1].toIntOrNull()
            minute = withMinutes.groupValues[2].toIntOrNull() ?: 0
        } else {
            val hourOnly = clockHourOnlyRegex.find(lower)
            if (hourOnly != null) {
                hour = hourOnly.groupValues[1].toIntOrNull()
            } else {
                val amPm = Regex("(\\d{1,2})\\s*(am|pm|এএম|পিএম)").find(lower)
                if (amPm != null) {
                    hour = amPm.groupValues[1].toIntOrNull()
                    val marker = amPm.groupValues[2]
                    if (marker == "pm" || marker == "পিএম") {
                        if (hour != null && hour < 12) hour += 12
                    }
                }
            }
        }

        val resolvedHour = hour ?: return null
        if (resolvedHour > 23 || minute > 59) return null

        val period = periodRegex.find(lower)?.groupValues?.getOrNull(1) ?: ""
        var finalHour = resolvedHour
        when (period) {
            "সকাল", "ভোর" -> if (finalHour == 12) finalHour = 0
            "দুপুর" -> if (finalHour in 1..5) finalHour += 12
            "বিকাল", "বিকেল" -> if (finalHour in 1..11) finalHour += 12
            "সন্ধ্যা" -> if (finalHour in 1..11) finalHour += 12
            "রাত", "রাত্রি" -> when (finalHour) {
                12 -> finalHour = 0
                in 1..4 -> finalHour += 12
                in 5..11 -> finalHour += 12
            }
            "pm", "পিএম" -> if (finalHour in 1..11) finalHour += 12
            "am", "এএম" -> if (finalHour == 12) finalHour = 0
        }

        val dayOffset = resolveDayOffset(lower) ?: 0
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, finalHour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun resolveDayOffset(lower: String): Int? = when {
        lower.contains("পরশু") || lower.contains("day after tomorrow") -> 2
        lower.contains("আগামীকাল") || lower.contains("tomorrow") || lower.contains("কালকে") -> 1
        lower.contains("আজকে") || lower.contains("আজ") || lower.contains("today") -> 0
        else -> null
    }
}
