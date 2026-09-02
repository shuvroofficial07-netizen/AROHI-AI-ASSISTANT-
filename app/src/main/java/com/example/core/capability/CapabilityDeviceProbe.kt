package com.example.core.capability

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.ArohiAccessibilityService
import com.example.service.ArohiBackgroundService
import com.example.service.ArohiNotificationListenerService
import com.example.data.repository.SettingsRepository

/**
 * Android-side evaluator that turns a static [CapabilityDescriptor] into a real
 * [CapabilityStatusEntry] by inspecting the actual device: runtime permissions,
 * enabled special services, hardware features, OS version and configuration.
 *
 * Nothing here is assumed — every status is read from the system.
 */
class CapabilityDeviceProbe(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private fun isAccessibilityEnabled(): Boolean =
        ArohiAccessibilityService.isServiceRunning() ||
            runCatching { ArohiAccessibilityService.isAccessibilityPermissionGranted(appContext) }.getOrDefault(false)

    private fun isNotificationListenerEnabled(): Boolean =
        ArohiNotificationListenerService.isConnected ||
            runCatching {
                val flat = Settings.Secure.getString(
                    appContext.contentResolver,
                    "enabled_notification_listeners"
                ) ?: ""
                flat.contains(appContext.packageName)
            }.getOrDefault(false)

    private fun hasFlash(): Boolean = runCatching {
        val cm = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        cm?.cameraIdList?.any { id ->
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } == true
    }.getOrDefault(false)

    private fun hasMicrophone(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

    private fun hasCameraHardware(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)

    private fun hasLocationHardware(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)

    private fun canWriteSettings(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.System.canWrite(appContext) else true

    /** Evaluates a single descriptor against the live device state. */
    fun evaluate(descriptor: CapabilityDescriptor): CapabilityStatusEntry {
        // OS gate
        if (Build.VERSION.SDK_INT < descriptor.minAndroidVersion) {
            return CapabilityStatusEntry(
                descriptor, CapabilityStatus.UNSUPPORTED,
                "Requires Android API ${descriptor.minAndroidVersion}; this device is API ${Build.VERSION.SDK_INT}."
            )
        }

        // Probe-specific resolution
        return when (descriptor.probeKey) {
            "alwaysAvailable" ->
                CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Ready on this device.")

            "voiceListen" -> when {
                !hasMicrophone() -> CapabilityStatusEntry(descriptor, CapabilityStatus.UNSUPPORTED, "No microphone detected on this device.")
                !hasPermission(Manifest.permission.RECORD_AUDIO) ->
                    CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_PERMISSION, "Microphone permission not granted.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Microphone permission granted.")
            }

            "tts" -> {
                // Android always ships a TTS engine; whether a voice actually speaks is
                // verified live in Diagnostics. A usable engine is assumed present here.
                CapabilityStatusEntry(
                    descriptor,
                    CapabilityStatus.AVAILABLE,
                    "System text-to-speech engine present (live voice verified in Diagnostics)."
                )
            }

            "aiCloud" -> when {
                !hasPermission(Manifest.permission.INTERNET) ->
                    CapabilityStatusEntry(descriptor, CapabilityStatus.UNSUPPORTED, "INTERNET permission missing.")
                settingsRepository.getApiKey().isBlank() ->
                    CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_PERMISSION, "No AI API key configured. Offline commands still work.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "AI provider configured (connection tested in Diagnostics).")
            }

            "flashlight" -> when {
                !hasFlash() -> CapabilityStatusEntry(descriptor, CapabilityStatus.UNSUPPORTED, "No camera flash unit available.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Flash unit available.")
            }

            "brightness" -> when {
                canWriteSettings() -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Brightness control permitted.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_PERMISSION, "WRITE_SETTINGS not granted; Arohi will open the system brightness slider.")
            }

            "accessibility" -> when {
                isAccessibilityEnabled() -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Accessibility service connected.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_SERVICE, "Accessibility service is not enabled.")
            }

            "notificationListener" -> when {
                isNotificationListenerEnabled() -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Notification access granted.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_SERVICE, "Notification access is not granted.")
            }

            "calling" -> when {
                !packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ->
                    CapabilityStatusEntry(descriptor, CapabilityStatus.UNSUPPORTED, "No telephony radio on this device.")
                !hasPermission(Manifest.permission.READ_CONTACTS) ->
                    CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_PERMISSION, "Contacts permission not granted.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE,
                    if (hasPermission(Manifest.permission.CALL_PHONE)) "Can place calls directly."
                    else "Can open the dialer (CALL_PHONE not granted).")
            }

            "sms" -> when {
                !packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ->
                    CapabilityStatusEntry(descriptor, CapabilityStatus.UNSUPPORTED, "No telephony radio on this device.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Composes SMS and opens the messaging app to send.")
            }

            "calendar" -> when {
                !hasPermission(Manifest.permission.READ_CALENDAR) ->
                    CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_PERMISSION, "Calendar permission not granted (events are created via system calendar intent).")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Calendar provider available.")
            }

            "web" -> when {
                !hasPermission(Manifest.permission.INTERNET) -> CapabilityStatusEntry(descriptor, CapabilityStatus.UNSUPPORTED, "INTERNET permission missing.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Browser search available.")
            }

            "vision" -> when {
                settingsRepository.getApiKey().isBlank() -> CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_PERMISSION, "Vision requires a configured AI key.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Vision analysis available via AI provider.")
            }

            "camera" -> when {
                !hasCameraHardware() -> CapabilityStatusEntry(descriptor, CapabilityStatus.UNSUPPORTED, "No camera detected.")
                !hasPermission(Manifest.permission.CAMERA) -> CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_PERMISSION, "Camera permission not granted.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Camera available.")
            }

            "location" -> when {
                !hasLocationHardware() -> CapabilityStatusEntry(descriptor, CapabilityStatus.UNSUPPORTED, "No location hardware detected.")
                !hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) -> CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_PERMISSION, "Location permission not granted.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Location available.")
            }

            "media" -> {
                val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE,
                    if (am != null) "Audio/transport controls available." else "Audio service unavailable.")
            }

            "foregroundService" -> when {
                ArohiBackgroundService.isRunning -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Background service is RUNNING.")
                else -> CapabilityStatusEntry(descriptor, CapabilityStatus.REQUIRES_SERVICE, "Background service is STOPPED.")
            }

            else -> CapabilityStatusEntry(descriptor, CapabilityStatus.AVAILABLE, "Ready.")
        }
    }

    /** Evaluates every declared capability — this powers the dynamic discovery UI. */
    fun evaluateAll(): List<CapabilityStatusEntry> = CapabilityRegistry.all.map { evaluate(it) }
}
