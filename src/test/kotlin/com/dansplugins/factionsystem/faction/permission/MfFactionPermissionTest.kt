package com.dansplugins.factionsystem.faction.permission

import com.dansplugins.factionsystem.faction.MfFaction
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MfFactionPermissionTest {

    @Test
    fun testInitializationWithTranslateFunction() {
        // prepare
        val name = "test.permission"
        val translateFn: (MfFaction) -> String = { "Test Permission" }
        val default = true

        // execute
        val permission = MfFactionPermission(name, translateFn, default)

        // verify
        assertEquals(name, permission.name)
        assertEquals(default, permission.default)
    }

    @Test
    fun testInitializationWithStaticTranslation() {
        // prepare
        val name = "test.permission"
        val translation = "Static Translation"
        val default = false

        // execute
        val permission = MfFactionPermission(name, translation, default)

        // verify
        assertEquals(name, permission.name)
        assertEquals(default, permission.default)
    }

    @Test
    fun testTranslateFunction() {
        // prepare
        val name = "test.permission"
        val mockFaction = mock(MfFaction::class.java)
        val translateFn: (MfFaction) -> String = { faction -> "Permission for faction" }

        // execute
        val permission = MfFactionPermission(name, translateFn, true)
        val translation = permission.translate(mockFaction)

        // verify
        assertEquals("Permission for faction", translation)
    }

    @Test
    fun testStaticTranslate() {
        // prepare
        val name = "test.permission"
        val translation = "Static Permission Text"
        val mockFaction = mock(MfFaction::class.java)

        // execute
        val permission = MfFactionPermission(name, translation, true)
        val result = permission.translate(mockFaction)

        // verify
        assertEquals(translation, result)
    }

    @Test
    fun testDefaultTrue() {
        // prepare
        val permission = MfFactionPermission("test.permission", "Test", true)

        // verify
        assertTrue(permission.default)
    }

    @Test
    fun testDefaultFalse() {
        // prepare
        val permission = MfFactionPermission("test.permission", "Test", false)

        // verify
        assertFalse(permission.default)
    }

    @Test
    fun testToString() {
        // prepare
        val name = "test.permission"
        val permission = MfFactionPermission(name, "Test", true)

        // execute
        val result = permission.toString()

        // verify
        assertEquals(name, result)
    }

    @Test
    fun testEquality() {
        // prepare
        val name = "test.permission"

        // execute
        val permission1 = MfFactionPermission(name, "Translation 1", true)
        val permission2 = MfFactionPermission(name, "Translation 2", false)

        // verify - permissions are equal if names match, regardless of translation or default
        assertEquals(permission1, permission2)
    }

    @Test
    fun testInequality() {
        // prepare
        val permission1 = MfFactionPermission("test.permission1", "Test 1", true)
        val permission2 = MfFactionPermission("test.permission2", "Test 2", true)

        // verify
        assertNotEquals(permission1, permission2)
    }

    @Test
    fun testHashCodeEquality() {
        // prepare
        val name = "test.permission"
        val permission1 = MfFactionPermission(name, "Translation 1", true)
        val permission2 = MfFactionPermission(name, "Translation 2", false)

        // verify
        assertEquals(permission1.hashCode(), permission2.hashCode())
    }

    @Test
    fun testHashCodeInequality() {
        // prepare
        val permission1 = MfFactionPermission("test.permission1", "Test 1", true)
        val permission2 = MfFactionPermission("test.permission2", "Test 2", true)

        // verify
        assertNotEquals(permission1.hashCode(), permission2.hashCode())
    }

    @Test
    fun testSameNameWithDifferentDefaults() {
        // prepare
        val name = "test.permission"

        // execute
        val permissionTrue = MfFactionPermission(name, "Test", true)
        val permissionFalse = MfFactionPermission(name, "Test", false)

        // verify - equality based on name only
        assertEquals(permissionTrue, permissionFalse)
        assertTrue(permissionTrue.default)
        assertFalse(permissionFalse.default)
    }
}
