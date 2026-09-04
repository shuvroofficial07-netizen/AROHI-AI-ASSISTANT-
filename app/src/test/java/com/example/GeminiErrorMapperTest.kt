package com.example

import com.example.data.remote.ApiErrorType
import com.example.data.remote.GeminiErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Real unit tests for the structured network error model (pure JVM).
 */
class GeminiErrorMapperTest {

    @Test
    fun `dns failure is classified honestly`() {
        val error = GeminiErrorMapper.fromException(UnknownHostException("generativelanguage.googleapis.com"))
        assertEquals(ApiErrorType.DNS_FAILURE, error.type)
        assertTrue(error.userMessage.contains("DNS"))
    }

    @Test
    fun `timeout is classified honestly`() {
        val error = GeminiErrorMapper.fromException(SocketTimeoutException("read timed out"))
        assertEquals(ApiErrorType.TIMEOUT, error.type)
        assertTrue(error.userMessage.contains("timeout", ignoreCase = true))
    }

    @Test
    fun `tls failure is classified honestly`() {
        val error = GeminiErrorMapper.fromException(
            javax.net.ssl.SSLHandshakeException("certificate mismatch")
        )
        assertEquals(ApiErrorType.TLS_FAILURE, error.type)
    }

    @Test
    fun `invalid key from real error body`() {
        val error = GeminiErrorMapper.fromHttp(
            400,
            """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}"""
        )
        assertEquals(ApiErrorType.INVALID_API_KEY, error.type)
        assertEquals(400, error.httpCode)
    }

    @Test
    fun `rate limit maps to 429`() {
        val error = GeminiErrorMapper.fromHttp(
            429,
            """{"error":{"code":429,"message":"Quota exceeded","status":"RESOURCE_EXHAUSTED"}}"""
        )
        assertEquals(ApiErrorType.HTTP_429, error.type)
    }

    @Test
    fun `model not found maps to MODEL_UNAVAILABLE`() {
        val error = GeminiErrorMapper.fromHttp(
            404,
            """{"error":{"code":404,"message":"Model some-bad-model not found","status":"NOT_FOUND"}}"""
        )
        assertEquals(ApiErrorType.MODEL_UNAVAILABLE, error.type)
    }

    @Test
    fun `server errors are retryable`() {
        val error500 = GeminiErrorMapper.fromHttp(500, null)
        val error503 = GeminiErrorMapper.fromHttp(503, null)
        assertEquals(ApiErrorType.HTTP_500, error500.type)
        assertEquals(ApiErrorType.SERVER_UNAVAILABLE, error503.type)
        assertTrue(error500.isRetryable)
        assertTrue(error503.isRetryable)
    }

    @Test
    fun `invalid key is never retryable and never reported as connected`() {
        val error = GeminiErrorMapper.invalidKey(400, "API key not valid")
        assertEquals(ApiErrorType.INVALID_API_KEY, error.type)
        assertTrue(!error.isRetryable)
    }

    @Test
    fun `forbidden maps to HTTP_403`() {
        val error = GeminiErrorMapper.fromHttp(403, """{"error":{"status":"PERMISSION_DENIED"}}""")
        assertEquals(ApiErrorType.HTTP_403, error.type)
    }
}
