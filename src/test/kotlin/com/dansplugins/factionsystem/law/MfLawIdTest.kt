package com.dansplugins.factionsystem.law

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MfLawIdTest {

    @Test
    fun testGenerate() {
        // execute
        val lawId = MfLawId.generate()

        // verify - UUID string format is 36 characters
        assertEquals(36, lawId.value.length)
    }

    @Test
    fun testGenerateCreatesUniqueIds() {
        // execute
        val lawId1 = MfLawId.generate()
        val lawId2 = MfLawId.generate()

        // verify
        assertNotEquals(lawId1.value, lawId2.value)
    }

    @Test
    fun testConstructorWithValue() {
        // prepare
        val value = "test-law-id"

        // execute
        val lawId = MfLawId(value)

        // verify
        assertEquals(value, lawId.value)
    }

    @Test
    fun testEquality() {
        // prepare
        val value = "test-law-id"

        // execute
        val lawId1 = MfLawId(value)
        val lawId2 = MfLawId(value)

        // verify
        assertEquals(lawId1, lawId2)
        assertEquals(lawId1.value, lawId2.value)
    }

    @Test
    fun testInequality() {
        // prepare
        val value1 = "law-1"
        val value2 = "law-2"

        // execute
        val lawId1 = MfLawId(value1)
        val lawId2 = MfLawId(value2)

        // verify
        assertNotEquals(lawId1, lawId2)
        assertNotEquals(lawId1.value, lawId2.value)
    }
}
