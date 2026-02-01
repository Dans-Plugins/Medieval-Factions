package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.ChatChannelMessage
import com.dansplugins.factionsystem.chat.ChatChannelMessageId
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayerId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsonMfChatChannelMessageRepositoryTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfChatChannelMessageRepository
    
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        val config = mock(org.bukkit.configuration.file.FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getString("storage.json.path")).thenReturn(tempDir.toString())
        
        repository = JsonMfChatChannelMessageRepository(plugin)
    }

    @AfterEach
    fun cleanup() {
        tempDir.toFile().listFiles()?.forEach { it.deleteRecursively() }
    }

    @Test
    fun testInsert_NewMessage() {
        // prepare
        val messageId = ChatChannelMessageId.generate()
        val factionId = MfFactionId.generate()
        val playerId = MfPlayerId("test-player")
        val message = ChatChannelMessage(messageId, 1, factionId, playerId, "Hello world!", Instant.now())

        // execute
        repository.insert(message)
        val result = repository.getMessage(messageId)

        // verify
        assertNotNull(result)
        assertEquals(messageId, result.id)
        assertEquals("Hello world!", result.message)
        assertEquals(playerId, result.playerId)
        assertEquals(factionId, result.factionId)
    }

    @Test
    fun testGetAllMessages_Empty() {
        // execute
        val result = repository.getAllMessages()

        // verify
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun testGetAllMessages_MultipleFactions() {
        // prepare
        val faction1 = MfFactionId.generate()
        val faction2 = MfFactionId.generate()
        val playerId = MfPlayerId("player-1")
        
        val message1 = ChatChannelMessage(ChatChannelMessageId.generate(), 1, faction1, playerId, "Message 1", Instant.now())
        val message2 = ChatChannelMessage(ChatChannelMessageId.generate(), 1, faction2, playerId, "Message 2", Instant.now())
        val message3 = ChatChannelMessage(ChatChannelMessageId.generate(), 1, faction1, playerId, "Message 3", Instant.now())

        repository.insert(message1)
        repository.insert(message2)
        repository.insert(message3)

        // execute
        val result = repository.getAllMessages()

        // verify
        assertEquals(3, result.size)
    }

    @Test
    fun testMessageLimit_EnforcesMaxPerFaction() {
        // prepare
        val factionId = MfFactionId.generate()
        val playerId = MfPlayerId("player-1")
        
        // Insert 1100 messages (should keep only last 1000)
        for (i in 1..1100) {
            val message = ChatChannelMessage(
                ChatChannelMessageId.generate(),
                1,
                factionId,
                playerId,
                "Message $i",
                Instant.now().plusMillis(i.toLong())
            )
            repository.insert(message)
        }

        // execute
        val allMessages = repository.getAllMessages()
        val factionMessages = allMessages.filter { it.factionId == factionId }

        // verify - should have exactly 1000 messages
        assertEquals(1000, factionMessages.size)
        
        // verify - should have the most recent messages (101-1100)
        val messageTexts = factionMessages.map { it.message }.toSet()
        assertTrue(messageTexts.contains("Message 1100"))
        assertTrue(messageTexts.contains("Message 101"))
        assertTrue(!messageTexts.contains("Message 1")) // oldest should be removed
        assertTrue(!messageTexts.contains("Message 100")) // oldest should be removed
    }

    @Test
    fun testMessageLimit_DifferentFactions() {
        // prepare
        val faction1 = MfFactionId.generate()
        val faction2 = MfFactionId.generate()
        val playerId = MfPlayerId("player-1")
        
        // Insert 600 messages for faction1
        for (i in 1..600) {
            val message = ChatChannelMessage(
                ChatChannelMessageId.generate(),
                1,
                faction1,
                playerId,
                "Faction1 Message $i",
                Instant.now().plusMillis(i.toLong())
            )
            repository.insert(message)
        }
        
        // Insert 600 messages for faction2
        for (i in 1..600) {
            val message = ChatChannelMessage(
                ChatChannelMessageId.generate(),
                1,
                faction2,
                playerId,
                "Faction2 Message $i",
                Instant.now().plusMillis(i.toLong())
            )
            repository.insert(message)
        }

        // execute
        val allMessages = repository.getAllMessages()
        val faction1Messages = allMessages.filter { it.factionId == faction1 }
        val faction2Messages = allMessages.filter { it.factionId == faction2 }

        // verify - each faction should have 600 messages (under the 1000 limit)
        assertEquals(600, faction1Messages.size)
        assertEquals(600, faction2Messages.size)
        assertEquals(1200, allMessages.size)
    }

    @Test
    fun testDelete_ExistingMessage() {
        // prepare
        val messageId = ChatChannelMessageId.generate()
        val factionId = MfFactionId.generate()
        val playerId = MfPlayerId("player-1")
        val message = ChatChannelMessage(messageId, 1, factionId, playerId, "Test message", Instant.now())
        repository.insert(message)

        // execute
        repository.delete(messageId)
        val result = repository.getMessage(messageId)

        // verify
        assertEquals(null, result)
    }

    @Test
    fun testMessagesWithSpecialCharacters() {
        // prepare
        val messageId = ChatChannelMessageId.generate()
        val factionId = MfFactionId.generate()
        val playerId = MfPlayerId("player-1")
        val specialText = "Special chars: @#$% 你好 🎉 \"quotes\" 'apostrophes' <html>"
        val message = ChatChannelMessage(messageId, 1, factionId, playerId, specialText, Instant.now())

        // execute
        repository.insert(message)
        val result = repository.getMessage(messageId)

        // verify
        assertNotNull(result)
        assertEquals(specialText, result.message)
    }

    @Test
    fun testMessagesOrderedByTimestamp() {
        // prepare
        val factionId = MfFactionId.generate()
        val playerId = MfPlayerId("player-1")
        val now = Instant.now()
        
        val message1 = ChatChannelMessage(ChatChannelMessageId.generate(), 1, factionId, playerId, "First", now.minusSeconds(10))
        val message2 = ChatChannelMessage(ChatChannelMessageId.generate(), 1, factionId, playerId, "Second", now.minusSeconds(5))
        val message3 = ChatChannelMessage(ChatChannelMessageId.generate(), 1, factionId, playerId, "Third", now)

        // Insert in random order
        repository.insert(message2)
        repository.insert(message1)
        repository.insert(message3)

        // execute
        val allMessages = repository.getAllMessages()
        val factionMessages = allMessages.filter { it.factionId == factionId }
            .sortedBy { it.timestamp }

        // verify - messages should be retrievable and sortable by timestamp
        assertEquals("First", factionMessages[0].message)
        assertEquals("Second", factionMessages[1].message)
        assertEquals("Third", factionMessages[2].message)
    }

    @Test
    fun testEmptyMessage() {
        // prepare
        val messageId = ChatChannelMessageId.generate()
        val factionId = MfFactionId.generate()
        val playerId = MfPlayerId("player-1")
        val message = ChatChannelMessage(messageId, 1, factionId, playerId, "", Instant.now())

        // execute
        repository.insert(message)
        val result = repository.getMessage(messageId)

        // verify
        assertNotNull(result)
        assertEquals("", result.message)
    }

    @Test
    fun testVeryLongMessage() {
        // prepare
        val messageId = ChatChannelMessageId.generate()
        val factionId = MfFactionId.generate()
        val playerId = MfPlayerId("player-1")
        val longMessage = "A".repeat(10000)
        val message = ChatChannelMessage(messageId, 1, factionId, playerId, longMessage, Instant.now())

        // execute
        repository.insert(message)
        val result = repository.getMessage(messageId)

        // verify
        assertNotNull(result)
        assertEquals(10000, result.message.length)
    }
}
