package com.example.privacy

import android.content.Context
import android.provider.Settings
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Optional Firestore backup of the user's own data (memories, to-dos, notes, reminders).
 *
 * The feature is a no-op unless a google-services.json is present and Firebase initialised itself,
 * so the app never breaks when the developer has not wired Firebase yet. It is OFF by default and
 * can be toggled from the Privacy Center.
 */
object CloudSyncManager {

    fun isAvailable(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun deviceDocumentId(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
        return androidId?.takeIf { it.isNotBlank() } ?: "arohi-default-device"
    }

    suspend fun upload(context: Context, payloadJson: String): Result<Long> {
        if (!isAvailable(context)) {
            return Result.failure(IllegalStateException("Firebase কনফিগার করা নেই (google-services.json পাওয়া যায়নি)"))
        }
        val timestamp = System.currentTimeMillis()
        return try {
            val document = FirebaseFirestore.getInstance()
                .collection("arohi_users")
                .document(deviceDocumentId(context))
            val data = mapOf(
                "payload" to payloadJson,
                "updatedAt" to timestamp,
                "appVersion" to "14.0.0"
            )
            awaitTask { document.set(data) }
            Result.success(timestamp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun download(context: Context): Result<String> {
        if (!isAvailable(context)) {
            return Result.failure(IllegalStateException("Firebase কনফিগার করা নেই (google-services.json পাওয়া যায়নি)"))
        }
        return try {
            val document = FirebaseFirestore.getInstance()
                .collection("arohi_users")
                .document(deviceDocumentId(context))
            val snapshot = awaitTask { document.get() }
            val payload = snapshot?.getString("payload")
            if (payload.isNullOrBlank()) {
                Result.failure(IllegalStateException("ক্লাউডে কোনো ব্যাকআপ পাওয়া যায়নি"))
            } else {
                Result.success(payload)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> awaitTask(block: () -> com.google.android.gms.tasks.Task<T>): T? =
        suspendCancellableCoroutine { continuation ->
            try {
                val task = block()
                task.addOnSuccessListener { result -> continuation.resume(result) }
                task.addOnFailureListener { error -> continuation.resume(null) }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
}
