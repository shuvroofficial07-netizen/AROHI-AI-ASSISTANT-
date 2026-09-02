package com.example.core.capability

/** Live availability of a capability on THIS device, evaluated at runtime. */
enum class CapabilityStatus {
    /** Fully usable right now. */
    AVAILABLE,
    /** Present but blocked behind a runtime permission the user can grant. */
    REQUIRES_PERMISSION,
    /** Present but blocked behind a special service (accessibility / notif access / …). */
    REQUIRES_SERVICE,
    /** Disabled by the user's settings/policy. */
    DISABLED,
    /** Cannot run on this device/OS — report transparently, never fake it. */
    UNSUPPORTED;

    val usable: Boolean get() = this == AVAILABLE
}

/** High-level grouping for the discovery UI. */
enum class CapabilityCategory {
    VOICE, AI, DEVICE, AUTOMATION, SCREEN, APPS, COMMUNICATION,
    TIME, NOTIFICATIONS, MEMORY, WEB, VISION, LOCATION, MEDIA,
    SECURITY, SYSTEM, MODES, PERSONALITY
}

/**
 * Static declaration of a single Arohi capability (spec §76). The registry is
 * intentionally data-driven: instead of a hard-coded feature count, the UI
 * enumerates whatever the device can actually support.
 */
data class CapabilityDescriptor(
    val id: String,
    val name: String,
    val category: CapabilityCategory,
    val description: String,
    val voiceCommands: List<String> = emptyList(),
    val requiredPermissions: List<String> = emptyList(),
    /** Special services required, e.g. "accessibility", "notification_listener". */
    val requiredServices: List<String> = emptyList(),
    val minAndroidVersion: Int = 24,
    /** Name of a probe method in CapabilityDeviceProbe used to evaluate status. */
    val probeKey: String,
    /** True when the action mutates device state and may need confirmation. */
    val sensitive: Boolean = false
)

/** A descriptor plus its evaluated runtime status and human detail. */
data class CapabilityStatusEntry(
    val descriptor: CapabilityDescriptor,
    val status: CapabilityStatus,
    val detail: String
) {
    val usable: Boolean get() = status.usable
}
