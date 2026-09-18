package com.example.engine

import com.example.data.local.entity.ReminderRepeat
import com.example.data.repository.NoteRepository
import com.example.data.repository.ReminderRepository
import com.example.data.repository.TodoRepository
import com.example.data.repository.WeatherRepository
import com.example.device.CalendarHelper
import com.example.device.ReminderScheduler
import java.util.Calendar
import java.util.Locale

/**
 * Executes the productivity half of the assistant completely on-device:
 * reminders/alarms, to-dos, notes, calculator, unit + currency conversion, weather, calendar,
 * clock/date answers and offline phrase translation.
 *
 * Runs BEFORE [LocalCommandEngine] so that phrases like "রিমাইন্ডার বন্ধ করো" are not mistaken
 * for a "stop talking" command.
 */
class ProductivityEngine(
    private val reminderRepository: ReminderRepository,
    private val todoRepository: TodoRepository,
    private val noteRepository: NoteRepository,
    private val weatherRepository: WeatherRepository,
    private val calendarHelper: CalendarHelper,
    private val offlineFallbackEngine: OfflineFallbackEngine
) {

    suspend fun tryExecute(input: String): LocalExecutionResult {
        val query = input.trim()
        if (query.isBlank()) return notHandled()
        val normalized = TimePhraseParser.normalizeDigits(query)
        val lower = normalized.lowercase(Locale.ROOT)

        clockAndDate(lower)?.let { return it }
        reminderCreation(lower, query, normalized)?.let { return it }
        reminderListing(lower)?.let { return it }
        reminderDeletion(lower)?.let { return it }
        calendarQuery(lower)?.let { return it }
        todos(lower, query, normalized)?.let { return it }
        notes(lower, query, normalized)?.let { return it }
        weather(lower)?.let { return it }
        translation(lower, query)?.let { return it }
        maths(lower, normalized)?.let { return it }
        unitConversion(lower, normalized)?.let { return it }

        return notHandled()
    }

    // ------------------------------------------------------------------ clock & date

    private fun clockAndDate(lower: String): LocalExecutionResult? {
        val asksTime = lower.contains("সময় কত") || lower.contains("কতটা বাজে") || lower.contains("কত বাজে") ||
            lower.contains("what time") || lower.contains("time koto")
        val asksDate = lower.contains("কী তারিখ") || lower.contains("কি তারিখ") || lower.contains("আজ কী বার") ||
            lower.contains("আজ কি বার") || lower.contains("আজকের তারিখ") || lower.contains("today date")
        return when {
            asksTime -> LocalExecutionResult(
                isHandled = true,
                responseText = "এখন ${offlineFallbackEngine.currentTimeText()}।",
                emotion = ArohiEmotion.CALM,
                toolName = "clock"
            )
            asksDate -> LocalExecutionResult(
                isHandled = true,
                responseText = "আজ ${offlineFallbackEngine.currentDateText()}।",
                emotion = ArohiEmotion.CALM,
                toolName = "date"
            )
            else -> null
        }
    }

    // ------------------------------------------------------------------ reminders

    private fun isReminderRequest(lower: String): Boolean {
        val triggers = listOf(
            "রিমাইন্ডার", "রিমাইন্ডার", "মনে করিয়ে", "মনে করাও", "মনে রাখো", "মনে রাখ",
            "আলার্ম", "অ্যালার্ম", "remind", "reminder", "alarm"
        )
        return triggers.any { lower.contains(it) }
    }

    private suspend fun reminderCreation(
        lower: String,
        original: String,
        normalized: String
    ): LocalExecutionResult? {
        if (!isReminderRequest(lower)) return null
        val listingWords = listOf("দেখাও", "দেখা", "লিস্ট", "list", "কি কি", "কী কী", "show", "আছে কি", "কতগুলো")
        if (listingWords.any { lower.contains(it) } && !lower.contains("সেট কর")) return null

        val triggerAt = TimePhraseParser.parse(normalized)
            ?: return LocalExecutionResult(
                isHandled = true,
                responseText = "কখন মনে করিয়ে দিতে হবে বলো — যেমন 'সন্ধ্যা ৭টায় মনে করিয়ে দিও'।",
                emotion = ArohiEmotion.THINKING,
                toolName = "create_reminder"
            )

        val repeatRule = TimePhraseParser.parseRepeat(normalized)
        val title = extractReminderTitle(original)
        val id = reminderRepository.addReminder(title, triggerAt, note = "", repeatRule = repeatRule)
        val repeatText = if (repeatRule == ReminderRepeat.NONE) "" else " (${ReminderRepeat.bengaliLabel(repeatRule)})"
        return LocalExecutionResult(
            isHandled = true,
            responseText = "ঠিক আছে, '$title' — ${ReminderScheduler.formatBengaliDateTime(triggerAt)}$repeatText মনে করিয়ে দেব।",
            emotion = ArohiEmotion.HAPPY,
            toolName = "create_reminder#$id"
        )
    }

    private suspend fun reminderListing(lower: String): LocalExecutionResult? {
        if (!isReminderRequest(lower)) return null
        val listingWords = listOf("দেখাও", "দেখা", "লিস্ট", "list", "কি কি", "কী কী", "show", "আছে কি", "কতগুলো")
        if (listingWords.none { lower.contains(it) }) return null

        val reminders = reminderRepository.getActiveReminders()
        if (reminders.isEmpty()) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = "এই মুহূর্তে কোনো রিমাইন্ডার নেই। নতুন কিছু মনে করিয়ে দিতে হবে?",
                emotion = ArohiEmotion.CALM,
                toolName = "list_reminders"
            )
        }
        val summary = reminders.take(5).joinToString("\n") {
            "• ${it.title} — ${ReminderScheduler.formatBengaliDateTime(it.triggerAt)}"
        }
        return LocalExecutionResult(
            isHandled = true,
            responseText = "তোমার ${TimePhraseParser.toBengaliDigits(reminders.size.toString())}টা রিমাইন্ডার আছে:\n$summary",
            emotion = ArohiEmotion.FOCUSED,
            toolName = "list_reminders"
        )
    }

    private suspend fun reminderDeletion(lower: String): LocalExecutionResult? {
        val deleting = lower.contains("মুছে") || lower.contains("ডিলিট") || lower.contains("delete") || lower.contains("বাতিল")
        if (!isReminderRequest(lower) || !deleting) return null

        if (lower.contains("সব")) {
            reminderRepository.clearAll()
            return LocalExecutionResult(
                isHandled = true,
                responseText = "সব রিমাইন্ডার মুছে ফেলা হয়েছে।",
                emotion = ArohiEmotion.CALM,
                toolName = "delete_reminders"
            )
        }
        val title = extractReminderTitle(lower)
        val match = reminderRepository.search(title).firstOrNull()
        return if (match != null) {
            reminderRepository.deleteReminder(match.id)
            LocalExecutionResult(
                isHandled = true,
                responseText = "'${match.title}' রিমাইন্ডার মুছে দিয়েছি।",
                emotion = ArohiEmotion.CALM,
                toolName = "delete_reminder"
            )
        } else {
            LocalExecutionResult(
                isHandled = true,
                responseText = "'$title' নামে কোনো রিমাইন্ডার পাইনি।",
                emotion = ArohiEmotion.CONFUSED,
                toolName = "delete_reminder"
            )
        }
    }

    private fun extractReminderTitle(original: String): String {
        var title = original
        val noisyPhrases = listOf(
            "মনে করিয়ে দিও", "মনে করিয়ে দাও", "মনে করিয়ে দেও", "মনে করাও", "মনে রাখো",
            "রিমাইন্ডার সেট করো", "রিমাইন্ডার সেট কর", "রিমাইন্ডার দাও", "রিমাইন্ডার করে দাও",
            "রিমাইন্ডার", "অ্যালার্ম সেট করো", "আলার্ম সেট করো", "অ্যালার্ম", "আলার্ম",
            "remind me to", "remind me", "set a reminder", "reminder", "alarm",
            "সেট করে দাও", "সেট করো", "দিয়ে দাও"
        )
        for (phrase in noisyPhrases) {
            title = title.replace(phrase, " ", ignoreCase = true)
        }
        title = title.replace(Regex("[০-৯0-9]{1,4}\\s*(মিনিট|ঘন্টা|ঘণ্টা|minutes|minute|min|hours|hour|hrs|hr)"), " ")
        title = title.replace(
            Regex("(সকাল|ভোর|দুপুর|বিকাল|বিকেল|সন্ধ্যা|রাত|morning|afternoon|evening|night)?\\s*[০-৯0-9]{1,2}\\s*(?::\\s*[০-৯0-9]{2})?\\s*(টা|টায়|টার|ঘটিকায়|o'clock|টার সময়)?"),
            " "
        )
        title = title.replace(
            Regex("(আগামীকাল|পরশু|আজকে|আজ|কালকে|প্রতিদিন|রোজ|প্রতি\\s*সপ্তাহে|প্রতি\\s*মাসে|tomorrow|today|every day)"),
            " "
        )
        title = title.replace(
            Regex(
                "(পরে|পর|এর জন্য|জন্য|একটা|একটি|আমার|আমাকে|যে|বলে|সময়|টায়|" +
                    "\\bat\\b|\\bin\\b|\\bon\\b|\\bfor\\b|\\bthe\\b)",
                RegexOption.IGNORE_CASE
            ),
            " "
        )
        title = title.replace(Regex("[\\n\\t]"), " ")
        title = title.replace(Regex("\\s+"), " ").trim(' ', ',', '.', '।', '-', ':', '"', '\'')
        return title.ifBlank { "আরোহীর রিমাইন্ডার" }.take(80)
    }

    // ------------------------------------------------------------------ calendar

    private suspend fun calendarQuery(lower: String): LocalExecutionResult? {
        val triggers = listOf("শিডিউল", "schedule", "ক্যালেন্ডার", "calendar", "মিটিং", "meeting", "অ্যাপয়েন্টমেন্ট")
        if (triggers.none { lower.contains(it) }) return null
        if (lower.contains("সেট") || lower.contains("যোগ") || lower.contains("add ")) {
            // Creating events is delegated to the cloud tool so the model can parse details.
            return null
        }

        if (!calendarHelper.hasReadPermission()) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = "ক্যালেন্ডার পড়তে হলে আমাকে Calendar permission দিতে হবে — সেটিংস → Permissions থেকে চালু করে দাও।",
                emotion = ArohiEmotion.CONCERNED,
                toolName = "get_calendar_events"
            )
        }

        val events = calendarHelper.queryUpcomingEvents(days = 7, limit = 8)
        if (events.isEmpty()) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = "আগামী সাত দিনে ক্যালেন্ডারে কোনো ইভেন্ট নেই।",
                emotion = ArohiEmotion.CALM,
                toolName = "get_calendar_events"
            )
        }
        val summary = events.joinToString("\n") { event ->
            val dayText = dayLabel(event.begin)
            "• $dayText ${CalendarHelper.formatTime(event.begin)} — ${event.title}"
        }
        return LocalExecutionResult(
            isHandled = true,
            responseText = "আগামী দিনগুলোর শিডিউল:\n$summary",
            emotion = ArohiEmotion.FOCUSED,
            toolName = "get_calendar_events"
        )
    }

    private fun dayLabel(timestamp: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
        val sameDay = sameYear && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        val days = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহস্পতি", "শুক্র", "শনি")
        if (sameDay) return "আজ"
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        if (sameYear && tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) return "কাল"
        return "${days.getOrElse(target.get(Calendar.DAY_OF_WEEK) - 1) { "" }}বার"
    }

    // ------------------------------------------------------------------ to-dos

    private suspend fun todos(lower: String, original: String, normalized: String): LocalExecutionResult? {
        val todoTriggers = listOf("টু-ডু", "টুডু", "to-do", "todo", "কাজের তালিকা", "কাজের লিস্ট", "টাস্ক লিস্ট")
        if (todoTriggers.none { lower.contains(it) } && !lower.contains("লিস্টে যোগ")) return null

        val addWords = listOf("যোগ", "add", "লিখে রাখ", "নোট কর", "বসাও")
        val listWords = listOf("দেখাও", "দেখা", "কি কি", "কী কী", "list", "show", "কতগুলো")
        val doneWords = listOf("শেষ", "সম্পন্ন", "করে ফেলেছি", "done", "complete")

        if (listWords.any { lower.contains(it) }) {
            val pending = todoRepository.getPendingTodos()
            if (pending.isEmpty()) {
                return LocalExecutionResult(
                    isHandled = true,
                    responseText = "টু-ডু লিস্ট এখন খালি — সব কাজ শেষ!",
                    emotion = ArohiEmotion.HAPPY,
                    toolName = "list_todos"
                )
            }
            val summary = pending.take(7).joinToString("\n") { "• ${it.title}" }
            return LocalExecutionResult(
                isHandled = true,
                responseText = "তোমার ${TimePhraseParser.toBengaliDigits(pending.size.toString())}টা কাজ বাকি:\n$summary",
                emotion = ArohiEmotion.FOCUSED,
                toolName = "list_todos"
            )
        }

        if (doneWords.any { lower.contains(it) }) {
            val candidate = cleanTodoTitle(original, todoTriggers + doneWords + listOf("করা", "হয়ে গেছে", "করে"))
            val completed = todoRepository.completeByTitle(candidate)
            return LocalExecutionResult(
                isHandled = true,
                responseText = if (completed) "'$candidate' কাজটা সম্পন্ন হিসেবে টিক দিয়েছি।" else
                    "'$candidate' নামে কোনো খোলা কাজ পাইনি।",
                emotion = if (completed) ArohiEmotion.HAPPY else ArohiEmotion.CONFUSED,
                toolName = "complete_todo"
            )
        }

        if (addWords.any { lower.contains(it) }) {
            val priority = when {
                lower.contains("জরুরি") || lower.contains("urgent") -> 2
                lower.contains("পরে") || lower.contains("low priority") -> 0
                else -> 1
            }
            val dueAt = TimePhraseParser.parse(normalized)
            val title = cleanTodoTitle(original, todoTriggers + addWords)
            if (title.isBlank()) {
                return LocalExecutionResult(
                    isHandled = true,
                    responseText = "কী কাজটা যোগ করতে হবে বলো।",
                    emotion = ArohiEmotion.THINKING,
                    toolName = "add_todo"
                )
            }
            todoRepository.addTodo(title, priority = priority, dueAt = dueAt)
            val dueText = if (dueAt != null) " (${ReminderScheduler.formatBengaliDateTime(dueAt)})" else ""
            return LocalExecutionResult(
                isHandled = true,
                responseText = "'$title' টু-ডু লিস্টে যোগ করেছি$dueText।",
                emotion = ArohiEmotion.HAPPY,
                toolName = "add_todo"
            )
        }
        return null
    }

    private fun cleanTodoTitle(original: String, noisy: List<String>): String {
        var title = original
        for (phrase in noisy) {
            title = title.replace(phrase, " ", ignoreCase = true)
        }
        title = title.replace(
            Regex(
                "(আমার|আমাকে|একটা|একটি|লিস্টে|তালিকায়|কাজটা|কাজটি|" +
                    "\\bthe\\b|\\ba\\b|\\ban\\b|\\bmy\\b|\\bto\\b|\\bin\\b|\\bon\\b)",
                RegexOption.IGNORE_CASE
            ),
            " "
        )
        title = title.replace(Regex("[০-৯0-9]{1,4}\\s*(মিনিট|ঘন্টা|ঘণ্টা|minutes?|hours?|hrs?)"), " ")
        title = title.replace(Regex("(আজ|আগামীকাল|কাল|পরশু|সকাল|দুপুর|বিকাল|বিকেল|সন্ধ্যা|রাত|টা|টায়|পরে)"), " ")
        title = title.replace(Regex("\\s+"), " ").trim(' ', ',', '.', '।', '-', ':', '"', '\'')
        return title.take(90)
    }

    // ------------------------------------------------------------------ notes

    private suspend fun notes(lower: String, original: String, normalized: String): LocalExecutionResult? {
        if (lower.contains("নোটিফিকেশন") || lower.contains("notification")) return null
        val noteTriggers = listOf("নোট", "note")
        if (noteTriggers.none { lower.contains(it) }) return null

        if (lower.contains("দেখাও") || lower.contains("পড়ে শোনাও") || lower.contains("list") || lower.contains("show")) {
            val notes = noteRepository.getNotes()
            if (notes.isEmpty()) {
                return LocalExecutionResult(
                    isHandled = true,
                    responseText = "এখনো কোনো নোট নেই। 'নোট লেখো ...' বললে আমি লিখে রাখব।",
                    emotion = ArohiEmotion.CALM,
                    toolName = "list_notes"
                )
            }
            val summary = notes.take(5).joinToString("\n") { "• ${it.title}: ${it.content.take(60)}" }
            return LocalExecutionResult(
                isHandled = true,
                responseText = "তোমার নোটগুলো:\n$summary",
                emotion = ArohiEmotion.FOCUSED,
                toolName = "list_notes"
            )
        }

        if (lower.contains("মুছে") || lower.contains("ডিলিট") || lower.contains("delete")) {
            val query = cleanNoteText(original).ifBlank { "" }
            val match = noteRepository.search(query).firstOrNull()
            return if (match != null) {
                noteRepository.deleteNote(match.id)
                LocalExecutionResult(
                    isHandled = true,
                    responseText = "'${match.title}' নোটটা মুছে দিয়েছি।",
                    emotion = ArohiEmotion.CALM,
                    toolName = "delete_note"
                )
            } else {
                LocalExecutionResult(
                    isHandled = true,
                    responseText = "'$query' নামে কোনো নোট পাইনি।",
                    emotion = ArohiEmotion.CONFUSED,
                    toolName = "delete_note"
                )
            }
        }

        val content = cleanNoteText(original)
        if (content.isBlank()) return null
        val title = content.split(" ").take(6).joinToString(" ")
        noteRepository.addNote(title, content)
        return LocalExecutionResult(
            isHandled = true,
            responseText = "নোটটা লিখে রেখেছি: \"${content.take(80)}\"",
            emotion = ArohiEmotion.HAPPY,
            toolName = "add_note"
        )
    }

    private fun cleanNoteText(original: String): String {
        var text = original
        for (phrase in listOf(
            "নোট লেখো", "নোট লিখো", "নোট করো", "নোট যোগ করো", "নোট নাও", "নোট দেখাও",
            "নোট মুছে ফেলো", "নোট ডিলিট করো", "নোট", "note down", "note", "write down"
        )) {
            text = text.replace(phrase, " ", ignoreCase = true)
        }
        text = text.replace(Regex("^(যে|এটা|এই|লিখে রাখো|মনে রাখো)\\s*"), "")
        return text.replace(Regex("\\s+"), " ").trim(' ', ':', '-', '।', '.', '"', '\'')
    }

    // ------------------------------------------------------------------ weather

    private suspend fun weather(lower: String): LocalExecutionResult? {
        val triggers = listOf("আবহাওয়া", "আবহাওয়া", "weather", "বৃষ্টি", "তাপমাত্রা", "temperature", "গরম লাগছে", "ঠান্ডা লাগছে", "ছাতা")
        if (triggers.none { lower.contains(it) }) return null

        val snapshot = weatherRepository.refresh()
        if (snapshot == null) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = "আবহাওয়ার ডেটা আনতে পারছি না — ইন্টারনেট চালু আছে কিনা দেখো, আর সেটিংসে তোমার শহরের নাম দিয়ে রাখো।",
                emotion = ArohiEmotion.CONCERNED,
                toolName = "get_weather"
            )
        }
        val extra = when {
            snapshot.rainChancePercent >= 50 -> " ছাতা নিয়ে বেরোনো, বৃষ্টি হতে পারে।"
            snapshot.maxC >= 35 -> " গরম বেশি, পানি খেতে ভুলো না।"
            snapshot.minC <= 15 -> " ঠান্ডা পড়েছে, গরম কাপড় পরে নিও।"
            else -> ""
        }
        return LocalExecutionResult(
            isHandled = true,
            responseText = snapshot.spokenSummary() + extra,
            emotion = if (snapshot.rainChancePercent >= 50) ArohiEmotion.CONCERNED else ArohiEmotion.CALM,
            toolName = "get_weather"
        )
    }

    // ------------------------------------------------------------------ translation

    private fun translation(lower: String, original: String): LocalExecutionResult? {
        if (!PhraseBook.looksLikeTranslation(lower)) return null
        val wantsEnglish = lower.contains("ইংরেজি") || lower.contains("english")
        val source = PhraseBook.extractSourceText(original)
        if (source.isBlank()) return null
        val translated = PhraseBook.translate(source, if (wantsEnglish) "en" else "bn") ?: return null
        return LocalExecutionResult(
            isHandled = true,
            responseText = if (wantsEnglish) "\"$source\" — in English: \"$translated\"" else "\"$source\" — বাংলায়: \"$translated\"",
            emotion = ArohiEmotion.CALM,
            toolName = "translate_text"
        )
    }

    // ------------------------------------------------------------------ maths & conversion

    private suspend fun maths(lower: String, normalized: String): LocalExecutionResult? {
        if (CurrencyConverter.looksLikeCurrencyConversion(normalized)) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = CurrencyConverter.convertFromText(normalized),
                emotion = ArohiEmotion.CALM,
                toolName = "convert_currency"
            )
        }
        if (UnitConverter.looksLikeConversion(lower)) {
            return LocalExecutionResult(
                isHandled = true,
                responseText = UnitConverter.convertFromText(normalized),
                emotion = ArohiEmotion.CALM,
                toolName = "convert_units"
            )
        }
        if (CalculatorEngine.looksLikeCalculation(lower) && !isReminderRequest(lower)) {
            return when (val result = CalculatorEngine.calculate(normalized)) {
                is CalculatorEngine.Result.Success -> LocalExecutionResult(
                    isHandled = true,
                    responseText = "${result.expression}",
                    emotion = ArohiEmotion.CALM,
                    toolName = "calculate"
                )
                is CalculatorEngine.Result.Failure -> null
            }
        }
        return null
    }

    private suspend fun unitConversion(lower: String, normalized: String): LocalExecutionResult? {
        if (!UnitConverter.looksLikeConversion(lower)) return null
        return LocalExecutionResult(
            isHandled = true,
            responseText = UnitConverter.convertFromText(normalized),
            emotion = ArohiEmotion.CALM,
            toolName = "convert_units"
        )
    }

    private fun notHandled() = LocalExecutionResult(isHandled = false, responseText = "")
}
