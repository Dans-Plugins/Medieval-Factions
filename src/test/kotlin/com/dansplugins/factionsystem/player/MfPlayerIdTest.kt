package com.dansplugins.factionsystem.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MfPlayerIdTest {

    @Test
    fun testConstructorWithValue() {
        // prepare
        val value = "test-player-id"

        // execute
        val playerId = MfPlayerId(value)

        // verify
        assertEquals(value, playerId.value)
    }

    @Test
    fun testEquality() {
        // prepare
        val value = "test-player-id"

        // execute
        val playerId1 = MfPlayerId(value)
        val playerId2 = MfPlayerId(value)

        // verify
        assertEquals(playerId1, playerId2)
        assertEquals(playerId1.value, playerId2.value)
    }

    @Test
    fun testInequality() {
        // prepare
        val value1 = "player-1"
        val value2 = "player-2"

        // execute
        val playerId1 = MfPlayerId(value1)
        val playerId2 = MfPlayerId(value2)

        // verify
        assertNotEquals(playerId1, playerId2)
        assertNotEquals(playerId1.value, playerId2.value)
    }

    @Test
    fun testUUIDFormat() {
        // prepare
        val uuid = "550e8400-e29b-41d4-a716-446655440000"

        // execute
        val playerId = MfPlayerId(uuid)

        // verify
        assertEquals(uuid, playerId.value)
    }

    @Test
    fun testHashCode() {
        // prepare
        val value = "test-player-id"

        // execute
        val playerId1 = MfPlayerId(value)
        val playerId2 = MfPlayerId(value)

        // verify
        assertEquals(playerId1.hashCode(), playerId2.hashCode())
    }
}
