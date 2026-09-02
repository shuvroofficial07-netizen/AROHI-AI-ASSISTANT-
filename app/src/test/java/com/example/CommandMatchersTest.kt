package com.example

import com.example.engine.CommandMatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Real unit tests for the local command matcher logic (pure JVM, no Android).
 */
class CommandMatchersTest {

    // --- Silence command -------------------------------------------------

    @Test
    fun `plain silence phrases are recognized`() {
        assertTrue(CommandMatchers.isSilenceCommand("চুপ করো"))
        assertTrue(CommandMatchers.isSilenceCommand("চুপ"))
        assertTrue(CommandMatchers.isSilenceCommand("থামো"))
        assertTrue(CommandMatchers.isSilenceCommand("Stop talking"))
        assertTrue(CommandMatchers.isSilenceCommand("shut up"))
        assertTrue(CommandMatchers.isSilenceCommand("be quiet!"))
        assertTrue(CommandMatchers.isSilenceCommand("  ব্যাস  "))
    }

    @Test
    fun `torch off command is NOT hijacked by silence matcher`() {
        // Regression: previously "টর্চ বন্ধ করো" was treated as a silence command
        // because it contains "বন্ধ করো" — the torch handler never ran.
        assertFalse(CommandMatchers.isSilenceCommand("টর্চ বন্ধ করো"))
        assertFalse(CommandMatchers.isSilenceCommand("flashlight off"))
        assertFalse(CommandMatchers.isSilenceCommand("torch বন্ধ করো"))
    }

    @Test
    fun `other device commands are not treated as silence`() {
        assertFalse(CommandMatchers.isSilenceCommand("stop the music"))
        assertFalse(CommandMatchers.isSilenceCommand("stopwatch খোলো"))
        assertFalse(CommandMatchers.isSilenceCommand("ভলিউম বন্ধ করো"))
        assertFalse(CommandMatchers.isSilenceCommand("set an alarm to stop me"))
        assertFalse(CommandMatchers.isSilenceCommand("মিউজিক থামাও"))
        assertFalse(CommandMatchers.isSilenceCommand("record করো"))
    }

    @Test
    fun `long sentences are never silence commands`() {
        assertFalse(CommandMatchers.isSilenceCommand("please stop explaining quantum physics to me right now"))
    }

    @Test
    fun `empty input is not silence`() {
        assertFalse(CommandMatchers.isSilenceCommand(""))
        assertFalse(CommandMatchers.isSilenceCommand("   "))
    }

    // --- Memory save -----------------------------------------------------

    @Test
    fun `memory save triggers are recognized`() {
        assertTrue(CommandMatchers.isMemorySaveQuery("মনে রেখো আমার পছন্দের রং নীল"))
        assertTrue(CommandMatchers.isMemorySaveQuery("Remember that my birthday is in May"))
        assertTrue(CommandMatchers.isMemorySaveQuery("মনে রাখো আমি চা ভালোবাসি"))
    }

    @Test
    fun `memory save fact extraction works`() {
        assertEquals(
            "আমার পছন্দের রং নীল",
            CommandMatchers.extractMemoryFact("মনে রেখো আমার পছন্দের রং নীল।")
        )
        assertEquals(
            "my birthday is in May",
            CommandMatchers.extractMemoryFact("remember that my birthday is in May")
        )
        assertEquals("", CommandMatchers.extractMemoryFact("what is the weather"))
    }

    @Test
    fun `random sentences are not memory saves`() {
        assertFalse(CommandMatchers.isMemorySaveQuery("আজকের আবহাওয়া কেমন"))
        assertFalse(CommandMatchers.isMemorySaveQuery("battery কত"))
    }

    // --- Memory recall ---------------------------------------------------

    @Test
    fun `memory recall triggers are recognized`() {
        assertTrue(CommandMatchers.isMemoryRecallQuery("তোমার মেমোরিতে কী আছে?"))
        assertTrue(CommandMatchers.isMemoryRecallQuery("what do you remember"))
        assertTrue(CommandMatchers.isMemoryRecallQuery("কি মনে আছে বলো তো"))
    }

    @Test
    fun `ram query is not memory recall`() {
        // "মেমোরি" alone refers to RAM — must keep routing to device telemetry.
        assertFalse(CommandMatchers.isMemoryRecallQuery("মেমোরি কত খালি"))
        assertFalse(CommandMatchers.isMemoryRecallQuery("ram কত"))
    }
}
