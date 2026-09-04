package com.example.data.remote

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Structured error model for every network / API failure that Arohi can hit.
 * Each error carries a human-readable (Bengali + English) explanation that is
 * safe to show to the user — derived from the REAL failure, never fabricated.
 */
enum class ApiErrorType {
    NETWORK_UNAVAILABLE,
    DNS_FAILURE,
    TIMEOUT,
    TLS_FAILURE,
    HTTP_400,
    HTTP_401,
    HTTP_403,
    HTTP_404,
    HTTP_429,
    HTTP_500,
    SERVER_UNAVAILABLE,
    INVALID_RESPONSE,
    INVALID_API_KEY,
    MODEL_UNAVAILABLE,
    UNKNOWN_ERROR
}

data class ApiError(
    val type: ApiErrorType,
    val userMessage: String,
    val technicalDetail: String,
    val httpCode: Int? = null
) {
    val isRetryable: Boolean
        get() = type in RETRYABLE_TYPES

    companion object {
        val RETRYABLE_TYPES = setOf(
            ApiErrorType.NETWORK_UNAVAILABLE,
            ApiErrorType.DNS_FAILURE,
            ApiErrorType.TIMEOUT,
            ApiErrorType.HTTP_500,
            ApiErrorType.SERVER_UNAVAILABLE,
            ApiErrorType.UNKNOWN_ERROR
        )
    }
}

/**
 * Maps real exceptions / HTTP responses into a structured [ApiError].
 * Pure logic — unit-testable on the JVM without Android.
 */
object GeminiErrorMapper {

    fun fromException(e: Throwable): ApiError {
        val detail = e.javaClass.simpleName + (e.message?.let { ": $it" } ?: "")
        return when (e) {
            is UnknownHostException -> ApiError(
                type = ApiErrorType.DNS_FAILURE,
                userMessage = "নেটওয়ার্কে ডোমেইন নাম (DNS) মেলানো যাচ্ছে না। ইন্টারনেট সংযোগ বা DNS সেটিংস পরীক্ষা করুন। (DNS resolution failed)",
                technicalDetail = detail
            )
            is SocketTimeoutException -> ApiError(
                type = ApiErrorType.TIMEOUT,
                userMessage = "সার্ভার থেকে সাড়া পেতে বেশি সময় লাগছে (timeout)। নেটওয়ার্ক ধীর থাকতে পারে — কিছুক্ষণ পর আবার চেষ্টা করুন। (Connection timed out)",
                technicalDetail = detail
            )
            is SSLException -> ApiError(
                type = ApiErrorType.TLS_FAILURE,
                userMessage = "সুরক্ষিত (TLS/SSL) সংযোগ স্থাপন করা যায়নি। ভুল সময়/তারিখ বা নেটওয়ার্ক হস্তক্ষেপ এর কারণ হতে পারে। (TLS handshake failed)",
                technicalDetail = detail
            )
            is ConnectException -> ApiError(
                type = ApiErrorType.NETWORK_UNAVAILABLE,
                userMessage = "সার্ভারের সাথে সংযোগ স্থাপন করা যায়নি। ইন্টারনেট সক্রিয় আছে কিনা দেখুন। (Connection failed)",
                technicalDetail = detail
            )
            else -> ApiError(
                type = ApiErrorType.NETWORK_UNAVAILABLE,
                userMessage = "নেটওয়ার্ক সমস্যার কারণে অনুরোধ সম্পন্ন হয়নি। ইন্টারনেট সংযোগ পরীক্ষা করুন। (Network error: ${e.javaClass.simpleName})",
                technicalDetail = detail
            )
        }
    }

