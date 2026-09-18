package com.example.device

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

data class CalendarEventInfo(
    val title: String,
    val begin: Long,
    val end: Long,
    val calendarName: String,
    val isAllDay: Boolean
)

/**
 * Real device-calendar integration through CalendarContract.
 */
class CalendarHelper(private val context: Context) {

    fun hasReadPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.READ_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED

    fun hasWritePermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.WRITE_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED

    suspend fun queryUpcomingEvents(days: Int = 7, limit: Int = 15): List<CalendarEventInfo> =
        withContext(Dispatchers.IO) {
            if (!hasReadPermission()) return@withContext emptyList()
            val now = System.currentTimeMillis()
            val end = now + days.toLong() * 24L * 60L * 60L * 1000L
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, now)
            ContentUris.appendId(builder, end)

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME
            )

            val results = mutableListOf<CalendarEventInfo>()
            try {
                context.contentResolver.query(
                    builder.build(),
                    projection,
                    null,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC"
                )?.use { cursor ->
                    val titleIndex = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                    val beginIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                    val endIndex = cursor.getColumnIndex(CalendarContract.Instances.END)
                    val allDayIndex = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                    val calendarIndex = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

                    while (cursor.moveToNext() && results.size < limit) {
                        results.add(
                            CalendarEventInfo(
                                title = if (titleIndex >= 0) cursor.getString(titleIndex) ?: "(শিরোনামহীন)" else "(শিরোনামহীন)",
                                begin = if (beginIndex >= 0) cursor.getLong(beginIndex) else 0L,
                                end = if (endIndex >= 0) cursor.getLong(endIndex) else 0L,
                                calendarName = if (calendarIndex >= 0) cursor.getString(calendarIndex) ?: "" else "",
                                isAllDay = if (allDayIndex >= 0) cursor.getInt(allDayIndex) == 1 else false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                return@withContext emptyList()
            }
            results
        }

    /** Writes an event straight into the device calendar when permission is granted. */
    suspend fun insertEvent(title: String, beginMillis: Long, durationMinutes: Int = 30): Long? =
        withContext(Dispatchers.IO) {
            if (!hasWritePermission()) return@withContext null
            try {
                val calendarId = primaryCalendarId() ?: return@withContext null
                val values = ContentValues().apply {
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DTSTART, beginMillis)
                    put(CalendarContract.Events.DTEND, beginMillis + durationMinutes * 60_000L)
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }
                context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)?.lastPathSegment?.toLongOrNull()
            } catch (e: Exception) {
                null
            }
        }

    private fun primaryCalendarId(): Long? {
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY),
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val primaryIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                var fallback: Long? = null
                while (cursor.moveToNext()) {
                    val id = if (idIndex >= 0) cursor.getLong(idIndex) else continue
                    if (fallback == null) fallback = id
                    if (primaryIndex >= 0 && cursor.getInt(primaryIndex) == 1) return@use id
                }
                fallback
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Fallback path: opens the system calendar "new event" screen with the values pre-filled. */
    fun openEventInsert(title: String, beginMillis: Long, durationMinutes: Int = 30): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, beginMillis + durationMinutes * 60_000L)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        /** Builds an absolute timestamp from "today/tomorrow at hour:minute". */
        fun timestampAt(hour: Int, minute: Int, dayOffset: Int = 0): Long {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }

        fun formatTime(timestamp: Long): String {
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val minuteText = if (minute < 10) "0$minute" else "$minute"
            return "$hour:$minuteText"
        }
    }
}
