package com.dansplugins.factionsystem.law

import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.faction.MfFaction
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MfLawTest {
    private val testUtils = TestUtils()

    @Test
    fun testInitializationWithAllParameters() {
        // prepare
        val id = MfLawId.generate()
        val version = 1
        val factionId = testUtils.createFactionId()
        val text = "No stealing from faction members"
        val number = 1

        // execute
        val law = MfLaw(id, version, factionId, text, number)

        // verify
        assertEquals(id, law.id)
        assertEquals(version, law.version)
        assertEquals(factionId, law.factionId)
        assertEquals(text, law.text)
        assertEquals(number, law.number)
    }

    @Test
    fun testInitializationWithFaction() {
        // prepare
        val factionId = testUtils.createFactionId()
        val faction = mock(MfFaction::class.java)
        `when`(faction.id).thenReturn(factionId)
        val text = "Follow server rules"

        // execute
        val law = MfLaw(faction, text)

        // verify
        assertNotNull(law.id)
        assertEquals(36, law.id.value.length) // UUID format
        assertEquals(1, law.version)
        assertEquals(factionId, law.factionId)
        assertEquals(text, law.text)
        assertNull(law.number)
    }

    @Test
    fun testLawWithNumber() {
        // prepare
        val id = MfLawId.generate()
        val factionId = testUtils.createFactionId()
        val text = "Law with number"
        val number = 5

        // execute
        val law = MfLaw(id, 1, factionId, text, number)

        // verify
        assertEquals(number, law.number)
    }

    @Test
    fun testLawWithoutNumber() {
        // prepare
        val id = MfLawId.generate()
        val factionId = testUtils.createFactionId()
        val text = "Law without number"

        // execute
        val law = MfLaw(id, 1, factionId, text, null)

        // verify
        assertNull(law.number)
    }

    @Test
    fun testLawWithEmptyText() {
        // prepare
        val id = MfLawId.generate()
        val factionId = testUtils.createFactionId()
        val text = ""

        // execute
        val law = MfLaw(id, 1, factionId, text, null)

        // verify
        assertEquals("", law.text)
    }

    @Test
    fun testLawWithLongText() {
        // prepare
        val id = MfLawId.generate()
        val factionId = testUtils.createFactionId()
        val longText = "A".repeat(500)

        // execute
        val law = MfLaw(id, 1, factionId, longText, null)

        // verify
        assertEquals(500, law.text.length)
    }

    @Test
    fun testLawWithSpecialCharacters() {
        // prepare
        val id = MfLawId.generate()
        val factionId = testUtils.createFactionId()
        val text = "No griefing! @#$% 你好 🎉"

        // execute
        val law = MfLaw(id, 1, factionId, text, null)

        // verify
        assertEquals(text, law.text)
    }

    @Test
    fun testVersioning() {
        // prepare
        val id = MfLawId.generate()
        val factionId = testUtils.createFactionId()
        val text = "Test law"

        // execute
        val lawV0 = MfLaw(id, 0, factionId, text, null)
        val lawV1 = MfLaw(id, 1, factionId, text, null)
        val lawV2 = MfLaw(id, 2, factionId, text, null)

        // verify
        assertEquals(0, lawV0.version)
        assertEquals(1, lawV1.version)
        assertEquals(2, lawV2.version)
    }

    @Test
    fun testMultipleLawsForSameFaction() {
        // prepare
        val factionId = testUtils.createFactionId()

        // execute
        val law1 = MfLaw(MfLawId.generate(), 1, factionId, "Law 1", 1)
        val law2 = MfLaw(MfLawId.generate(), 1, factionId, "Law 2", 2)
        val law3 = MfLaw(MfLawId.generate(), 1, factionId, "Law 3", 3)

        // verify
        assertEquals(factionId, law1.factionId)
        assertEquals(factionId, law2.factionId)
        assertEquals(factionId, law3.factionId)
        assertEquals(1, law1.number)
        assertEquals(2, law2.number)
        assertEquals(3, law3.number)
    }

    @Test
    fun testCopyWithUpdatedText() {
        // prepare
        val id = MfLawId.generate()
        val factionId = testUtils.createFactionId()
        val originalLaw = MfLaw(id, 1, factionId, "Original text", 1)

        // execute
        val updatedLaw = originalLaw.copy(text = "Updated text", version = 2)

        // verify
        assertEquals("Updated text", updatedLaw.text)
        assertEquals(2, updatedLaw.version)
        assertEquals(id, updatedLaw.id)
        assertEquals(factionId, updatedLaw.factionId)
    }
}
