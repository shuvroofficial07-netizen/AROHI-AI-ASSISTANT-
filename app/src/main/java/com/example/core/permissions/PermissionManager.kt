package com.example.core.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.ArohiAccessibilityService
import com.example.service.ArohiNotificationListenerService

/** Real grant status of a permission / special service access. */
enum class PermissionStatus { GRANTED, DENIED, NOT_REQUESTED, NOT_SUPPORTED_ON_DEVICE, SERVICE_DISABLED }

/**
 * Declarative description of one permission Arohi needs (spec §64):
 * its status, why it is required, and how to grant it.
 */
data class PermissionRequirement(
    val id: String,
    val displayName: String,
    val whyRequired: String,
    val isRuntimePermission: Boolean,
    val androidPermission: String? = null,
    /** Set for special-access items (accessibility, notification access, battery, overlays…). */
    val specialService: SpecialService? = null,
    val optional: Boolean = false
)

enum class SpecialService {
    ACCESSIBILITY, NOTIFICATION_ACCESS, BATTERY_OPTIMIZATION, OVERLAY, WRITE_SETTINGS
}

/**
 * Central, single source of truth for every permission / special access Arohi
 * uses (spec §64). It reports the REAL grant state read from the system — never
 * assumed. The UI renders each item with status, rationale and a grant action.
 */
class PermissionManager(private val context: Context) {

    private val appContext = context.applicationContext

    val requirements: List<PermissionRequirement> = listOf(
        PermissionRequirement(
            id = "microphone", displayName = "Microphone",
            whyRequired = "Needed to hear your voice commands (speech recognition).",
            isRuntimePermission = true, androidPermission = Manifest.permission.RECORD_AUDIO
        ),
        PermissionRequirement(
            id = "camera", displayName = "Camera",
            whyRequired = "Used for vision commands, QR scanning and flashlight.",
            isRuntimePermission = true, androidPermission = Manifest.permission.CAMERA,
            optional = true
        ),
        PermissionRequirement(
            id = "contacts", displayName = "Contacts",
            whyRequired = "Resolve contact names when you ask to call or message someone.",
            isRuntimePermission = true, androidPermission = Manifest.permission.READ_CONTACTS,
            optional = true
        ),
        PermissionRequirement(
            id = "call_phone", displayName = "Phone (direct call)",
            whyRequired = "Place calls directly; without it Arohi opens the dialer for you to confirm.",
            isRuntimePermission = true, androidPermission = Manifest.permission.CALL_PHONE,
            optional = true
        ),
        PermissionRequirement(
            id = "notifications", displayName = "Post Notifications",
            whyRequired = "Shows the foreground-service status, timers and reminders.",
            isRuntimePermission = true,
            androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
            optional = true
        ),
        PermissionRequirement(
            id = "location", displayName = "Location",
            whyRequired = "Nearby places, navigation and location-based routines.",
            isRuntimePermission = true, androidPermission = Manifest.permission.ACCESS_FINE_LOCATION,
            optional = true
        ),
        PermissionRequirement(
            id = "accessibility", displayName = "Accessibility Service",
            whyRequired = "Enables real screen automation: back/home, tap, scroll and reading UI elements.",
            isRuntimePermission = false, specialService = SpecialService.ACCESSIBILITY,
            optional = true
        ),
        PermissionRequirement(
            id = "notification_access", displayName = "Notification Access",
            whyRequired = "Lets Arohi read and summarize your notifications.",
            isRuntimePermission = false, specialService = SpecialService.NOTIFICATION_ACCESS,
            optional = true
        ),
        PermissionRequirement(
            id = "battery_optimization", displayName = "Battery Optimization Exemption",
            whyRequired = "Keeps the voice assistant reliable in the background.",
            isRuntimePermission = false, specialService = SpecialService.BATTERY_OPTIMIZATION,
            optional = true
        ),
        PermissionRequirement(
            id = "write_settings", displayName = "Modify System Settings",
            whyRequired = "Change screen brightness directly; otherwise Arohi opens the system slider.",
            isRuntimePermission = false, specialService = SpecialService.WRITE_SETTINGS,
            optional = true
        )
    )

    fun isGranted(requirement: PermissionRequirement): Boolean = when {
        requirement.specialService != null -> isSpecialServiceEnabled(requirement.specialService)
        requirement.androidPermission != null -> hasRuntimePermission(requirement.androidPermission)
        else -> true
    }

    fun status(requirement: PermissionRequirement): PermissionStatus {
        if (requirement.androidPermission != null && Build.VERSION.SDK_INT < 23) {
            // Pre-marshmallow: install-time permissions are granted at install.
            return if (hasRuntimePermission(requirement.androidPermission)) PermissionStatus.GRANTED
            else PermissionStatus.NOT_SUPPORTED_ON_DEVICE
        }
        return if (isGranted(requirement)) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    fun hasRuntimePermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private fun isSpecialServiceEnabled(service: SpecialService): Boolean = when (service) {
        SpecialService.ACCESSIBILITY ->
            ArohiAccessibilityService.isServiceRunning() ||
                runCatching { ArohiAccessibilityService.isAccessibilityPermissionGranted(appContext) }.getOrDefault(false)
        SpecialService.NOTIFICATION_ACCESS ->
            ArohiNotificationListenerService.isConnected ||
                runCatching {
                    val flat = Settings.Secure.getString(appContext.contentResolver, "enabled_notification_listeners") ?: ""
                    flat.contains(appContext.packageName)
                }.getOrDefault(false)
        SpecialService.BATTERY_OPTIMIZATION ->
            runCatching {
                val pm = appContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pm?.isIgnoringBatteryOptimizations(appContext.packageName) == true
                } else true
            }.getOrDefault(false)
        SpecialService.OVERLAY ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(appContext) else true
        SpecialService.WRITE_SETTINGS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.System.canWrite(appContext) else true
    }

    /** Notifications enabled at the system channel level (API 26+). */
    fun areNotificationsEnabled(): Boolean = runCatching {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.areNotificationsEnabled() ?: true
    }.getOrDefault(true)

    fun requirementById(id: String): PermissionRequirement? = requirements.firstOrNull { it.id == id }

    fun grantedCount(): Int = requirements.count { isGranted(it) }
}
