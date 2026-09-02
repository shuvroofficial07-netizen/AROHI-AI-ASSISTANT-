package com.example.core

import com.example.core.text.InputLanguage
import com.example.core.text.TextProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextProcessorTest {

    @Test
    fun `detects bengali script`() {
        assertEquals(InputLanguage.BENGALI, TextProcessor.detectLanguage("ব্যাটারি কত?"))
    }

    @Test
    fun `detects english`() {
        assertEquals(InputLanguage.ENGLISH, TextProcessor.detectLanguage("What is the capital of France?"))
    }

    @Test
    fun `detects banglish`() {
        assertEquals(InputLanguage.BANGLISH, TextProcessor.detectLanguage("battery koy percent?"))
        assertEquals(InputLanguage.BANGLISH, TextProcessor.detectLanguage("timer dao 10 min"))
    }

    @Test
    fun `detects mixed script`() {
        assertEquals(InputLanguage.MIXED, TextProcessor.detectLanguage("YouTube খোলো please"))
    }

    @Test
    fun `strips bengali wake word`() {
        val stripped = TextProcessor.stripWakeWord("অরোহী, ব্যাটারি কত?")
        assertFalse(stripped.startsWith("অরোহী"))
        assertTrue(stripped.contains("ব্যাটারি"))
    }

    @Test
    fun `strips english wake word`() {
        val stripped = TextProcessor.stripWakeWord("Arohi, open YouTube")
        assertTrue(stripped.contains("YouTube"))
    }

    @Test
    fun `detects wake word presence`() {
        assertTrue(TextProcessor.containsWakeWord("hey arohi what time is it"))
        assertFalse(TextProcessor.containsWakeWord("what time is it"))
    }

    @Test
    fun `normalizes whitespace`() {
        assertEquals("open  youtube", TextProcessor.normalize("  open   youtube  "))
    }
}
