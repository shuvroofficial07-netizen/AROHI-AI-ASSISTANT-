package com.example.core.result

/**
 * Universal operation result for every Arohi subsystem.
 *
 * The spec is strict: an action must NEVER report success unless it actually
 * succeeded (and, where possible, was verified). Every real action returns one
 * of the [StatusCode] outcomes instead of throwing or faking a result.
 */
enum class StatusCode {
    /** The action executed AND was verified to have taken effect. */
    SUCCESS,

    /** The capability cannot run on this device / OS level / configuration. */
    UNSUPPORTED,

    /** A runtime permission or special access must be granted first. */
    REQUIRES_PERMISSION,

    /** A sensitive action that needs explicit user confirmation before running. */
    REQUIRES_CONFIRMATION,

    /** The action was attempted but genuinely failed. */
    FAILED
}

/**
 * Canonical, machine-readable error codes (spec §62). Every failure path in
 * Arohi maps to one of these so the UI can show a human explanation plus a
 * concrete recovery action.
 */
enum class ArohiErrorCode(val code: String, val humanMessage: String, val recoveryAction: String) {
    API_KEY_INVALID("API_KEY_INVALID", "The AI API key is missing or invalid.", "Open API Center and add a valid key, then run Test Connection."),
    API_TIMEOUT("API_TIMEOUT", "The AI provider took too long to respond.", "Check your internet connection and try again."),
    API_UNAVAILABLE("API_UNAVAILABLE", "No configured AI provider is reachable.", "Check network / provider settings; offline commands still work."),
    NETWORK_UNAVAILABLE("NETWORK_UNAVAILABLE", "Internet connection required for this action.", "Connect to Wi-Fi or mobile data and retry."),
    MIC_PERMISSION_DENIED("MIC_PERMISSION_DENIED", "Microphone permission is not granted.", "Grant Microphone permission in Setup / Settings."),
    MIC_UNAVAILABLE("MIC_UNAVAILABLE", "No microphone is available on this device.", "This device cannot perform voice input."),
    SPEECH_RECOGNIZER_ERROR("SPEECH_RECOGNIZER_ERROR", "Speech recognition failed or is unavailable.", "Install/enable a speech recognizer (e.g. Google app) and retry."),
    TTS_UNAVAILABLE("TTS_UNAVAILABLE", "Text-to-speech engine is unavailable.", "Install a TTS engine in Settings and retry."),
    ACCESSIBILITY_DISABLED("ACCESSIBILITY_DISABLED", "Accessibility service is not enabled.", "Enable Arohi Accessibility in system settings."),
    NOTIFICATION_ACCESS_DISABLED("NOTIFICATION_ACCESS_DISABLED", "Notification access is not granted.", "Grant Notification access to Arohi in system settings."),
    SERVICE_NOT_RUNNING("SERVICE_NOT_RUNNING", "The Arohi background service is not running.", "Start the service from the home screen / Setup."),
    DATABASE_ERROR("DATABASE_ERROR", "Local database operation failed.", "Restart Arohi; if it persists, clear app data from Settings."),
    AUTOMATION_FAILED("AUTOMATION_FAILED", "The on-screen automation could not be completed.", "Verify the target screen is visible and retry; open the app manually if needed."),
    PERMISSION_DENIED("PERMISSION_DENIED", "A required permission was denied.", "Grant the permission and retry."),
    FEATURE_UNSUPPORTED("FEATURE_UNSUPPORTED", "This feature is not supported on this device/OS.", "Use a supported device or newer Android version."),
    CONFIRMATION_REQUIRED("CONFIRMATION_REQUIRED", "This action needs your confirmation.", "Confirm to proceed, or cancel."),
    NO_MATCH("NO_MATCH", "Arohi could not understand the request.", "Rephrase the command or tap a suggested action."),
    TIMEOUT("TIMEOUT", "The operation timed out.", "Retry; the system uses bounded retries only."),
    UNKNOWN("UNKNOWN", "An unexpected error occurred.", "Try again; check Diagnostics if it repeats.");

    companion object {
        fun fromCode(code: String?): ArohiErrorCode? = values().firstOrNull { it.code == code }
    }
}

/**
 * The result of any Arohi operation. [data] carries a payload on success;
 * on failure [errorCode], [technicalCause] and [recoveryAction] explain what
 * happened and how to recover.
 */
data class ArohiResult<out T>(
    val status: StatusCode,
    val message: String,
    val data: T? = null,
    val errorCode: ArohiErrorCode? = null,
    val technicalCause: String? = null,
    val recoveryAction: String? = null,
    /** True only when the effect was actually confirmed (not just attempted). */
    val verified: Boolean = false
) {
    val succeeded: Boolean get() = status == StatusCode.SUCCESS
    val needsPermission: Boolean get() = status == StatusCode.REQUIRES_PERMISSION
    val needsConfirmation: Boolean get() = status == StatusCode.REQUIRES_CONFIRMATION

    companion object {
        fun <T> success(data: T? = null, message: String = "OK", verified: Boolean = true): ArohiResult<T> =
            ArohiResult(StatusCode.SUCCESS, message, data = data, verified = verified)

        fun <T> failed(
            errorCode: ArohiErrorCode,
            message: String? = null,
            technicalCause: String? = null,
            recoveryAction: String? = null
        ): ArohiResult<T> = ArohiResult(
            status = StatusCode.FAILED,
            message = message ?: errorCode.humanMessage,
            errorCode = errorCode,
            technicalCause = technicalCause,
            recoveryAction = recoveryAction ?: errorCode.recoveryAction
        )

        fun <T> requiresPermission(
            errorCode: ArohiErrorCode = ArohiErrorCode.PERMISSION_DENIED,
            message: String? = null,
            recoveryAction: String? = null
        ): ArohiResult<T> = ArohiResult(
            status = StatusCode.REQUIRES_PERMISSION,
            message = message ?: errorCode.humanMessage,
            errorCode = errorCode,
            recoveryAction = recoveryAction ?: errorCode.recoveryAction
        )

        fun <T> requiresConfirmation(message: String, data: T? = null): ArohiResult<T> =
            ArohiResult(
                status = StatusCode.REQUIRES_CONFIRMATION,
                message = message,
                data = data,
                errorCode = ArohiErrorCode.CONFIRMATION_REQUIRED
            )

        fun <T> unsupported(
            message: String,
            errorCode: ArohiErrorCode = ArohiErrorCode.FEATURE_UNSUPPORTED
        ): ArohiResult<T> = ArohiResult(
            status = StatusCode.UNSUPPORTED,
            message = message,
            errorCode = errorCode,
            recoveryAction = errorCode.recoveryAction
        )
    }
}
