package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "tools") val tools: List<Tool>? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null,
    @Json(name = "functionCall") val functionCall: FunctionCall? = null,
    @Json(name = "functionResponse") val functionResponse: FunctionResponse? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class FunctionCall(
    @Json(name = "name") val name: String,
    @Json(name = "args") val args: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class FunctionResponse(
    @Json(name = "name") val name: String,
    @Json(name = "response") val response: Map<String, Any?>
)

@JsonClass(generateAdapter = true)
data class Tool(
    @Json(name = "functionDeclarations") val functionDeclarations: List<FunctionDeclaration>? = null
)

@JsonClass(generateAdapter = true)
data class FunctionDeclaration(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "parameters") val parameters: FunctionParameters? = null
)

@JsonClass(generateAdapter = true)
data class FunctionParameters(
    @Json(name = "type") val type: String = "OBJECT",
    @Json(name = "properties") val properties: Map<String, PropertySchema>? = null,
    @Json(name = "required") val required: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class PropertySchema(
    @Json(name = "type") val type: String,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.7f,
    @Json(name = "topP") val topP: Float? = 0.95f,
    @Json(name = "topK") val topK: Int? = 40,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 1024
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null,
    @Json(name = "error") val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)

/** Gemini error responses are wrapped in an outer `{"error": {...}}` envelope. */
@JsonClass(generateAdapter = true)
data class GeminiErrorEnvelope(
    @Json(name = "error") val error: GeminiError? = null
)
