package com.example.privacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.ArohiApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * User-controlled data export / deletion. The export is a plain JSON document written into the
 * app's own external files directory and shared through a FileProvider — no hidden uploads.
 */
object DataExporter {

    suspend fun buildExportJson(app: ArohiApplication): String = withContext(Dispatchers.IO) {
        val messages = runCatching { app.conversationRepository.allMessages.first() }.getOrDefault(emptyList())
        val memories = runCatching { app.memoryRepository.allMemories.first() }.getOrDefault(emptyList())
        val reminders = runCatching { app.reminderRepository.allReminders.first() }.getOrDefault(emptyList())
        val todos = runCatching { app.todoRepository.allTodos.first() }.getOrDefault(emptyList())
        val notes = runCatching { app.noteRepository.allNotes.first() }.getOrDefault(emptyList())
        val routines = runCatching { app.routineRepository.allRoutines.first() }.getOrDefault(emptyList())
        val notifications = runCatching { app.notificationRepository.allNotifications.first() }.getOrDefault(emptyList())

        val builder = StringBuilder()
        builder.append("{\n")
        builder.append("  \"app\": \"AROHI AI Assistant\",\n")
        builder.append("  \"exportedAt\": ${System.currentTimeMillis()},\n")
        builder.append("  \"settings\": {\n")
        builder.append("    \"pronounStyle\": ${quote(app.settingsRepository.getPronounStyle())},\n")
        builder.append("    \"personalityIntensity\": ${app.settingsRepository.getPersonalityIntensity()},\n")
        builder.append("    \"userName\": ${quote(app.settingsRepository.getUserName())},\n")
        builder.append("    \"themeAccent\": ${quote(app.settingsRepository.getThemeAccent())},\n")
        builder.append("    \"avatarMode\": ${quote(app.settingsRepository.getAvatarMode())},\n")
        builder.append("    \"voiceSpeed\": ${app.settingsRepository.getVoiceSpeed()},\n")
        builder.append("    \"voicePitch\": ${app.settingsRepository.getVoicePitch()}\n")
        builder.append("  },\n")

        builder.append("  \"memories\": [\n")
        memories.forEachIndexed { index, memory ->
            builder.append(
                "    {\"category\": ${quote(memory.category)}, \"key\": ${quote(memory.key)}, " +
                    "\"value\": ${quote(memory.value)}}"
            )
            if (index != memories.lastIndex) builder.append(",")
            builder.append("\n")
        }
        builder.append("  ],\n")

        builder.append("  \"messages\": [\n")
        messages.forEachIndexed { index, message ->
            builder.append(
                "    {\"role\": ${quote(message.role)}, \"content\": ${quote(message.content)}, " +
                    "\"timestamp\": ${message.timestamp}}"
            )
            if (index != messages.lastIndex) builder.append(",")
            builder.append("\n")
        }
        builder.append("  ],\n")

        builder.append("  \"reminders\": [\n")
        reminders.forEachIndexed { index, reminder ->
            builder.append(
                "    {\"title\": ${quote(reminder.title)}, \"triggerAt\": ${reminder.triggerAt}, " +
                    "\"repeatRule\": ${quote(reminder.repeatRule)}, \"isCompleted\": ${reminder.isCompleted}}"
            )
            if (index != reminders.lastIndex) builder.append(",")
            builder.append("\n")
        }
        builder.append("  ],\n")

        builder.append("  \"todos\": [\n")
        todos.forEachIndexed { index, todo ->
            builder.append(
                "    {\"title\": ${quote(todo.title)}, \"isDone\": ${todo.isDone}, " +
                    "\"createdAt\": ${todo.createdAt}}"
            )
            if (index != todos.lastIndex) builder.append(",")
            builder.append("\n")
        }
        builder.append("  ],\n")

        builder.append("  \"notes\": [\n")
        notes.forEachIndexed { index, note ->
            builder.append(
                "    {\"title\": ${quote(note.title)}, \"content\": ${quote(note.content)}, " +
                    "\"updatedAt\": ${note.updatedAt}}"
            )
            if (index != notes.lastIndex) builder.append(",")
            builder.append("\n")
        }
        builder.append("  ],\n")

        builder.append("  \"routines\": [\n")
        routines.forEachIndexed { index, routine ->
            builder.append(
                "    {\"name\": ${quote(routine.name)}, \"trigger\": ${quote(routine.triggerPhrase)}, " +
                    "\"enabled\": ${routine.isEnabled}}"
            )
            if (index != routines.lastIndex) builder.append(",")
            builder.append("\n")
        }
        builder.append("  ],\n")

        builder.append("  \"capturedNotifications\": ${notifications.size}\n")
        builder.append("}\n")
        builder.toString()
    }

    suspend fun writeExportFile(context: Context, app: ArohiApplication): File? = withContext(Dispatchers.IO) {
        try {
            val json = buildExportJson(app)
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, "exports")
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, "arohi-export-$stamp.json")
            file.writeText(json)
            file
        } catch (e: Exception) {
            null
        }
    }

    fun shareExport(context: Context, file: File): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AROHI data export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "AROHI export শেয়ার করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Deletes every locally stored message, memory, reminder, to-do, note and routine. */
    suspend fun deleteAllLocalData(app: ArohiApplication) = withContext(Dispatchers.IO) {
        runCatching { app.conversationRepository.clearHistory() }
        runCatching { app.memoryRepository.clearAll() }
        runCatching { app.reminderRepository.clearAll() }
        runCatching { app.todoRepository.clearAll() }
        runCatching { app.noteRepository.clearAll() }
        runCatching { app.notificationRepository.clearAll() }
        Unit
    }

    private fun quote(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
