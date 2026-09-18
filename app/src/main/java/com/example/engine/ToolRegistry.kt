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
            ),
            FunctionDeclaration(
                name = "create_reminder",
                description = "Creates a REAL device reminder/alarm that fires even when the app is closed. " +
                    "Pass the natural time phrase in Bengali or English.",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "title" to PropertySchema(type = "STRING", description = "What to remind the user about"),
                        "time_text" to PropertySchema(type = "STRING", description = "Bengali/English time phrase, e.g. 'আগামীকাল সকাল ৭টায়' or '10 minutes later' or '8:30 pm'"),
                        "repeat" to PropertySchema(type = "STRING", description = "NONE, DAILY, WEEKLY, MONTHLY or WEEKDAYS")
                    ),
                    required = listOf("title", "time_text")
                )
            ),
            FunctionDeclaration(
                name = "list_reminders",
                description = "Lists the user's pending reminders with their real times",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = emptyMap(),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "delete_reminder",
                description = "Deletes a pending reminder whose title matches the query",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "query" to PropertySchema(type = "STRING", description = "Title or keyword of the reminder. Use '*' to delete everything.")
                    ),
                    required = listOf("query")
                )
            ),
            FunctionDeclaration(
                name = "add_todo",
                description = "Adds an item to the user's real to-do list (Room database)",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "title" to PropertySchema(type = "STRING", description = "The task"),
                        "due_text" to PropertySchema(type = "STRING", description = "Optional due time phrase"),
                        "priority" to PropertySchema(type = "INTEGER", description = "0 = low, 1 = normal, 2 = urgent")
                    ),
                    required = listOf("title")
                )
            ),
            FunctionDeclaration(
                name = "list_todos",
                description = "Lists the user's pending to-do items",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = emptyMap(),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "complete_todo",
                description = "Marks a to-do item as done by matching its title",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "query" to PropertySchema(type = "STRING", description = "Title or keyword of the completed task")
                    ),
                    required = listOf("query")
                )
            ),
            FunctionDeclaration(
                name = "add_note",
                description = "Saves a note into the user's real notes database",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "title" to PropertySchema(type = "STRING", description = "Short note title"),
                        "content" to PropertySchema(type = "STRING", description = "Note body")
                    ),
                    required = listOf("title", "content")
                )
            ),
            FunctionDeclaration(
                name = "list_notes",
                description = "Lists the saved notes",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = emptyMap(),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "get_weather",
                description = "Fetches REAL current weather (temperature, rain chance, humidity, wind) for the user's city",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "city" to PropertySchema(type = "STRING", description = "Optional city name; uses the configured city/location when omitted")
                    ),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "calculate",
                description = "Evaluates a maths expression or Bengali word calculation (যোগ, বিয়োগ, গুণ, ভাগ, শতাংশ)",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "expression" to PropertySchema(type = "STRING", description = "Expression or Bengali calculation sentence")
                    ),
                    required = listOf("expression")
                )
            ),
            FunctionDeclaration(
                name = "convert_units",
                description = "Converts between real units (length, mass, volume, data, speed, temperature)",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "text" to PropertySchema(type = "STRING", description = "Conversion sentence, e.g. '২০ কেজি কত পাউন্ড' or '30 celsius to fahrenheit'")
                    ),
                    required = listOf("text")
                )
            ),
            FunctionDeclaration(
                name = "convert_currency",
                description = "Converts money between currencies using live exchange rates",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "text" to PropertySchema(type = "STRING", description = "Conversion sentence, e.g. '১০০ ডলার কত টাকা'")
                    ),
                    required = listOf("text")
                )
            ),
            FunctionDeclaration(
                name = "translate_text",
                description = "Translates short text between Bengali and English",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "text" to PropertySchema(type = "STRING", description = "Text to translate"),
                        "target_language" to PropertySchema(type = "STRING", description = "Target language, e.g. 'English' or 'Bengali'")
                    ),
                    required = listOf("text", "target_language")
                )
            ),
            FunctionDeclaration(
                name = "get_calendar_events",
                description = "Reads the user's real device calendar for the coming days",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "days" to PropertySchema(type = "INTEGER", description = "How many days ahead to read (default 7)")
                    ),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "create_calendar_event",
                description = "Creates a real calendar event on the device calendar (or opens the calendar insert screen when permission is missing)",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = mapOf(
                        "title" to PropertySchema(type = "STRING", description = "Event title"),
                        "start_text" to PropertySchema(type = "STRING", description = "Bengali/English start time phrase"),
                        "duration_minutes" to PropertySchema(type = "INTEGER", description = "Duration in minutes (default 60)")
                    ),
                    required = listOf("title", "start_text")
                )
            ),
            FunctionDeclaration(
                name = "get_proactive_suggestion",
                description = "Returns a context aware suggestion based on real weather, reminders, to-dos and battery state",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = emptyMap(),
                    required = emptyList()
                )
            ),
            FunctionDeclaration(
                name = "export_user_data",
                description = "Exports all of the user's local AROHI data into a JSON file the user can share",
                parameters = FunctionParameters(
                    type = "OBJECT",
                    properties = emptyMap(),
                    required = emptyList()
                )
            )
        )
    )
}
