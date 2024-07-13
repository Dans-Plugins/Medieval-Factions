package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.player.MfPlayerId
import org.junit.Test
import org.junit.Assert.assertEquals

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