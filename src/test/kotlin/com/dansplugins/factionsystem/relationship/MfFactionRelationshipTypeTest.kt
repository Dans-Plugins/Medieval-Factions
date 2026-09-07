package com.dansplugins.factionsystem.relationship

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MfFactionRelationshipTypeTest {

    @Test
    fun testAllyType() {
        // execute
        val type = MfFactionRelationshipType.ALLY

        // verify
        assertEquals("ALLY", type.name)
    }

    @Test
    fun testAtWarType() {
        // execute
        val type = MfFactionRelationshipType.AT_WAR

        // verify
        assertEquals("AT_WAR", type.name)
    }

    @Test
    fun testVassalType() {
        // execute
        val type = MfFactionRelationshipType.VASSAL

        // verify
        assertEquals("VASSAL", type.name)
    }

    @Test
    fun testLiegeType() {
        // execute
        val type = MfFactionRelationshipType.LIEGE

        // verify
        assertEquals("LIEGE", type.name)
    }

    @Test
    fun testAllTypesPresent() {
        // execute
        val types = MfFactionRelationshipType.values()

        // verify - ensure all 4 types exist
        assertEquals(4, types.size)
        assertTrue(types.contains(MfFactionRelationshipType.ALLY))
        assertTrue(types.contains(MfFactionRelationshipType.AT_WAR))
        assertTrue(types.contains(MfFactionRelationshipType.VASSAL))
        assertTrue(types.contains(MfFactionRelationshipType.LIEGE))
    }

    @Test
    fun testValueOf() {
        // execute & verify
        assertEquals(MfFactionRelationshipType.ALLY, MfFactionRelationshipType.valueOf("ALLY"))
        assertEquals(MfFactionRelationshipType.AT_WAR, MfFactionRelationshipType.valueOf("AT_WAR"))
        assertEquals(MfFactionRelationshipType.VASSAL, MfFactionRelationshipType.valueOf("VASSAL"))
        assertEquals(MfFactionRelationshipType.LIEGE, MfFactionRelationshipType.valueOf("LIEGE"))
    }

    @Test
    fun testEnumOrdinal() {
        // execute & verify - test ordering
        assertEquals(0, MfFactionRelationshipType.ALLY.ordinal)
        assertEquals(1, MfFactionRelationshipType.AT_WAR.ordinal)
        assertEquals(2, MfFactionRelationshipType.VASSAL.ordinal)
        assertEquals(3, MfFactionRelationshipType.LIEGE.ordinal)
    }
}
