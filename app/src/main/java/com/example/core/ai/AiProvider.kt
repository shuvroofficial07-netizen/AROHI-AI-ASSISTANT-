package com.example.core.ai

import com.example.core.result.ArohiErrorCode
import com.example.core.result.ArohiResult

/** A single chat turn exchanged with an AI provider. */
data class AiMessage(val role: Role, val content: String, val imageBase64: String? = null, val mimeType: String? = null) {
    enum class Role { SYSTEM, USER, MODEL }
}

/** A function/tool the model may invoke. */
data class AiToolCall(val name: String, val args: Map<String, Any?>)

/** The outcome of an AI generation request. */
sealed class AiResponse {
    data class Text(val text: String) : AiResponse()
    data class ToolCall(val call: AiToolCall) : AiResponse()
}

/** Static description of an available provider, for the API Center UI. */
data class AiProviderInfo(
    val id: String,
    val displayName: String,
    val requiresApiKey: Boolean,
    val supportsImage: Boolean,
    val models: List<String>
)

/**
 * Abstraction over a reasoning backend (spec §53). Implementations include a
 * cloud Gemini provider and an offline/fallback provider. The assistant only
 * ever talks to this interface, so providers can be swapped without touching
 * the brain. Implementations must NOT throw for expected failures — they return
 * a failed [ArohiResult] with a concrete [ArohiErrorCode].
 */
interface AiProvider {
    val info: AiProviderInfo

    /** True when the provider is configured (e.g. key present) and could be used. */
    fun isConfigured(): Boolean

    /**
     * Generates a response. [tools] are optional function declarations the model
     * may call. Must perform network work off the main thread.
     */
    suspend fun generate(
        messages: List<AiMessage>,
        tools: List<AiToolDeclaration> = emptyList(),
        temperature: Float = 0.7f,
        maxOutputTokens: Int = 1024
    ): ArohiResult<AiResponse>

    /** Lightweight connectivity/key validation used by the "Test Connection" button. */
    suspend fun testConnection(): ArohiResult<Unit>
}

/** Provider-agnostic tool declaration. */
data class AiToolDeclaration(
    val name: String,
    val description: String,
    val properties: Map<String, String> = emptyMap(),
    val required: List<String> = emptyList()
) {
    companion object {
        fun of(name: String, description: String): AiToolDeclaration =
            AiToolDeclaration(name, description)
    }
}

/** Maps low-level network/HTTP errors to canonical Arohi error codes. */
object AiErrorMapper {
    fun fromHttp(code: Int): ArohiErrorCode = when (code) {
        400, 401, 403 -> ArohiErrorCode.API_KEY_INVALID
        408, 504 -> ArohiErrorCode.API_TIMEOUT
        429 -> ArohiErrorCode.API_UNAVAILABLE
        in 500..599 -> ArohiErrorCode.API_UNAVAILABLE
        else -> ArohiErrorCode.API_UNAVAILABLE
    }
}
