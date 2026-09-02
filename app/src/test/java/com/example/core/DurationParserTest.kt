package com.example.core

import com.example.core.text.DurationParser
import com.example.core.text.TextProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DurationParserTest {

    @Test
    fun `parses english minute timer`() {
        val d = DurationParser.parseDuration("set a timer for 20 minutes")
        assertNotNull(d)
        assertEquals(20 * 60_000L, d!!.totalMillis)
    }

    @Test
    fun `parses ascii seconds`() {
        val d = DurationParser.parseDuration("timer dao 45 seconds")
        assertNotNull(d)
        assertEquals(45_000L, d!!.totalMillis)
    }

    @Test
    fun `parses bengali digit minutes`() {
        // "২০ মিনিট" -> digits normalized to 20
        val d = DurationParser.parseDuration(TextProcessor.bengaliDigitsToAscii("২০ মিনিট টাইমার"))
        assertNotNull(d)
        assertEquals(20 * 60_000L, d!!.totalMillis)
    }

    @Test
    fun `parses banglish minute via min keyword`() {
        val d = DurationParser.parseDuration("timer dao 10 min")
        assertNotNull(d)
        assertEquals(10 * 60_000L, d!!.totalMillis)
    }

    @Test
    fun `no duration returns null`() {
        assertNull(DurationParser.parseDuration("আমার আজকের schedule বলো"))
    }

    @Test
    fun `bengali digits convert to ascii`() {
        assertEquals("20", TextProcessor.bengaliDigitsToAscii("২০"))
        assertEquals("7:30", TextProcessor.bengaliDigitsToAscii("৭:৩০"))
    }

    @Test
    fun `clock time parses hour and minute`() {
        val t = DurationParser.parseClockTime("7:30")
        assertNotNull(t)
        assertEquals(7, t!!.hour24)
        assertEquals(30, t.minute)
    }
}
