package com.example.core

import com.example.core.ai.AiErrorMapper
import com.example.core.result.ArohiErrorCode
import org.junit.Assert.assertEquals
import org.junit.Test

class AiErrorMapperTest {
    @Test
    fun `auth errors map to invalid key`() {
        assertEquals(ArohiErrorCode.API_KEY_INVALID, AiErrorMapper.fromHttp(401))
        assertEquals(ArohiErrorCode.API_KEY_INVALID, AiErrorMapper.fromHttp(403))
        assertEquals(ArohiErrorCode.API_KEY_INVALID, AiErrorMapper.fromHttp(400))
    }

    @Test
    fun `timeout maps to api timeout`() {
        assertEquals(ArohiErrorCode.API_TIMEOUT, AiErrorMapper.fromHttp(408))
        assertEquals(ArohiErrorCode.API_TIMEOUT, AiErrorMapper.fromHttp(504))
    }

    @Test
    fun `server errors map to unavailable`() {
        assertEquals(ArohiErrorCode.API_UNAVAILABLE, AiErrorMapper.fromHttp(500))
        assertEquals(ArohiErrorCode.API_UNAVAILABLE, AiErrorMapper.fromHttp(503))
        assertEquals(ArohiErrorCode.API_UNAVAILABLE, AiErrorMapper.fromHttp(429))
    }
}
