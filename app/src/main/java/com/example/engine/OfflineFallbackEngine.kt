package com.example.engine

import java.util.Calendar
import java.util.Locale

/**
 * Guarantees a natural Bengali answer when the device is offline or the cloud brain is not
 * configured. These are preset, hand-written responses — no network, no fake claims.
 */
class OfflineFallbackEngine(private val conversationHistoryProvider: () -> String? = { null }) {

    data class FallbackReply(
        val text: String,
        val emotion: ArohiEmotion,
        val matchedPreset: String?
    )

    fun timeBasedGreeting(now: Long = System.currentTimeMillis()): String {
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "শুভ সকাল"
            in 12..15 -> "শুভ দুপুর"
            in 16..18 -> "শুভ বিকাল"
            in 19..21 -> "শুভ সন্ধ্যা"
            else -> "শুভ রাত্রি"
        }
    }

    fun respond(input: String, privacyMode: Boolean = false): FallbackReply {
        val lower = input.lowercase(Locale.ROOT)

        val preset = when {
            lower.contains("কেমন আছ") || lower.contains("how are you") ->
                Preset(
                    "wellbeing",
                    "আমি ভালো আছি, জিজ্ঞেস করার জন্য ধন্যবাদ! তুমি কেমন আছ?",
                    ArohiEmotion.HAPPY
                )

            lower.contains("শুভ সকাল") || lower.contains("good morning") ->
                Preset("morning", "শুভ সকাল! আজকের দিনটা তোমার জন্য শুভ হোক। কিছু মনে করিয়ে দিতে হবে?", ArohiEmotion.HAPPY)

            lower.contains("শুভ রাত্রি") || lower.contains("good night") ->
                Preset("night", "শুভ রাত্রি। ফোনটা চার্জে দিয়ে আরাম করে ঘুমাও, কাল সকালে দেখা হবে।", ArohiEmotion.CALM)

            lower.contains("ধন্যবাদ") || lower.contains("thank") ->
                Preset("thanks", "আর বলো না, এটা তো আমার কাজ। আর কিছু লাগলে বলো।", ArohiEmotion.HAPPY)

            lower.contains("তুমি কে") || lower.contains("তোমার নাম") || lower.contains("who are you") ->
                Preset("identity", "আমি আরোহী — তোমার ব্যক্তিগত AI সঙ্গী। এই অ্যাপেই থাকি, তোমার ফোনের রিমাইন্ডার, টু-ডু, নোট আর হিসাব আমি সামলাতে পারি।", ArohiEmotion.CALM)

            lower.contains("কি করতে পার") || lower.contains("তোমার কাজ") || lower.contains("what can you do") ->
                Preset(
                    "capabilities",
                    "নেট ছাড়াও আমি এখন এসব করতে পারি: রিমাইন্ডার/অ্যালার্ম, টু-ডু ও নোট, হিসাব-নিকাশ, একক ও মুদ্রা রূপান্তর, সময়-তারিখ, ফোনের ব্যাটারি ও টর্চ, আর রুটিন কমান্ড।",
                    ArohiEmotion.FOCUSED
                )

            lower.contains("নেট নেই") || lower.contains("ইন্টারনেট নেই") || lower.contains("offline") ->
                Preset("offline", "ঠিক আছে, নেট না থাকলেও আমি তোমার পাশে আছি — লোকাল কমান্ড, রিমাইন্ডার আর নোট সব কাজ করবে।", ArohiEmotion.CALM)

            lower.contains("গান") && (lower.contains("শোনাও") || lower.contains("চালাও")) ->
                Preset(
                    "music",
                    "মিউজিক চালানোর জন্য তোমার গানের অ্যাপটার নাম বলো, আমি খুলে দিচ্ছি।",
                    ArohiEmotion.CALM
                )

            lower.contains("ভালোবাসি") || lower.contains("love you") ->
                Preset("boundary", "ধন্যবাদ! আমি তোমার পাশে থাকার সঙ্গী, তবে তোমার আসল মানুষগুলোকেও একটু সময় দিও — ওরাই সবচেয়ে বেশি জরুরি।", ArohiEmotion.CONCERNED)

            lower.contains("একা") || lower.contains("মন খারাপ") || lower.contains("depressed") ->
                Preset("mood", "মন খারাপ লাগলে বলো, আমি শুনব। তবে মনে রাখো — কাছের কোনো মানুষের সাথে আজ একটু কথা বললে অনেক ভালো লাগবে।", ArohiEmotion.CONCERNED)

            lower.contains("সময় কত") || lower.contains("কতটা বাজে") || lower.contains("what time") ->
                Preset("clock", "এখন সময় ${currentTimeText()}।", ArohiEmotion.CALM)

            lower.contains("তারিখ") || lower.contains("আজ কী বার") || lower.contains("today date") ->
                Preset("date", "আজ ${currentDateText()}।", ArohiEmotion.CALM)
        }

        if (preset != null) {
            return FallbackReply(preset.text, preset.emotion, preset.key)
        }

        val carryOver = conversationHistoryProvider()
        val continuity = if (!carryOver.isNullOrBlank()) " আগের কথাটা মনে আছে — $carryOver" else ""
        val reason = if (privacyMode) {
            "তুমি প্রাইভেসি মোড চালু রেখেছ, তাই আমি ক্লাউডে যাচ্ছি না।"
        } else {
            "এখন ইন্টারনেট বা API সংযোগ পাচ্ছি না।"
        }
        return FallbackReply(
            text = "$reason$continuity তবুও আমি তোমার লোকাল কাজগুলো করতে পারি — রিমাইন্ডার, টু-ডু, নোট, হিসাব আর ফোনের কমান্ড। বলো কী করতে হবে?",
            emotion = ArohiEmotion.CONCERNED,
            matchedPreset = null
        )
    }

    fun currentTimeText(now: Long = System.currentTimeMillis()): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val minuteText = if (minute < 10) "0$minute" else "$minute"
        val period = when (hour24) {
            in 4..11 -> "সকাল"
            in 12..15 -> "দুপুর"
            in 16..18 -> "বিকাল"
            in 19..20 -> "সন্ধ্যা"
            else -> "রাত"
        }
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return "$period $hour12:$minuteText"
    }

    fun currentDateText(now: Long = System.currentTimeMillis()): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val months = listOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val days = listOf("রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার")
        val dayName = days.getOrElse(calendar.get(Calendar.DAY_OF_WEEK) - 1) { "" }
        val monthName = months.getOrElse(calendar.get(Calendar.MONTH)) { "" }
        return "$dayName, ${calendar.get(Calendar.DAY_OF_MONTH)} $monthName ${calendar.get(Calendar.YEAR)}"
    }

    private data class Preset(val key: String, val text: String, val emotion: ArohiEmotion)
}
