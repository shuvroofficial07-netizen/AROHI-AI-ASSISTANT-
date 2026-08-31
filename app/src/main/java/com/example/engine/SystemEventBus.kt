package com.example.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SystemEventLevel { INFO, SUCCESS, WARNING, ERROR }

/**
 * A single real system event. Every entry is produced by an actual component
 * (service start, Gemini connect, notification captured, task executed...).
 * Nothing here is synthesized for the UI.
 */
data class SystemEvent(
    val id: Long = System.currentTimeMillis() * 1000 + (System.nanoTime() % 1000),
    val component: String,
    val message: String,
    val level: SystemEventLevel = SystemEventLevel.INFO,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formattedTime(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * App-scoped, in-memory stream of genuine system events. The UI shows the
 * latest entries; nothing is invented.
 */
class SystemEventBus(private val maxEntries: Int = 120) {
    private val _events = MutableStateFlow<List<SystemEvent>>(emptyList())
    val events: StateFlow<List<SystemEvent>> = _events.asStateFlow()

    fun log(component: String, message: String, level: SystemEventLevel = SystemEventLevel.INFO) {
        val entry = SystemEvent(component = component, message = message, level = level)
        _events.value = (listOf(entry) + _events.value).take(maxEntries)
    }

    fun clear() {
        _events.value = emptyList()
    }
}
