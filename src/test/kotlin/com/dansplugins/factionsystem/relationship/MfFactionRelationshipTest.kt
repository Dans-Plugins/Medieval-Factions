package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MfFactionRelationshipTest {

    @Test
    fun testInitialization() {
        // prepare
        val id = MfFactionRelationshipId.generate()
        val factionId = MfFactionId.generate()
        val targetId = MfFactionId.generate()
        val type = MfFactionRelationshipType.ALLY

        // execute
        val relationship = MfFactionRelationship(id, factionId, targetId, type)

        // verify
        assertEquals(id, relationship.id)
        assertEquals(factionId, relationship.factionId)
        assertEquals(targetId, relationship.targetId)
        assertEquals(type, relationship.type)
    }

    @Test
    fun testInitializationWithDefaultId() {
        // prepare
        val factionId = MfFactionId.generate()
        val targetId = MfFactionId.generate()
        val type = MfFactionRelationshipType.AT_WAR

        // execute
        val relationship = MfFactionRelationship(factionId = factionId, targetId = targetId, type = type)

        // verify
        assertEquals(factionId, relationship.factionId)
        assertEquals(targetId, relationship.targetId)
        assertEquals(type, relationship.type)
        assertEquals(36, relationship.id.value.length) // UUID format
    }

    @Test
    fun testAllyRelationship() {
        // prepare
        val factionId = MfFactionId.generate()
        val targetId = MfFactionId.generate()

        // execute
        val relationship = MfFactionRelationship(factionId = factionId, targetId = targetId, type = MfFactionRelationshipType.ALLY)

        // verify
        assertEquals(MfFactionRelationshipType.ALLY, relationship.type)
    }

    @Test
    fun testWarRelationship() {
        // prepare
        val factionId = MfFactionId.generate()
        val targetId = MfFactionId.generate()

        // execute
        val relationship = MfFactionRelationship(factionId = factionId, targetId = targetId, type = MfFactionRelationshipType.AT_WAR)

        // verify
        assertEquals(MfFactionRelationshipType.AT_WAR, relationship.type)
    }

    @Test
    fun testVassalRelationship() {
        // prepare
        val factionId = MfFactionId.generate()
        val targetId = MfFactionId.generate()

        // execute
        val relationship = MfFactionRelationship(factionId = factionId, targetId = targetId, type = MfFactionRelationshipType.VASSAL)

        // verify
        assertEquals(MfFactionRelationshipType.VASSAL, relationship.type)
    }

    @Test
    fun testLiegeRelationship() {
        // prepare
        val factionId = MfFactionId.generate()
        val targetId = MfFactionId.generate()

        // execute
        val relationship = MfFactionRelationship(factionId = factionId, targetId = targetId, type = MfFactionRelationshipType.LIEGE)

        // verify
        assertEquals(MfFactionRelationshipType.LIEGE, relationship.type)
    }

    @Test
    fun testEqualityWithSameValues() {
        // prepare
        val id = MfFactionRelationshipId.generate()
        val factionId = MfFactionId.generate()
        val targetId = MfFactionId.generate()
        val type = MfFactionRelationshipType.ALLY

        // execute
        val relationship1 = MfFactionRelationship(id, factionId, targetId, type)
        val relationship2 = MfFactionRelationship(id, factionId, targetId, type)

        // verify
        assertEquals(relationship1, relationship2)
    }

    @Test
    fun testInequalityWithDifferentIds() {
        // prepare
        val factionId = MfFactionId.generate()
        val targetId = MfFactionId.generate()
        val type = MfFactionRelationshipType.ALLY

        // execute
        val relationship1 = MfFactionRelationship(MfFactionRelationshipId.generate(), factionId, targetId, type)
        val relationship2 = MfFactionRelationship(MfFactionRelationshipId.generate(), factionId, targetId, type)

        // verify
        assertNotEquals(relationship1, relationship2)
    }

    @Test
    fun testInequalityWithDifferentTypes() {
        // prepare
        val id = MfFactionRelationshipId.generate()
        val factionId = MfFactionId.generate()
        val targetId = MfFactionId.generate()

        // execute
        val relationship1 = MfFactionRelationship(id, factionId, targetId, MfFactionRelationshipType.ALLY)
        val relationship2 = MfFactionRelationship(id, factionId, targetId, MfFactionRelationshipType.AT_WAR)

        // verify
        assertNotEquals(relationship1, relationship2)
    }

    @Test
    fun testBidirectionalRelationship() {
        // prepare - faction A and faction B can have relationships in both directions
        val factionA = MfFactionId.generate()
        val factionB = MfFactionId.generate()

        // execute
        val aToB = MfFactionRelationship(factionId = factionA, targetId = factionB, type = MfFactionRelationshipType.ALLY)
        val bToA = MfFactionRelationship(factionId = factionB, targetId = factionA, type = MfFactionRelationshipType.ALLY)

        // verify - they are different relationships
        assertEquals(factionA, aToB.factionId)
        assertEquals(factionB, aToB.targetId)
        assertEquals(factionB, bToA.factionId)
        assertEquals(factionA, bToA.targetId)
        assertNotEquals(aToB, bToA)
    }
}
