package com.example.data.remote

import kotlinx.coroutines.delay
import retrofit2.Response

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
    private const val MAX_ATTEMPTS = 2
    private const val RETRY_DELAY_MS = 1200L

    private val moshi: com.squareup.moshi.Moshi by lazy {
        com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: okhttp3.OkHttpClient by lazy {
        // BASIC level logs only the request line + status line — the API key is
        // sent in the x-goog-api-key header, so it can never appear in logs.
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
            redactHeader("x-goog-api-key")
        }
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: retrofit2.Retrofit by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
            .build()
    }

    val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Performs a REAL generateContent request with timeout, retry-with-backoff on
     * transient failures and a structured error result. Never throws for network
     * problems; only rethrows if the caller's coroutine is cancelled.
     */
    suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GenerateContentRequest
    ): GeminiResult {
        var lastError: ApiError? = null

        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val response: Response<GenerateContentResponse> =
                    service.generateContent(model = model, apiKey = apiKey, request = request)

                if (response.isSuccessful) {
                    val body = response.body()
                    return if (body != null) {
                        GeminiResult.Success(body)
                    } else {
                        GeminiResult.Failure(ApiError.invalidResponse)
                    }
                }

                val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                val error = GeminiErrorMapper.fromHttp(response.code(), errorBody)
                if (!error.isRetryable) {
                    return GeminiResult.Failure(error)
                }
                lastError = error
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = GeminiErrorMapper.fromException(e)
                if (!error.isRetryable) {
                    return GeminiResult.Failure(error)
                }
                lastError = error
            }

            if (attempt < MAX_ATTEMPTS - 1) {
                delay(RETRY_DELAY_MS)
            }
        }

        return GeminiResult.Failure(lastError ?: ApiError.invalidResponse)
    }

    /**
     * Tests the connection with a REAL minimal API request and classifies the
     * actual outcome. An invalid key is never reported as valid.
     */
    suspend fun testConnection(
        apiKey: String,
        model: String = "gemini-3.5-flash"
    ): Pair<GeminiConnectionState, String> {
        if (apiKey.isBlank()) {
            return Pair(GeminiConnectionState.DISCONNECTED, "API Key is empty")
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "Hello, are you ready?")))
            ),
            generationConfig = GenerationConfig(maxOutputTokens = 10)
        )

        return when (val result = generateContent(model, apiKey, request)) {
            is GeminiResult.Success -> {
                // The request succeeded — but also verify a real candidate came back,
                // otherwise the key/model pair is not genuinely usable.
                val hasContent = result.body.candidates?.isNotEmpty() == true ||
                    result.body.error == null
                if (hasContent) {
                    Pair(GeminiConnectionState.CONNECTED, "Gemini Connected ($model)")
                } else {
                    Pair(GeminiConnectionState.MODEL_UNAVAILABLE, "Model $model returned no content")
                }
            }
            is GeminiResult.Failure -> {
                Pair(GeminiErrorMapper.toConnectionState(result.error), result.error.userMessage)
            }
        }
    }
}
