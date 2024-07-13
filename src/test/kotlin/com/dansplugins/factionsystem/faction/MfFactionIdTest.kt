package com.dansplugins.factionsystem.faction

import org.junit.Assert.assertEquals
import org.junit.Test

class MfFactionIdTest {
    /**
     * Test the MfFactionId.generate() method
     */
    @Test
    fun testGenerate() {
        val factionId = MfFactionId.generate()
        assertEquals(36, factionId.value.length)
    }
}
