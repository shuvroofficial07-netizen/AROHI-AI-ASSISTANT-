package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Optional Anthropic (Claude) transport.
 *
 * AROHI is provider-agnostic: the personality, the Bengali-first system prompt and the tool
 * contract all live in [com.example.engine.PersonaEngine] / [com.example.engine.ToolRegistry],
 * so the very same AROHI persona can be served either by Gemini or by Claude. Users who have an
 * Anthropic key simply switch the brain provider in Settings — still 100% free to choose.
 */
@JsonClass(generateAdapter = true)
data class ClaudeMessageRequest(
    @Json(name = "model") val model: String,
    @Json(name = "max_tokens") val maxTokens: Int = 1024,
    @Json(name = "system") val system: String? = null,
    @Json(name = "messages") val messages: List<ClaudeMessage>,
    @Json(name = "temperature") val temperature: Float? = 0.8f,
    @Json(name = "tools") val tools: List<ClaudeTool>? = null
)

@JsonClass(generateAdapter = true)
data class ClaudeMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ClaudeTool(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "input_schema") val inputSchema: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class ClaudeContentBlock(
    @Json(name = "type") val type: String? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "input") val input: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class ClaudeError(
    @Json(name = "type") val type: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ClaudeMessageResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "content") val content: List<ClaudeContentBlock>? = null,
    @Json(name = "stop_reason") val stopReason: String? = null,
    @Json(name = "error") val error: ClaudeError? = null
)

interface ClaudeApiService {
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String,
        @Header("content-type") contentType: String,
        @Body request: ClaudeMessageRequest
    ): Response<ClaudeMessageResponse>
}

object ClaudeClient {
    private const val BASE_URL = "https://api.anthropic.com/"
    const val DEFAULT_MODEL = "claude-sonnet-4-5"
    private const val API_VERSION = "2023-06-01"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val service: ClaudeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ClaudeApiService::class.java)
    }

    suspend fun sendMessage(
        apiKey: String,
        model: String,
        system: String,
        messages: List<ClaudeMessage>,
        tools: List<ClaudeTool>? = null,
        maxTokens: Int = 1024
    ): Response<ClaudeMessageResponse> = service.createMessage(
        apiKey = apiKey,
        version = API_VERSION,
        contentType = "application/json",
        request = ClaudeMessageRequest(
            model = model.ifBlank { DEFAULT_MODEL },
            maxTokens = maxTokens,
            system = system,
            messages = messages,
            temperature = 0.8f,
            tools = tools?.takeIf { it.isNotEmpty() }
        )
    )

    suspend fun testConnection(
        apiKey: String,
        model: String = DEFAULT_MODEL
    ): Pair<GeminiConnectionState, String> {
        if (apiKey.isBlank()) {
            return Pair(GeminiConnectionState.DISCONNECTED, "Anthropic API Key is empty")
        }
        return try {
            val response = sendMessage(
                apiKey = apiKey,
                model = model,
                system = "You are a connection probe. Reply with a single short word.",
                messages = listOf(ClaudeMessage(role = "user", content = "Ping")),
                maxTokens = 16
            )
            if (response.isSuccessful) {
                Pair(GeminiConnectionState.CONNECTED, "Claude Connected (${model.ifBlank { DEFAULT_MODEL }})")
            } else {
                val code = response.code()
                val body = response.errorBody()?.string() ?: ""
                when {
                    code == 401 || code == 403 || body.contains("authentication", true) ->
                        Pair(GeminiConnectionState.INVALID_KEY, "Invalid or unauthorized Anthropic key")
                    code == 429 -> Pair(GeminiConnectionState.RATE_LIMITED, "Rate limit exceeded. Please wait.")
                    code == 404 -> Pair(GeminiConnectionState.MODEL_UNAVAILABLE, "Model $model not found")
                    else -> Pair(GeminiConnectionState.NETWORK_ERROR, "Anthropic responded with error code $code")
                }
            }
        } catch (e: Exception) {
            Pair(GeminiConnectionState.NETWORK_ERROR, e.localizedMessage ?: "Network connection failure")
        }
    }
}
