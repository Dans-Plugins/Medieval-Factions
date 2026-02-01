package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfChatChannelMessage
import com.dansplugins.factionsystem.chat.MfFactionChatChannel
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayerId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger

class JsonMfChatChannelMessageRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var storageManager: JsonStorageManager
    private lateinit var repository: JsonMfChatChannelMessageRepository

    @BeforeEach
    fun setup() {
        plugin = mockk<MedievalFactions>(relaxed = true)
        every { plugin.logger } returns Logger.getLogger("TestLogger")
        every { plugin.dataFolder } returns tempDir.toFile()
        storageManager = JsonStorageManager(plugin, tempDir.toString())
        repository = JsonMfChatChannelMessageRepository(plugin, storageManager)
    }

    @AfterEach
    fun cleanup() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `test insert and retrieve chat message`() {
        val factionId = MfFactionId(UUID.randomUUID())
        val playerId = MfPlayerId(UUID.randomUUID())
        val message = MfChatChannelMessage(
            Instant.now(),
            playerId,
            factionId,
            MfFactionChatChannel.FACTION,
            "Test message"
        )

        repository.insert(message)
        val messages = repository.getChatChannelMessages(factionId)

        assertEquals(1, messages.size)
        assertEquals("Test message", messages[0].message)
        assertEquals(playerId, messages[0].playerId)
    }

    @Test
    fun `test getChatChannelMessages returns empty list for unknown faction`() {
        val factionId = MfFactionId(UUID.randomUUID())
        val messages = repository.getChatChannelMessages(factionId)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `test insert multiple messages for same faction`() {
        val factionId = MfFactionId(UUID.randomUUID())
        val playerId = MfPlayerId(UUID.randomUUID())

        for (i in 1..5) {
            val message = MfChatChannelMessage(
                Instant.now(),
                playerId,
                factionId,
                MfFactionChatChannel.FACTION,
                "Message $i"
            )
            repository.insert(message)
        }

        val messages = repository.getChatChannelMessages(factionId)
        assertEquals(5, messages.size)
    }

    @Test
    fun `test message limit enforcement - keeps only 1000 messages per faction`() {
        val factionId = MfFactionId(UUID.randomUUID())
        val playerId = MfPlayerId(UUID.randomUUID())

        // Insert 1050 messages
        for (i in 1..1050) {
            val message = MfChatChannelMessage(
                Instant.now().plusMillis(i.toLong()),
                playerId,
                factionId,
                MfFactionChatChannel.FACTION,
                "Message $i"
            )
            repository.insert(message)
        }

        val messages = repository.getChatChannelMessages(factionId)

        // Should only keep the most recent 1000 messages
        assertEquals(1000, messages.size)

        // The oldest messages (1-50) should be gone
        assertFalse(messages.any { it.message == "Message 1" })
        assertFalse(messages.any { it.message == "Message 50" })

        // The newest messages should be present
        assertTrue(messages.any { it.message == "Message 1050" })
        assertTrue(messages.any { it.message == "Message 1000" })
    }

    @Test
    fun `test multiple factions have independent message limits`() {
        val faction1 = MfFactionId(UUID.randomUUID())
        val faction2 = MfFactionId(UUID.randomUUID())
        val playerId = MfPlayerId(UUID.randomUUID())

        // Insert 600 messages for faction1
        for (i in 1..600) {
            repository.insert(
                MfChatChannelMessage(
                    Instant.now(),
                    playerId,
                    faction1,
                    MfFactionChatChannel.FACTION,
                    "Faction1 Message $i"
                )
            )
        }

        // Insert 400 messages for faction2
        for (i in 1..400) {
            repository.insert(
                MfChatChannelMessage(
                    Instant.now(),
                    playerId,
                    faction2,
                    MfFactionChatChannel.FACTION,
                    "Faction2 Message $i"
                )
            )
        }

        val faction1Messages = repository.getChatChannelMessages(faction1)
        val faction2Messages = repository.getChatChannelMessages(faction2)

        assertEquals(600, faction1Messages.size)
        assertEquals(400, faction2Messages.size)
    }

    @Test
    fun `test getChatChannelMessageCount returns correct count`() {
        val factionId = MfFactionId(UUID.randomUUID())
        val playerId = MfPlayerId(UUID.randomUUID())

        for (i in 1..10) {
            repository.insert(
                MfChatChannelMessage(
                    Instant.now(),
                    playerId,
                    factionId,
                    MfFactionChatChannel.FACTION,
                    "Message $i"
                )
            )
        }

        val count = repository.getChatChannelMessageCount(factionId)
        assertEquals(10, count)
    }

    @Test
    fun `test getChatChannelMessages with limit and offset`() {
        val factionId = MfFactionId(UUID.randomUUID())
        val playerId = MfPlayerId(UUID.randomUUID())

        // Insert 20 messages
        for (i in 1..20) {
            repository.insert(
                MfChatChannelMessage(
                    Instant.now().plusMillis(i.toLong()),
                    playerId,
                    factionId,
                    MfFactionChatChannel.FACTION,
                    "Message $i"
                )
            )
        }

        // Get messages with limit and offset
        val messages = repository.getChatChannelMessages(factionId, limit = 5, offset = 5)

        assertEquals(5, messages.size)
    }

    @Test
    fun `test messages with special characters`() {
        val factionId = MfFactionId(UUID.randomUUID())
        val playerId = MfPlayerId(UUID.randomUUID())
        val specialMessage = "Hello™ 世界 😀 \"quoted\" 'text' <html>"

        repository.insert(
            MfChatChannelMessage(
                Instant.now(),
                playerId,
                factionId,
                MfFactionChatChannel.FACTION,
                specialMessage
            )
        )

        val messages = repository.getChatChannelMessages(factionId)
        assertEquals(1, messages.size)
        assertEquals(specialMessage, messages[0].message)
    }

    @Test
    fun `test empty message string`() {
        val factionId = MfFactionId(UUID.randomUUID())
        val playerId = MfPlayerId(UUID.randomUUID())

        repository.insert(
            MfChatChannelMessage(
                Instant.now(),
                playerId,
                factionId,
                MfFactionChatChannel.FACTION,
                ""
            )
        )

        val messages = repository.getChatChannelMessages(factionId)
        assertEquals(1, messages.size)
        assertEquals("", messages[0].message)
    }

    @Test
    fun `test very long message`() {
        val factionId = MfFactionId(UUID.randomUUID())
        val playerId = MfPlayerId(UUID.randomUUID())
        val longMessage = "x".repeat(10000)

        repository.insert(
            MfChatChannelMessage(
                Instant.now(),
                playerId,
                factionId,
                MfFactionChatChannel.FACTION,
                longMessage
            )
        )

        val messages = repository.getChatChannelMessages(factionId)
        assertEquals(1, messages.size)
        assertEquals(10000, messages[0].message.length)
    }
}
