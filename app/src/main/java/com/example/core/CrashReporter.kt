package com.example.core

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records real uncaught exceptions to a local file and then hands the throwable to the
 * platform default handler.
 *
 * IMPORTANT: this does NOT swallow crashes and does NOT show a fake "everything is fine"
 * screen. Android still terminates the process exactly as it normally would; we only persist
 * the real stack trace so the Diagnostics screen can show the user what actually happened on
 * the previous run (there is no adb/logcat on a normal user's phone).
 */
object CrashReporter {

    private const val TAG = "ArohiCrash"
    private const val FILE_NAME = "arohi_last_crash.txt"
    private const val MAX_BYTES = 64 * 1024

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                write(appContext, thread, throwable)
            } catch (t: Throwable) {
                Log.e(TAG, "Unable to persist crash report", t)
            }
            // Never suppress: let Android/the previous handler terminate the process.
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val stack = StringWriter().also { sw ->
            PrintWriter(sw).use { throwable.printStackTrace(it) }
        }.toString()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val report = buildString {
            appendLine("AROHI AI ASSISTANT crash report")
            appendLine("time: $timestamp")
            appendLine("thread: ${thread.name}")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("type: ${throwable.javaClass.name}")
            appendLine("message: ${throwable.message ?: "(none)"}")
            appendLine("---")
            append(stack.take(MAX_BYTES))
        }
        file(context).writeText(report)
        Log.e(TAG, "Fatal exception captured", throwable)
    }

    /** Returns the real crash report of the previous run, or null when there is none. */
    fun lastCrash(context: Context): String? {
        return try {
            val f = file(context)
            if (f.exists() && f.length() > 0L) f.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        try {
            file(context).delete()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to clear crash report: ${e.message}")
        }
    }

    private fun file(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)

    /**
     * Runs [block] and reports failure instead of taking the whole process down.
     * Used only for genuinely optional subsystems (never to hide a real crash path).
     */
    fun <T> safe(label: String, block: () -> T): T? = try {
        block()
    } catch (t: Throwable) {
        Log.e(TAG, "Subsystem '$label' failed: ${t.message}", t)
        null
    }
}
