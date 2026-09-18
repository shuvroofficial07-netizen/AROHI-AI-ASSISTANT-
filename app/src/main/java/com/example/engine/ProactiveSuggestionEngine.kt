package com.example.engine

import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TodoEntity
import com.example.device.ReminderScheduler
import com.example.device.WeatherSnapshot
import com.example.service.CheckInType
import java.util.Calendar

/**
 * Builds the pro-active, context aware one-liners AROHI sends on her own:
 *  - "আজ বৃষ্টি হতে পারে, ছাতা নিও।"
 *  - "৩টায় তোমার meeting আছে, ২০ মিনিট বাকি।"
 */
class ProactiveSuggestionEngine {

    fun suggestion(
        weather: WeatherSnapshot?,
        reminders: List<ReminderEntity>,
        todos: List<TodoEntity>,
        batteryPercent: Int,
        isCharging: Boolean,
        now: Long = System.currentTimeMillis()
    ): String? {
        val candidates = mutableListOf<String>()
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)

        if (weather != null) {
            when {
                weather.rainChancePercent >= 50 ->
                    candidates.add("আজ বৃষ্টির সম্ভাবনা ${weather.rainChancePercent}% — বেরোনোর আগে ছাতা নিয়ে যেও।")
                weather.maxC >= 35 ->
                    candidates.add("আজ বেশ গরম পড়বে (${weather.maxC.toInt()}°C) — পানি খেতে ভুলো না।")
                weather.minC <= 15 ->
                    candidates.add("আজ অনেক ঠান্ডা (${weather.minC.toInt()}°C) — গরম কাপড় পরে বেরিও।")
                weather.rainChancePercent in 25..49 ->
                    candidates.add("আকাশটা একটু মেঘলা, বৃষ্টির সম্ভাবনা ${weather.rainChancePercent}% — ছাতা সাথে রাখলে ভালো।")
            }
        }

        val upcoming = reminders
            .filter { !it.isCompleted && it.triggerAt > now }
            .minByOrNull { it.triggerAt }
        if (upcoming != null) {
            val minutesLeft = (upcoming.triggerAt - now) / 60_000L
            if (minutesLeft in 1..180) {
                candidates.add(
                    "'${upcoming.title}' — ${ReminderScheduler.formatBengaliDateTime(upcoming.triggerAt)} " +
                        "(${minutesLeft} মিনিট বাকি)।"
                )
            }
        }

        val pendingTodos = todos.count { !it.isDone }
        if (pendingTodos > 0 && hour >= 11 && hour <= 21) {
            candidates.add("তোমার টু-ডু লিস্টে ${bengaliNumber(pendingTodos)}টা কাজ এখনো বাকি আছে।")
        }

        if (!isCharging && batteryPercent in 1..25) {
            candidates.add("ব্যাটারি $batteryPercent% — একটু চার্জে দিয়ে রাখলে ভালো হবে।")
        }

        return if (candidates.isEmpty()) null else candidates.take(2).joinToString(" ")
    }

    fun buildCheckIn(
        type: CheckInType,
        weather: WeatherSnapshot?,
        reminders: List<ReminderEntity>,
        todos: List<TodoEntity>,
        batteryPercent: Int,
        isCharging: Boolean,
        userName: String,
        now: Long = System.currentTimeMillis()
    ): String {
        val namePart = if (userName.isBlank()) "" else " $userName"
        val greeting = if (type == CheckInType.MORNING) "শুভ সকাল$namePart!" else "শুভ সন্ধ্যা$namePart!"

        val lines = mutableListOf<String>()
        lines.add(greeting)

        if (type == CheckInType.MORNING) {
            if (weather != null) {
                lines.add(weather.spokenSummary())
            }
            val todayReminders = reminders.filter {
                !it.isCompleted && isSameDay(it.triggerAt, now) && it.triggerAt > now
            }
            if (todayReminders.isNotEmpty()) {
                lines.add("আজ মনে রাখতে হবে: " + todayReminders.take(3).joinToString(", ") { it.title } + "।")
            }
        } else {
            val pending = todos.count { !it.isDone }
            if (pending > 0) {
                lines.add("আজ ${bengaliNumber(pending)}টা কাজ বাকি রয়ে গেছে — কাল সকালে দেখে নিও।")
            }
            val tomorrow = reminders.filter {
                !it.isCompleted && isSameDay(it.triggerAt, now + 24L * 60L * 60L * 1000L)
            }
            if (tomorrow.isNotEmpty()) {
                lines.add("কালকের জন্য আছে: " + tomorrow.take(3).joinToString(", ") { it.title } + "।")
            }
            if (!isCharging && batteryPercent < 40) {
                lines.add("ফোনটা চার্জে দিয়ে ঘুমাও, ব্যাটারি $batteryPercent%।")
            }
        }

        if (lines.size == 1) {
            lines.add(
                if (type == CheckInType.MORNING) {
                    "আজকের দিনটা তোমার জন্য ভালো কাটুক।"
                } else {
                    "আজকের দিনটা কেমন গেল? একটু বিশ্রাম নিও।"
                }
            )
        }
        return lines.joinToString(" ")
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = a }
        val calB = Calendar.getInstance().apply { timeInMillis = b }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    private fun bengaliNumber(value: Int): String =
        TimePhraseParser.toBengaliDigits(value.toString())
}
