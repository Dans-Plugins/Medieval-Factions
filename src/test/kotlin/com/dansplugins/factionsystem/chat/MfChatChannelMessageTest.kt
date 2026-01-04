package com.dansplugins.factionsystem.chat

import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayerId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MfChatChannelMessageTest {
    private val testUtils = TestUtils()

    @Test
    fun testInitialization() {
        // prepare
        val timestamp = testUtils.createTimestamp()
        val playerId = testUtils.createPlayerId("test-player")
        val factionId = testUtils.createFactionId()
        val chatChannel = MfFactionChatChannel.FACTION
        val message = "Hello, faction!"

        // execute
        val chatMessage = MfChatChannelMessage(timestamp, playerId, factionId, chatChannel, message)

        // verify
        assertEquals(timestamp, chatMessage.timestamp)
        assertEquals(playerId, chatMessage.playerId)
        assertEquals(factionId, chatMessage.factionId)
        assertEquals(chatChannel, chatMessage.chatChannel)
        assertEquals(message, chatMessage.message)
    }

    @Test
    fun testFactionChannelMessage() {
        // prepare
        val timestamp = testUtils.createTimestamp()
        val playerId = testUtils.createPlayerId("player1")
        val factionId = testUtils.createFactionId()
        val message = "Testing faction chat"

        // execute
        val chatMessage = MfChatChannelMessage(timestamp, playerId, factionId, MfFactionChatChannel.FACTION, message)

        // verify
        assertEquals(MfFactionChatChannel.FACTION, chatMessage.chatChannel)
    }

    @Test
    fun testVassalsChannelMessage() {
        // prepare
        val timestamp = testUtils.createTimestamp()
        val playerId = testUtils.createPlayerId("player2")
        val factionId = testUtils.createFactionId()
        val message = "Vassals message"

        // execute
        val chatMessage = MfChatChannelMessage(timestamp, playerId, factionId, MfFactionChatChannel.VASSALS, message)

        // verify
        assertEquals(MfFactionChatChannel.VASSALS, chatMessage.chatChannel)
    }

    @Test
    fun testAlliesChannelMessage() {
        // prepare
        val timestamp = testUtils.createTimestamp()
        val playerId = testUtils.createPlayerId("player3")
        val factionId = testUtils.createFactionId()
        val message = "Allies message"

        // execute
        val chatMessage = MfChatChannelMessage(timestamp, playerId, factionId, MfFactionChatChannel.ALLIES, message)

        // verify
        assertEquals(MfFactionChatChannel.ALLIES, chatMessage.chatChannel)
    }

    @Test
    fun testEmptyMessage() {
        // prepare
        val timestamp = testUtils.createTimestamp()
        val playerId = testUtils.createPlayerId("player")
        val factionId = testUtils.createFactionId()
        val message = ""

        // execute
        val chatMessage = MfChatChannelMessage(timestamp, playerId, factionId, MfFactionChatChannel.FACTION, message)

        // verify
        assertEquals("", chatMessage.message)
    }

    @Test
    fun testLongMessage() {
        // prepare
        val timestamp = testUtils.createTimestamp()
        val playerId = testUtils.createPlayerId("player")
        val factionId = testUtils.createFactionId()
        val longMessageLength = 1000 // Test with a long message to verify no length restrictions
        val message = "A".repeat(longMessageLength)

        // execute
        val chatMessage = MfChatChannelMessage(timestamp, playerId, factionId, MfFactionChatChannel.FACTION, message)

        // verify
        assertEquals(longMessageLength, chatMessage.message.length)
    }

    @Test
    fun testTimestampOrdering() {
        // prepare
        val playerId = testUtils.createPlayerId("player")
        val factionId = testUtils.createFactionId()
        val earlier = testUtils.createTimestamp(1000)
        val later = testUtils.createTimestamp(2000)

        // execute
        val message1 = MfChatChannelMessage(earlier, playerId, factionId, MfFactionChatChannel.FACTION, "First")
        val message2 = MfChatChannelMessage(later, playerId, factionId, MfFactionChatChannel.FACTION, "Second")

        // verify
        assertTrue(message1.timestamp.isBefore(message2.timestamp))
    }

    @Test
    fun testMultipleMessagesFromSamePlayer() {
        // prepare
        val playerId = testUtils.createPlayerId("player")
        val factionId = testUtils.createFactionId()
        val timestamp1 = testUtils.createTimestamp()
        val timestamp2 = timestamp1.plusSeconds(1)

        // execute
        val message1 = MfChatChannelMessage(timestamp1, playerId, factionId, MfFactionChatChannel.FACTION, "Message 1")
        val message2 = MfChatChannelMessage(timestamp2, playerId, factionId, MfFactionChatChannel.FACTION, "Message 2")

        // verify
        assertEquals(playerId, message1.playerId)
        assertEquals(playerId, message2.playerId)
        assertEquals("Message 1", message1.message)
        assertEquals("Message 2", message2.message)
    }

    @Test
    fun testMessageWithSpecialCharacters() {
        // prepare
        val timestamp = testUtils.createTimestamp()
        val playerId = testUtils.createPlayerId("player")
        val factionId = testUtils.createFactionId()
        val message = "Hello! @#$%^&*() 你好 🎉"

        // execute
        val chatMessage = MfChatChannelMessage(timestamp, playerId, factionId, MfFactionChatChannel.FACTION, message)

        // verify
        assertEquals(message, chatMessage.message)
    }
}