    fun fromHttp(code: Int, errorBody: String?): ApiError {
        val parsed = parseErrorBody(errorBody)
        val apiMessage = parsed?.message ?: ""
        val apiStatus = parsed?.status ?: ""

        return when {
            code == 400 && apiMessage.contains("API key", ignoreCase = true) -> invalidKey(code, apiMessage)
            code == 401 -> invalidKey(code, apiMessage)
            code == 403 -> ApiError(
                type = ApiErrorType.HTTP_403,
                userMessage = "অনুমতি নেই (403)। API key-তে এই মডেল/অপারেশনের অনুমতি নেই, অথবা key সঠিক নয়। (Permission denied)",
                technicalDetail = describe(code, apiStatus, apiMessage),
                httpCode = code
            )
            code == 404 || apiStatus.equals("NOT_FOUND", ignoreCase = true) -> ApiError(
                type = ApiErrorType.MODEL_UNAVAILABLE,
                userMessage = "মডেলটি খুঁজে পাওয়া যায়নি (404)। সেটিংসে সঠিক মডেলের নাম দিন — যেমন gemini-3.5-flash। (Model not found)",
                technicalDetail = describe(code, apiStatus, apiMessage),
                httpCode = code
            )
            code == 429 || apiStatus.equals("RESOURCE_EXHAUSTED", ignoreCase = true) -> ApiError(
                type = ApiErrorType.HTTP_429,
                userMessage = "API রেট লিমিট/কোটা শেষ হয়ে গেছে (429)। কিছুক্ষণ পর আবার চেষ্টা করুন। (Rate limit exceeded)",
                technicalDetail = describe(code, apiStatus, apiMessage),
                httpCode = code
            )
            code == 400 -> ApiError(
                type = ApiErrorType.HTTP_400,
                userMessage = "অনুরোধটি সার্ভার প্রত্যাখ্যান করেছে (400)। ইনপুট বা কনফিগারেশন পরীক্ষা করুন। (Bad request)",
                technicalDetail = describe(code, apiStatus, apiMessage),
                httpCode = code
            )
            code in 500..599 -> ApiError(
                type = if (code == 500) ApiErrorType.HTTP_500 else ApiErrorType.SERVER_UNAVAILABLE,
                userMessage = "Gemini সার্ভারে সমস্যা হয়েছে ($code)। এটি সাময়িক — কিছুক্ষণ পর আবার চেষ্টা করুন। (Server error)",
                technicalDetail = describe(code, apiStatus, apiMessage),
                httpCode = code
            )
            else -> ApiError(
                type = ApiErrorType.UNKNOWN_ERROR,
                userMessage = "সার্ভার থেকে অপ্রত্যাশিত সাড়া এসেছে (HTTP $code)। (Unexpected server response)",
                technicalDetail = describe(code, apiStatus, apiMessage),
                httpCode = code
            )
        }
    }

    fun invalidKey(code: Int, apiMessage: String): ApiError = ApiError(
        type = ApiErrorType.INVALID_API_KEY,
        userMessage = "API key সঠিক নয় বা অনুমোদিত নয়। সেটিংসে আসল Gemini API key দিন (aistudio.google.com/apikey)। (Invalid API key)",
        technicalDetail = describe(code, "INVALID_API_KEY", apiMessage),
        httpCode = code
    )

    val invalidResponse: ApiError = ApiError(
        type = ApiErrorType.INVALID_RESPONSE,
        userMessage = "সার্ভার থেকে প্রত্যাশিত ফরম্যাটে সাড়া আসেনি। মডেলের নাম ও key যাচাই করুন। (Malformed API response)",
        technicalDetail = "Response body missing expected candidates/content"
    )

    fun toConnectionState(error: ApiError): GeminiConnectionState = when (error.type) {
        ApiErrorType.INVALID_API_KEY -> GeminiConnectionState.INVALID_KEY
        ApiErrorType.MODEL_UNAVAILABLE -> GeminiConnectionState.MODEL_UNAVAILABLE
        ApiErrorType.HTTP_429 -> GeminiConnectionState.RATE_LIMITED
        else -> GeminiConnectionState.NETWORK_ERROR
    }

    private fun parseErrorBody(body: String?): GeminiError? {
        if (body.isNullOrBlank()) return null
        return try {
            val moshi = com.squareup.moshi.Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            // The real Gemini error body is {"error": {"code":..,"message":..,"status":..}}
            // — unwrap the envelope before reading the fields.
            moshi.adapter(GeminiErrorEnvelope::class.java).fromJson(body)?.error
        } catch (e: Exception) {
            null
        }
    }

    private fun describe(code: Int, status: String, message: String): String {
        val parts = mutableListOf("HTTP $code")
        if (status.isNotBlank()) parts.add("status=$status")
        if (message.isNotBlank()) parts.add(message.take(300))
        return parts.joinToString(" ")
    }
}

/** Result wrapper for a real Gemini API call. */
sealed class GeminiResult {
    data class Success(val body: GenerateContentResponse) : GeminiResult()
    data class Failure(val error: ApiError) : GeminiResult()
}
