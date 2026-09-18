package com.example

import com.example.engine.ArohiEmotion
import com.example.engine.CalculatorEngine
import com.example.engine.EmotionTagParser
import com.example.engine.TimePhraseParser
import com.example.engine.UnitConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the offline layer that makes AROHI useful without any cloud key.
 * These do not touch Android APIs, so they run on a plain JUnit test task.
 */
class OfflineEngineTest {

    @Test
    fun `emotion tag is parsed and stripped`() {
        val parsed = EmotionTagParser.parse("<emotion>happy</emotion> আজকের দিনটা দারুণ যাচ্ছে!")
        assertEquals(ArohiEmotion.HAPPY, parsed.emotion)
        assertTrue(parsed.hadTag)
        assertTrue(parsed.text.startsWith("আজকের"))
        assertTrue(!parsed.text.contains("emotion"))
    }

    @Test
    fun `reply without a tag still yields text`() {
        val parsed = EmotionTagParser.parse("ঠিক আছে, করে দিচ্ছি।")
        assertEquals(null, parsed.emotion)
        assertEquals("ঠিক আছে, করে দিচ্ছি।", parsed.text)
    }

    @Test
    fun `every allowed tag maps to an emotion`() {
        val allowed = listOf("neutral", "happy", "excited", "thinking", "concerned", "laughing", "sad", "surprised")
        for (tag in allowed) {
            assertNotNull("tag $tag should map", EmotionTagParser.mapToEmotion(tag))
        }
    }

    @Test
    fun `calculator handles bengali digits and word operators`() {
        val symbolic = CalculatorEngine.calculate("২৫৬ + ১২৫")
        assertTrue(symbolic is CalculatorEngine.Result.Success)
        assertEquals(381.0, (symbolic as CalculatorEngine.Result.Success).value, 0.001)

        val worded = CalculatorEngine.calculate("১০০ সাথে ৫০ যোগ করো")
        assertTrue(worded is CalculatorEngine.Result.Success)
        assertEquals(150.0, (worded as CalculatorEngine.Result.Success).value, 0.001)
    }

    @Test
    fun `plain sentence is not treated as a calculation`() {
        assertTrue(!CalculatorEngine.looksLikeCalculation("আজকের আবহাওয়া কেমন?"))
    }

    @Test
    fun `unit conversion recognises a phrase and returns a real answer`() {
        val text = "১০ কেজি থেকে পাউন্ড"
        assertTrue(UnitConverter.looksLikeConversion(text.lowercase()))
        val answer = UnitConverter.convertFromText(text)
        assertTrue(answer.isNotBlank())
        assertTrue(answer.contains("পাউন্ড"))
    }

    @Test
    fun `relative time phrases resolve into the future`() {
        val now = 1_700_000_000_000L
        val tenMinutes = TimePhraseParser.parse("১০ মিনিট পরে জানাও", now)
        assertNotNull(tenMinutes)
        assertEquals(now + 10 * 60_000L, tenMinutes!!)

        val later = TimePhraseParser.parse("২ ঘন্টা পরে", now)
        assertNotNull(later)
        assertEquals(now + 2 * 3_600_000L, later!!)
    }

    @Test
    fun `bengali digits are normalised for parsing`() {
        assertEquals("125", TimePhraseParser.normalizeDigits("১২৫"))
        assertEquals("১২৫", TimePhraseParser.toBengaliDigits("125"))
    }
}
