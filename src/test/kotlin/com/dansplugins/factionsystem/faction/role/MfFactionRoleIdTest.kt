package com.dansplugins.factionsystem.faction.role

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MfFactionRoleIdTest {

    @Test
    fun testGenerate() {
        // execute
        val roleId = MfFactionRoleId.generate()

        // verify - UUID string format is 36 characters (e.g., "550e8400-e29b-41d4-a716-446655440000")
        assertEquals(36, roleId.value.length)
    }

    @Test
    fun testGenerateCreatesUniqueIds() {
        // execute
        val roleId1 = MfFactionRoleId.generate()
        val roleId2 = MfFactionRoleId.generate()

        // verify
        assertNotEquals(roleId1.value, roleId2.value)
    }

    @Test
    fun testConstructorWithValue() {
        // prepare
        val value = "test-role-id"

        // execute
        val roleId = MfFactionRoleId(value)

        // verify
        assertEquals(value, roleId.value)
    }

    @Test
    fun testEquality() {
        // prepare
        val value = "test-role-id"

        // execute
        val roleId1 = MfFactionRoleId(value)
        val roleId2 = MfFactionRoleId(value)

        // verify
        assertEquals(roleId1, roleId2)
        assertEquals(roleId1.value, roleId2.value)
    }
}
