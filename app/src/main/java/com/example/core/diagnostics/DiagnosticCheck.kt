package com.example.core.diagnostics

/** The four real diagnostic outcomes required by spec §58. */
enum class DiagnosticStatus { PASS, WARNING, FAILED, NOT_AVAILABLE }

/** Identifies the subsystem a check belongs to (spec §58). */
enum class DiagnosticComponent(
    val id: String,
    val displayName: String,
    /** True when failure here means the assistant cannot deliver a core function. */
    val critical: Boolean
) {
    CORE("core", "Core Engine", true),
    DATABASE("database", "Database", true),
    MEMORY("memory", "Memory Store", true),
    AI("ai", "AI Provider", false),
    NETWORK("network", "Network", false),
    MICROPHONE("microphone", "Microphone", false),
    SPEECH("speech", "Speech Recognition", false),
    TTS("tts", "Text-To-Speech", false),
    AUDIO("audio", "Audio Output", false),
    ACCESSIBILITY("accessibility", "Accessibility Service", false),
    NOTIFICATION("notification", "Notification Listener", false),
    FOREGROUND_SERVICE("foreground_service", "Foreground Service", false),
    PERMISSIONS("permissions", "Permissions", false),
    AUTOMATION("automation", "Automation Engine", false),
    STORAGE("storage", "Storage", true)
}

/** The real result of probing one subsystem. */
data class DiagnosticCheckResult(
    val component: DiagnosticComponent,
    val status: DiagnosticStatus,
    /** Human-readable, factual detail (no fake values). */
    val detail: String,
    /** Optional recovery action shown when status != PASS. */
    val recoveryAction: String? = null,
    /** Measured value where applicable (latency ms, percent, count…). */
    val metric: String? = null
)

/** Aggregate report from a full system self-test (spec §60). */
data class DiagnosticReport(
    val results: List<DiagnosticCheckResult>,
    val generatedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0
) {
    val pass: List<DiagnosticCheckResult> get() = results.filter { it.status == DiagnosticStatus.PASS }
    val warnings: List<DiagnosticCheckResult> get() = results.filter { it.status == DiagnosticStatus.WARNING }
    val failed: List<DiagnosticCheckResult> get() = results.filter { it.status == DiagnosticStatus.FAILED }
    val notAvailable: List<DiagnosticCheckResult> get() = results.filter { it.status == DiagnosticStatus.NOT_AVAILABLE }

    /** Overall health: FAILED if any critical component failed, else WARNING if any issues, else PASS. */
    val overall: DiagnosticStatus
        get() = when {
            failed.any { it.component.critical } || failed.isNotEmpty() && failed.any { it.component.critical } -> DiagnosticStatus.FAILED
            failed.isNotEmpty() || warnings.isNotEmpty() -> DiagnosticStatus.WARNING
            else -> DiagnosticStatus.PASS
        }

    fun summary(): String = buildString {
        append("PASS ${pass.size}  •  WARNING ${warnings.size}  •  FAILED ${failed.size}  •  NOT AVAILABLE ${notAvailable.size}")
    }

    /** Renders a plain-text report suitable for voice readout. */
    fun renderText(): String = buildString {
        appendLine("Arohi system self-test — ${summary()}")
        results.forEach { r ->
            appendLine("• ${r.component.displayName}: ${r.status.name} — ${r.detail}")
            r.recoveryAction?.let { appendLine("   → $it") }
        }
    }
}
