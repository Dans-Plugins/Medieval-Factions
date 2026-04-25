package com.dansplugins.factionsystem.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MfFactionChatChannelTest {

    @Test
    fun testFactionChannel() {
        // execute
        val channel = MfFactionChatChannel.FACTION

        // verify
        assertEquals("FACTION", channel.name)
    }

    @Test
    fun testVassalsChannel() {
        // execute
        val channel = MfFactionChatChannel.VASSALS

        // verify
        assertEquals("VASSALS", channel.name)
    }

    @Test
    fun testAlliesChannel() {
        // execute
        val channel = MfFactionChatChannel.ALLIES

        // verify
        assertEquals("ALLIES", channel.name)
    }

    @Test
    fun testAllChannelsPresent() {
        // execute
        val channels = MfFactionChatChannel.values()

        // verify - ensure all 3 channels exist
        assertEquals(3, channels.size)
        assertTrue(channels.contains(MfFactionChatChannel.FACTION))
        assertTrue(channels.contains(MfFactionChatChannel.VASSALS))
        assertTrue(channels.contains(MfFactionChatChannel.ALLIES))
    }

    @Test
    fun testValueOf() {
        // execute & verify
        assertEquals(MfFactionChatChannel.FACTION, MfFactionChatChannel.valueOf("FACTION"))
        assertEquals(MfFactionChatChannel.VASSALS, MfFactionChatChannel.valueOf("VASSALS"))
        assertEquals(MfFactionChatChannel.ALLIES, MfFactionChatChannel.valueOf("ALLIES"))
    }

    @Test
    fun testEnumOrdinal() {
        // execute & verify - test ordering
        assertEquals(0, MfFactionChatChannel.FACTION.ordinal)
        assertEquals(1, MfFactionChatChannel.VASSALS.ordinal)
        assertEquals(2, MfFactionChatChannel.ALLIES.ordinal)
    }
}
