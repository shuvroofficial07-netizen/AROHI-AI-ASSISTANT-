package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
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

    /** Default timeout used when the user has not configured one. */
    const val DEFAULT_TIMEOUT_SECONDS = 45
    const val DEFAULT_MAX_RETRIES = 2

    /**
     * Models offered in the Gemini configuration screen. These are the model ids accepted by the
     * v1beta generateContent endpoint; the connection test verifies the selected one really works
     * with the user's key instead of assuming it.
     */
    val SELECTABLE_MODELS = listOf(
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.0-flash",
        "gemini-1.5-flash",
        "gemini-1.5-pro"
    )

    const val DEFAULT_MODEL = "gemini-2.5-flash"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Strips the `key` query parameter from anything that gets logged.
     * The API key must never reach logcat, crash files, or bug reports.
     */
    private class ApiKeyRedactingLogger : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            val safe = message.replace(Regex("key=[^&\\s]+"), "key=***REDACTED***")
            HttpLoggingInterceptor.Logger.DEFAULT.log(safe)
        }
    }

    /** Retries only transient failures (IO errors, 429, 5xx) with exponential backoff. */
    private class RetryInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val maxAttempts = (maxRetries + 1).coerceIn(1, 4)
            var lastError: IOException? = null

            for (attempt in 0 until maxAttempts) {
                if (attempt > 0) {
                    // 500ms, 1s, 2s — bounded, never an infinite retry loop.
                    val backoffMs = 500L shl (attempt - 1)
                    try {
                        Thread.sleep(backoffMs)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
                try {
                    val response = chain.proceed(request)
                    val retryable = response.code == 429 || response.code in 500..599
                    if (!retryable || attempt == maxAttempts - 1) return response
                    response.close()
                } catch (e: IOException) {
                    lastError = e
                    if (attempt == maxAttempts - 1) throw e
                }
            }
            throw lastError ?: IOException("Gemini request failed after $maxAttempts attempts")
        }
    }

    @Volatile
    private var timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS

    @Volatile
    private var maxRetries: Int = DEFAULT_MAX_RETRIES

    @Volatile
    private var cachedClient: OkHttpClient? = null

    @Volatile
    private var cachedRetrofit: Retrofit? = null

    /** Applies the user's real network preferences from the Gemini configuration screen. */
    @Synchronized
    fun configure(timeoutSeconds: Int, maxRetries: Int) {
        val newTimeout = timeoutSeconds.coerceIn(5, 180)
        val newRetries = maxRetries.coerceIn(0, 3)
        if (newTimeout == this.timeoutSeconds && newRetries == this.maxRetries && cachedRetrofit != null) return
        this.timeoutSeconds = newTimeout
        this.maxRetries = newRetries
        cachedClient = null
        cachedRetrofit = null
    }

    @Synchronized
    private fun retrofit(): Retrofit {
        cachedRetrofit?.let { return it }

        val logging = HttpLoggingInterceptor(ApiKeyRedactingLogger()).apply {
            // Never log request bodies or headers in release builds.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .addInterceptor(RetryInterceptor())
            .addInterceptor(logging)
            .build()

        val built = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        cachedClient = client
        cachedRetrofit = built
        return built
    }

    val service: GeminiApiService
        get() = retrofit().create(GeminiApiService::class.java)

    /**
     * Real connection test: performs a genuine minimal generateContent request with the given
     * key and model and reports exactly what the Gemini endpoint answered.
     */
    suspend fun testConnection(
        apiKey: String,
        model: String = DEFAULT_MODEL
    ): Pair<GeminiConnectionState, String> {
        if (apiKey.isBlank()) {
            return Pair(GeminiConnectionState.DISCONNECTED, "Gemini API is not configured.")
        }
        return try {
            val request = GenerateContentRequest(
                contents = listOf(Content(role = "user", parts = listOf(Part(text = "ping")))),
                generationConfig = GenerationConfig(maxOutputTokens = 8)
            )
            val response = service.generateContent(model = model, apiKey = apiKey, request = request)
            if (response.isSuccessful) {
                Pair(GeminiConnectionState.CONNECTED, "Gemini connected ($model)")
            } else {
                val code = response.code()
                val errorBody = try {
                    response.errorBody()?.string().orEmpty()
                } catch (e: Exception) {
                    ""
                }
                when {
                    errorBody.contains("API_KEY_INVALID", ignoreCase = true) || code == 401 || code == 403 ->
                        Pair(GeminiConnectionState.INVALID_KEY, "Invalid or unauthorized API key (HTTP $code)")
                    code == 429 ->
                        Pair(GeminiConnectionState.RATE_LIMITED, "Rate limit / quota exceeded (HTTP 429)")
                    code == 404 ->
                        Pair(GeminiConnectionState.MODEL_UNAVAILABLE, "Model '$model' is not available for this key (HTTP 404)")
                    code == 400 ->
                        Pair(GeminiConnectionState.INVALID_KEY, "Request rejected (HTTP 400). Check the API key and model.")
                    else ->
                        Pair(GeminiConnectionState.NETWORK_ERROR, "Gemini responded with HTTP $code")
                }
            }
        } catch (e: Exception) {
            Pair(
                GeminiConnectionState.NETWORK_ERROR,
                e.localizedMessage?.let { redact(it) } ?: "Network connection failure"
            )
        }
    }

    /** Removes any accidental key material from a message shown in the UI. */
    private fun redact(message: String): String =
        message.replace(Regex("key=[^&\\s]+"), "key=***REDACTED***")
}
