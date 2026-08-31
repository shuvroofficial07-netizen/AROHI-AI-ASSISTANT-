package com.example.engine

import com.example.data.remote.FunctionDeclaration
import com.example.data.remote.FunctionParameters
import com.example.data.remote.PropertySchema
import com.example.data.remote.Tool

object ToolRegistry {

    val availableTools = Tool(
        functionDeclarations = listOf(
            FunctionDeclaration(
                name = "open_app",
                description = "Opens an installed Android application by its name or alias (e.g. YouTube, Facebook, Calculator, Settings, Camera, WhatsApp)",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "app_name" to PropertySchema(type = "STRING", description = "The name or alias of the app to launch")
                    ),
                    required = listOf("app_name")
                )
            ),
            FunctionDeclaration(
                name = "make_phone_call",
                description = "Makes a phone call or opens the dialer for a given contact name or phone number",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "target" to PropertySchema(type = "STRING", description = "The contact name or raw phone number")
                    ),
                    required = listOf("target")
                )
            ),
            FunctionDeclaration(
                name = "send_sms",
                description = "Prepares and sends an SMS to a specified contact or phone number",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "target" to PropertySchema(type = "STRING", description = "Contact name or phone number"),
                        "message" to PropertySchema(type = "STRING", description = "Message body content")
                    ),
                    required = listOf("target", "message")
                )
            ),
            FunctionDeclaration(
                name = "send_whatsapp",
                description = "Sends a message via WhatsApp to a phone number or contact",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "phone_number" to PropertySchema(type = "STRING", description = "Target phone number with country code if available"),
                        "message" to PropertySchema(type = "STRING", description = "Message to send")
                    ),
                    required = listOf("phone_number", "message")
                )
            ),
            FunctionDeclaration(
                name = "read_device_telemetry",
                description = "Reads real-time device stats: Battery %, Charging type, Available RAM, Free Storage, Wi-Fi/Network status, Volume levels, and Android version",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = emptyMap(),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "toggle_flashlight",
                description = "Turns the device flashlight/torch on or off",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "enabled" to PropertySchema(type = "BOOLEAN", description = "True to turn on flashlight, False to turn off")
                    ),
                    required = listOf("enabled")
                )
            ),
            FunctionDeclaration(
                name = "set_media_volume",
                description = "Sets the device media volume percentage (0 to 100)",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "percent" to PropertySchema(type = "INTEGER", description = "Target volume percentage from 0 to 100")
                    ),
                    required = listOf("percent")
                )
            ),
            FunctionDeclaration(
                name = "read_notifications",
                description = "Fetches recent captured unread notifications from WhatsApp, Messenger, SMS, and other apps",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "limit" to PropertySchema(type = "INTEGER", description = "Number of notifications to retrieve (default 5)")
                    ),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "open_settings",
                description = "Opens a real Android settings panel: wifi, bluetooth, display, sound, battery, notification, accessibility, apps, storage, device_info, location or security",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "panel" to PropertySchema(type = "STRING", description = "Settings panel key, e.g. 'wifi', 'bluetooth', 'display', 'sound', 'battery', 'notification', 'accessibility', 'apps', 'storage', 'device_info', 'location', 'security'")
                    ),
                    required = listOf("panel")
                )
            ),
            FunctionDeclaration(
                name = "open_url",
                description = "Opens a web URL in the browser",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "url" to PropertySchema(type = "STRING", description = "The full URL to open")
                    ),
                    required = listOf("url")
                )
            ),
            FunctionDeclaration(
                name = "media_control",
                description = "Controls media playback with real media key events: play, pause, next, previous, stop",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "action" to PropertySchema(type = "STRING", description = "One of: play, pause, next, previous, stop")
                    ),
                    required = listOf("action")
                )
            ),
            FunctionDeclaration(
                name = "get_current_app",
                description = "Reads the current foreground application label (requires Usage Access permission; reports honestly when unavailable)",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = emptyMap(),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "check_call_state",
                description = "Reads the real current phone call state (ringing / offhook / idle) and caller identity from TelephonyManager",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = emptyMap(),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "inspect_screen",
                description = "Uses Accessibility Service to read the UI hierarchy and visible texts on the current device screen",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = emptyMap(),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "click_screen_element",
                description = "Clicks a button or text on screen via Accessibility Service",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "query" to PropertySchema(type = "STRING", description = "Text or element description to click")
                    ),
                    required = listOf("query")
                )
            ),
            FunctionDeclaration(
                name = "save_user_memory",
                description = "Saves a persistent user memory, preference, or fact into Room database",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "category" to PropertySchema(type = "STRING", description = "Category: PROFILE, PREFERENCES, APP_ALIASES, IMPORTANT_FACTS"),
                        "key" to PropertySchema(type = "STRING", description = "Short key or identifier"),
                        "value" to PropertySchema(type = "STRING", description = "Content/Fact to remember")
                    ),
                    required = listOf("category", "key", "value")
                )
            ),
            FunctionDeclaration(
                name = "search_memory",
                description = "Searches the user long-term memory database for saved facts or preferences",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "query" to PropertySchema(type = "STRING", description = "Search query keyword")
                    ),
                    required = listOf("query")
                )
            ),
            FunctionDeclaration(
                name = "diagnostics_check",
                description = "Performs real-time diagnostics of all assistant subsystems: Permissions, Services, Database, Audio, and AI link",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = emptyMap(),
                    required = emptyList()
                )
            )
        )
    )
}
