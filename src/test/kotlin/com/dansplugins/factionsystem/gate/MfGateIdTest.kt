package com.dansplugins.factionsystem.gate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MfGateIdTest {

    @Test
    fun testGenerate() {
        // execute
        val gateId = MfGateId.generate()

        // verify - UUID string format is 36 characters
        assertEquals(36, gateId.value.length)
    }

    @Test
    fun testGenerateCreatesUniqueIds() {
        // execute
        val gateId1 = MfGateId.generate()
        val gateId2 = MfGateId.generate()

        // verify
        assertNotEquals(gateId1.value, gateId2.value)
    }

    @Test
    fun testConstructorWithValue() {
        // prepare
        val value = "test-gate-id"

        // execute
        val gateId = MfGateId(value)

        // verify
        assertEquals(value, gateId.value)
    }

    @Test
    fun testEquality() {
        // prepare
        val value = "test-gate-id"

        // execute
        val gateId1 = MfGateId(value)
        val gateId2 = MfGateId(value)

        // verify
        assertEquals(gateId1, gateId2)
        assertEquals(gateId1.value, gateId2.value)
    }

    @Test
    fun testInequality() {
        // prepare
        val value1 = "gate-1"
        val value2 = "gate-2"

        // execute
        val gateId1 = MfGateId(value1)
        val gateId2 = MfGateId(value2)

        // verify
        assertNotEquals(gateId1, gateId2)
        assertNotEquals(gateId1.value, gateId2.value)
    }
}
