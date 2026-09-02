package com.example.core

import com.example.core.personality.EmotionalContext
import com.example.core.personality.PersonalityEngine
import com.example.core.personality.PersonalityTrait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityEngineTest {

    @Test
    fun `detects urgent context`() {
        val engine = PersonalityEngine()
        assertEquals(EmotionalContext.URGENT, engine.detectContext("এটা এখনই দরকার, hurry!"))
    }

    @Test
    fun `detects frustrated context`() {
        val engine = PersonalityEngine()
        assertEquals(EmotionalContext.FRUSTRATED, engine.detectContext("This is not working again, so stupid"))
    }

    @Test
    fun `detects happy context`() {
        val engine = PersonalityEngine()
        assertEquals(EmotionalContext.HAPPY, engine.detectContext("Thank you so much, darun!"))
    }

    @Test
    fun `defaults to neutral`() {
        val engine = PersonalityEngine()
        assertEquals(EmotionalContext.NEUTRAL, engine.detectContext("আজকের আবহাওয়া কেমন?"))
    }

    @Test
    fun `style directive reflects traits`() {
        val engine = PersonalityEngine(setOf(PersonalityTrait.PROFESSIONAL))
        assertTrue(engine.styleDirective().contains("professional"))
    }

    @Test
    fun `empty traits fall back to warm`() {
        val engine = PersonalityEngine(emptySet())
        assertTrue(engine.has(PersonalityTrait.WARM))
    }

    @Test
    fun `adaptive prefix for urgent is non-empty`() {
        val engine = PersonalityEngine()
        assertTrue(engine.adaptivePrefix(EmotionalContext.URGENT).isNotBlank())
        assertEquals("", engine.adaptivePrefix(EmotionalContext.NEUTRAL))
    }
}
