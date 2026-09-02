package com.example.core.capability

/**
 * The single source of truth for every capability Arohi can expose (spec §76).
 *
 * The list is static declaration only; runtime availability is resolved by
 * [com.example.core.capability.CapabilityDeviceProbe] (Android side) so the UI
 * always reflects the real device rather than a fixed feature count.
 */
object CapabilityRegistry {

    val all: List<CapabilityDescriptor> = listOf(
        CapabilityDescriptor(
            id = "voice_listen",
            name = "Voice Listening",
            category = CapabilityCategory.VOICE,
            description = "Push-to-talk and continuous speech recognition with partial results.",
            voiceCommands = listOf("ট্যাপ করে কথা বলুন", "Hey Arohi…"),
            requiredPermissions = listOf("android.permission.RECORD_AUDIO"),
            probeKey = "voiceListen"
        ),
        CapabilityDescriptor(
            id = "voice_tts",
            name = "Voice Speaking (TTS)",
            category = CapabilityCategory.VOICE,
            description = "Speaks responses aloud through the system text-to-speech engine.",
            probeKey = "tts"
        ),
        CapabilityDescriptor(
            id = "ai_cloud",
            name = "Cloud AI Brain",
            category = CapabilityCategory.AI,
            description = "Reasoning, multi-step planning and conversation via the configured AI provider.",
            requiredPermissions = listOf("android.permission.INTERNET"),
            probeKey = "aiCloud"
        ),
        CapabilityDescriptor(
            id = "ai_offline",
            name = "Offline Commands",
            category = CapabilityCategory.AI,
            description = "Local commands (torch, volume, timers, app launch) that work without internet.",
            probeKey = "alwaysAvailable"
        ),
        CapabilityDescriptor(
            id = "memory",
            name = "Personal Memory",
            category = CapabilityCategory.MEMORY,
            description = "Remembers facts, preferences, aliases and routines in encrypted local storage.",
            probeKey = "alwaysAvailable"
        ),
        CapabilityDescriptor(
            id = "launch_app",
            name = "Launch Apps",
            category = CapabilityCategory.APPS,
            description = "Opens installed applications by name or spoken alias.",
            voiceCommands = listOf("YouTube খোলো", "Chrome খুলে দাও"),
            probeKey = "alwaysAvailable"
        ),
        CapabilityDescriptor(
            id = "device_telemetry",
            name = "Device Status",
            category = CapabilityCategory.DEVICE,
            description = "Battery, storage, RAM, network and brightness readings.",
            voiceCommands = listOf("ব্যাটারি কত?", "স্টোরেজ দেখাও"),
            probeKey = "alwaysAvailable"
        ),
        CapabilityDescriptor(
            id = "flashlight",
            name = "Flashlight",
            category = CapabilityCategory.DEVICE,
            description = "Turns the rear camera torch on/off (requires a flash unit).",
            voiceCommands = listOf("টর্চ জ্বালাও", "flashlight off"),
            probeKey = "flashlight"
        ),
        CapabilityDescriptor(
            id = "volume_control",
            name = "Volume Control",
            category = CapabilityCategory.DEVICE,
            description = "Sets media and ring volume.",
            voiceCommands = listOf("ভলিউম ৫০% করো"),
            probeKey = "alwaysAvailable"
        ),
        CapabilityDescriptor(
            id = "brightness",
            name = "Brightness",
            category = CapabilityCategory.DEVICE,
            description = "Reads brightness; changing it opens the official system slider (WRITE_SETTINGS).",
            probeKey = "brightness"
        ),
        CapabilityDescriptor(
            id = "sound_mode",
            name = "Sound / DND Mode",
            category = CapabilityCategory.DEVICE,
            description = "Mute / vibrate via audio manager; full DND opens the system settings screen.",
            probeKey = "alwaysAvailable"
        ),
        CapabilityDescriptor(
            id = "accessibility",
            name = "Screen Automation",
            category = CapabilityCategory.AUTOMATION,
            description = "Back/home/recents, scroll, tap, text input and UI inspection via AccessibilityService.",
            voiceCommands = listOf("পিছনে যাও", "Login button চাপো"),
            requiredServices = listOf("accessibility"),
            probeKey = "accessibility",
            sensitive = true
        ),
        CapabilityDescriptor(
            id = "screen_read",
            name = "Read Screen",
            category = CapabilityCategory.SCREEN,
            description = "Extracts visible text and clickable elements from the current screen.",
            requiredServices = listOf("accessibility"),
            voiceCommands = listOf("স্ক্রিনে কী আছে?"),
            probeKey = "accessibility"
        ),
        CapabilityDescriptor(
            id = "call",
            name = "Phone Calls",
            category = CapabilityCategory.COMMUNICATION,
            description = "Search contacts and place calls (or open the dialer when CALL_PHONE is not granted).",
            voiceCommands = listOf("Rahim-কে call করো"),
            requiredPermissions = listOf("android.permission.READ_CONTACTS", "android.permission.CALL_PHONE"),
            probeKey = "calling",
            sensitive = true
        ),
        CapabilityDescriptor(
            id = "sms",
            name = "SMS",
            category = CapabilityCategory.COMMUNICATION,
            description = "Composes SMS; sending opens the default messaging app for user confirmation.",
            requiredPermissions = listOf("android.permission.SEND_SMS", "android.permission.READ_CONTACTS"),
            probeKey = "sms",
            sensitive = true
        ),
        CapabilityDescriptor(
            id = "notification_access",
            name = "Notification AI",
            category = CapabilityCategory.NOTIFICATIONS,
            description = "Reads, classifies and summarizes notifications through NotificationListenerService.",
            voiceCommands = listOf("নোটিফিকেশন summarize করো"),
            requiredServices = listOf("notification_listener"),
            probeKey = "notificationListener"
        ),
        CapabilityDescriptor(
            id = "timer",
            name = "Timers",
            category = CapabilityCategory.TIME,
            description = "Start, name and cancel multiple countdown timers with notifications.",
            voiceCommands = listOf("২০ মিনিটের timer দাও"),
            probeKey = "alwaysAvailable"
        ),
        CapabilityDescriptor(
            id = "alarm",
            name = "Alarms",
            category = CapabilityCategory.TIME,
            description = "Creates real alarms via the system AlarmClock API (opens the clock app to confirm).",
            voiceCommands = listOf("সকাল ৭টায় alarm দাও"),
            probeKey = "alwaysAvailable"
        ),
        CapabilityDescriptor(
            id = "reminder",
            name = "Reminders",
            category = CapabilityCategory.TIME,
            description = "Time-based reminders scheduled locally and surfaced as notifications.",
            voiceCommands = listOf("মনে করিয়ে দাও…"),
            probeKey = "alwaysAvailable"
        ),
        CapabilityDescriptor(
            id = "calendar",
            name = "Calendar",
            category = CapabilityCategory.TIME,
            description = "Creates/reads events through the system calendar provider.",
            voiceCommands = listOf("আজকের schedule বলো"),
            probeKey = "calendar"
        ),
        CapabilityDescriptor(
            id = "web_search",
            name = "Web Search",
            category = CapabilityCategory.WEB,
            description = "Launches a web search in the browser; cloud brain summarizes results.",
            voiceCommands = listOf("আবহাওয়া খুঁজে দাও"),
            requiredPermissions = listOf("android.permission.INTERNET"),
            probeKey = "web"
        ),
        CapabilityDescriptor(
            id = "vision",
            name = "Vision AI",
            category = CapabilityCategory.VISION,
            description = "Screenshot/camera image understanding and OCR via the AI provider.",
            voiceCommands = listOf("এই screenshot-এ কী সমস্যা?"),
            requiredPermissions = listOf("android.permission.INTERNET"),
            probeKey = "vision"
        ),
        CapabilityDescriptor(
            id = "camera",
            name = "Camera",
            category = CapabilityCategory.VISION,
            description = "Launches the camera and captures images for analysis.",
            requiredPermissions = listOf("android.permission.CAMERA"),
            probeKey = "camera"
        ),
        CapabilityDescriptor(
            id = "location",
            name = "Location & Maps",
            category = CapabilityCategory.LOCATION,
            description = "Current location, nearby places and navigation (Google Maps intents).",
            voiceCommands = listOf("বাসায় নেভিগেট করো"),
            requiredPermissions = listOf("android.permission.ACCESS_FINE_LOCATION"),
            probeKey = "location"
        ),
        CapabilityDescriptor(
            id = "media_control",
            name = "Media Control",
            category = CapabilityCategory.MEDIA,
            description = "Play/pause/next via media session and volume transport keys.",
            voiceCommands = listOf("গান বাজাও", "pause"),
            probeKey = "media"
        ),
        CapabilityDescriptor(
            id = "routines",
            name = "Routines & Custom Commands",
            category = CapabilityCategory.MODES,
            description = "Multi-step custom commands and modes (Study, Sleep, Driving…).",
            voiceCommands = listOf("Study Mode চালাও", "saved routine দেখাও"),
            probeKey = "alwaysAvailable"
        ),
        CapabilityDescriptor(
            id = "foreground_service",
            name = "Background Service",
            category = CapabilityCategory.SYSTEM,
            description = "Keeps Arohi reachable via a foreground service with accurate RUNNING/STOPPED state.",
            requiredPermissions = listOf("android.permission.POST_NOTIFICATIONS"),
            probeKey = "foregroundService"
        ),
        CapabilityDescriptor(
            id = "security_center",
            name = "Security & Privacy Center",
            category = CapabilityCategory.SECURITY,
            description = "Real status of permissions, services, keys and privacy controls.",
            probeKey = "alwaysAvailable"
        )
    )

    fun byId(id: String): CapabilityDescriptor? = all.firstOrNull { it.id == id }

    fun byCategory(category: CapabilityCategory): List<CapabilityDescriptor> =
        all.filter { it.category == category }
}
