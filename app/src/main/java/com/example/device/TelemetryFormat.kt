package com.example.device

import com.example.device.DeviceStateManager.Companion.UNAVAILABLE
import com.example.device.DeviceStateManager.Companion.UNAVAILABLE_TEXT
import java.util.Locale

/**
 * Display helpers for real telemetry.
 *
 * Rule: when Android does not give us a value we print "Unavailable" — never an invented
 * number and never a stale placeholder.
 */

fun Int.percentOrUnavailable(): String =
    if (this < 0) UNAVAILABLE_TEXT else "$this%"

fun DeviceTelemetry.batteryText(): String = batteryPercent.percentOrUnavailable()

fun DeviceTelemetry.chargingText(): String = when {
    chargingType == UNAVAILABLE_TEXT -> UNAVAILABLE_TEXT
    isCharging -> "Charging"
    else -> "Discharging"
}

/** Used storage percentage, or null when the filesystem stats are unavailable. */
fun DeviceTelemetry.storageUsedPercent(): Int? {
    if (totalStorageGb <= 0.0 || freeStorageGb < 0.0) return null
    return (((totalStorageGb - freeStorageGb) / totalStorageGb) * 100).toInt().coerceIn(0, 100)
}

fun DeviceTelemetry.storageDetailText(): String {
    if (totalStorageGb <= 0.0 || freeStorageGb < 0.0) return UNAVAILABLE_TEXT
    val used = String.format(Locale.US, "%.1f", (totalStorageGb - freeStorageGb).coerceAtLeast(0.0))
    val total = String.format(Locale.US, "%.1f", totalStorageGb)
    return "$used GB / $total GB"
}

/** Used RAM percentage, or null when ActivityManager memory info is unavailable. */
fun DeviceTelemetry.ramUsedPercent(): Int? {
    if (totalRamMb <= 0L || freeRamMb < 0L) return null
    return (((totalRamMb - freeRamMb).toFloat() / totalRamMb) * 100).toInt().coerceIn(0, 100)
}

fun DeviceTelemetry.ramDetailText(): String {
    if (totalRamMb <= 0L || freeRamMb < 0L) return UNAVAILABLE_TEXT
    val used = String.format(Locale.US, "%.1f", (totalRamMb - freeRamMb) / 1024f)
    val total = String.format(Locale.US, "%.0f", totalRamMb / 1024f)
    return "$used GB / $total GB"
}

fun DeviceTelemetry.networkText(): String =
    if (networkType.isBlank()) UNAVAILABLE_TEXT else networkType

fun DeviceTelemetry.mediaVolumeText(): String = mediaVolumePercent.percentOrUnavailable()

fun DeviceTelemetry.isMediaVolumeReadable(): Boolean = mediaVolumePercent != UNAVAILABLE

fun Int?.orUnavailable(): String = if (this == null || this < 0) UNAVAILABLE_TEXT else "$this%"
