package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

enum class GeminiConnectionState {
    CONNECTED,
    DISCONNECTED,
    INVALID_KEY,
    NETWORK_ERROR,
    RATE_LIMITED,
    MODEL_UNAVAILABLE,
    CHECKING
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

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
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Real connectivity test. `retries` is the user-configurable retry count
     * from the Gemini Control Center; each attempt is a genuine API ping.
     */
    suspend fun testConnection(
        apiKey: String,
        model: String = "gemini-3.5-flash",
        retries: Int = 0
    ): Pair<GeminiConnectionState, String> {
        if (apiKey.isBlank()) {
            return Pair(GeminiConnectionState.DISCONNECTED, "API Key is empty")
        }
        var lastState = GeminiConnectionState.DISCONNECTED
        var lastMessage = "API Key is empty"
        var attempt = 0
        do {
            attempt++
            val (state, message) = pingOnce(apiKey, model)
            lastState = state
            lastMessage = message
        } while (state != GeminiConnectionState.CONNECTED &&
            state != GeminiConnectionState.INVALID_KEY &&
            state != GeminiConnectionState.MODEL_UNAVAILABLE &&
            attempt <= retries)
        return Pair(lastState, lastMessage)
    }

    private suspend fun pingOnce(apiKey: String, model: String): Pair<GeminiConnectionState, String> {
        return try {
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = "Hello, are you ready?"))
                    )
                ),
                generationConfig = GenerationConfig(maxOutputTokens = 10)
            )
            val response = service.generateContent(model = model, apiKey = apiKey, request = request)
            if (response.isSuccessful) {
                Pair(GeminiConnectionState.CONNECTED, "Gemini Connected ($model)")
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                when {
                    code == 400 || code == 403 || errorBody.contains("API_KEY_INVALID", ignoreCase = true) ->
                        Pair(GeminiConnectionState.INVALID_KEY, "Invalid or unauthorized API key")
                    code == 429 ->
                        Pair(GeminiConnectionState.RATE_LIMITED, "Rate limit exceeded. Please wait.")
                    code == 404 ->
                        Pair(GeminiConnectionState.MODEL_UNAVAILABLE, "Model $model not found")
                    else ->
                        Pair(GeminiConnectionState.NETWORK_ERROR, "Server responded with error code $code")
                }
            }
        } catch (e: Exception) {
            Pair(GeminiConnectionState.NETWORK_ERROR, e.localizedMessage ?: "Network connection failure")
        }
    }

    companion object {
        /** Models selectable in the Gemini Control Center. */
        val SELECTABLE_MODELS = listOf(
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        )
    }
}
