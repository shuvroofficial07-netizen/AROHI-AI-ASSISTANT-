package com.example.core

import com.example.core.intent.IntentClassifier
import com.example.core.intent.IntentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentClassifierTest {
    private val classifier = IntentClassifier(
        appAliases = mapOf("youtube" to "YouTube", "youtube" to "YouTube")
    )

    private fun typeOf(input: String) = classifier.classify(input).type

    @Test
    fun `silence command in Bengali`() {
        assertEquals(IntentType.STOP_SPEAKING, typeOf("অরোহী, চুপ করো।"))
        assertEquals(IntentType.STOP_SPEAKING, typeOf("stop talking"))
    }

    @Test
    fun `battery status in multiple languages`() {
        assertEquals(IntentType.BATTERY_STATUS, typeOf("ব্যাটারি কত পারসেন্ট?"))
        assertEquals(IntentType.BATTERY_STATUS, typeOf("battery koy percent?"))
        assertEquals(IntentType.BATTERY_STATUS, typeOf("what is my battery level"))
    }

    @Test
    fun `timer command with bengali digits`() {
        val intent = classifier.classify("অরোহী, একটা timer দাও ২০ মিনিটের।")
        assertEquals(IntentType.SET_TIMER, intent.type)
        assertTrue(intent.confidence > 0.8f)
    }

    @Test
    fun `alarm command`() {
        assertEquals(IntentType.SET_ALARM, typeOf("সকাল ৭টায় alarm দাও"))
    }

    @Test
    fun `flashlight on and off`() {
        val on = classifier.classify("টর্চ জ্বালাও")
        assertEquals(IntentType.FLASHLIGHT, on.type)
        assertEquals("true", on.slot("enable"))

        val off = classifier.classify("flashlight off করো")
        assertEquals(IntentType.FLASHLIGHT, off.type)
        assertEquals("false", off.slot("enable"))
    }

    @Test
    fun `diagnostics command`() {
        assertEquals(IntentType.RUN_SELF_TEST, typeOf("run a full system test"))
        assertEquals(IntentType.RUN_DIAGNOSTICS, typeOf("তুমি ঠিক আছিস?"))
    }

    @Test
    fun `open app command`() {
        assertEquals(IntentType.OPEN_APP, typeOf("YouTube খোলো"))
        assertEquals(IntentType.OPEN_APP, typeOf("chrome open koro"))
    }

    @Test
    fun `call contact is marked sensitive`() {
        val intent = classifier.classify("Rahim-কে call করার জন্য Phone খুলে দাও")
        assertEquals(IntentType.CALL_CONTACT, intent.type)
        assertTrue(intent.requiresConfirmation)
    }

    @Test
    fun `memory remember command`() {
        assertEquals(IntentType.MEMORY_SAVE, typeOf("মনে রাখো আমার গাড়ির নাম্বার ১২৩"))
    }

    @Test
    fun `notification summary`() {
        assertEquals(IntentType.SUMMARIZE_NOTIFICATIONS, typeOf("আমার notificationগুলো summarize করো"))
    }

    @Test
    fun `unknown chat falls through to cloud`() {
        val intent = classifier.classify("মহাকাশে কয়টা গ্রহ আছে?")
        assertEquals(IntentType.CHAT, intent.type)
    }

    @Test
    fun `translate command detected`() {
        assertEquals(IntentType.TRANSLATE, typeOf("এই লেখাটা English-এ translate করো"))
    }
}
