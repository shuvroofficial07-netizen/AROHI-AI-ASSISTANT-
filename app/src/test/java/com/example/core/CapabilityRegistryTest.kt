package com.example.core

import com.example.core.capability.CapabilityCategory
import com.example.core.capability.CapabilityRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityRegistryTest {

    @Test
    fun `registry exposes core capabilities`() {
        val ids = CapabilityRegistry.all.map { it.id }
        assertTrue(ids.contains("voice_listen"))
        assertTrue(ids.contains("set_timer") || ids.contains("timer"))
        assertTrue(ids.contains("alarm"))
        assertTrue(ids.contains("accessibility"))
        assertTrue(ids.contains("notification_access"))
    }

    @Test
    fun `every capability has a probe key and known min version`() {
        for (cap in CapabilityRegistry.all) {
            assertNotNull("Missing probeKey for ${cap.id}", cap.probeKey)
            assertTrue("min Android must be <= 28 for Android 9", cap.minAndroidVersion <= 28)
        }
    }

    @Test
    fun `categories are populated`() {
        assertTrue(CapabilityRegistry.byCategory(CapabilityCategory.VOICE).isNotEmpty())
        assertTrue(CapabilityRegistry.byCategory(CapabilityCategory.TIME).isNotEmpty())
    }

    @Test
    fun `lookup by id works`() {
        assertNotNull(CapabilityRegistry.byId("flashlight"))
    }
}
