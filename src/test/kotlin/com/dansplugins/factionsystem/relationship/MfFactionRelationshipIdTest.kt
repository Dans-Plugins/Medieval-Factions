package com.dansplugins.factionsystem.relationship

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MfFactionRelationshipIdTest {

    @Test
    fun testGenerate() {
        // execute
        val relationshipId = MfFactionRelationshipId.generate()

        // verify - UUID string format is 36 characters
        assertEquals(36, relationshipId.value.length)
    }

    @Test
    fun testGenerateCreatesUniqueIds() {
        // execute
        val relationshipId1 = MfFactionRelationshipId.generate()
        val relationshipId2 = MfFactionRelationshipId.generate()

        // verify
        assertNotEquals(relationshipId1.value, relationshipId2.value)
    }

    @Test
    fun testConstructorWithValue() {
        // prepare
        val value = "test-relationship-id"

        // execute
        val relationshipId = MfFactionRelationshipId(value)

        // verify
        assertEquals(value, relationshipId.value)
    }

    @Test
    fun testEquality() {
        // prepare
        val value = "test-relationship-id"

        // execute
        val relationshipId1 = MfFactionRelationshipId(value)
        val relationshipId2 = MfFactionRelationshipId(value)

        // verify
        assertEquals(relationshipId1, relationshipId2)
        assertEquals(relationshipId1.value, relationshipId2.value)
    }

    @Test
    fun testInequality() {
        // prepare
        val value1 = "relationship-1"
        val value2 = "relationship-2"

        // execute
        val relationshipId1 = MfFactionRelationshipId(value1)
        val relationshipId2 = MfFactionRelationshipId(value2)

        // verify
        assertNotEquals(relationshipId1, relationshipId2)
        assertNotEquals(relationshipId1.value, relationshipId2.value)
    }
}
