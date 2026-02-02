package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger

class JsonMfPlayerRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var storageManager: JsonStorageManager
    private lateinit var repository: JsonMfPlayerRepository

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        storageManager = JsonStorageManager(plugin, tempDir.toString())
        repository = JsonMfPlayerRepository(plugin, storageManager)
    }

    @AfterEach
    fun cleanup() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `test upsert creates new player`() {
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        val player = MfPlayer(playerId, 0, "TestPlayer", 10.0, 10.0, false, null)

        val result = repository.upsert(player)

        assertEquals(playerId, result.id)
        assertEquals("TestPlayer", result.name)
        assertEquals(10.0, result.power)
        assertEquals(1, result.version) // Version should increment
    }

    @Test
    fun `test upsert updates existing player`() {
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        val player1 = MfPlayer(playerId, 0, "TestPlayer", 10.0, 10.0, false, null)

        repository.upsert(player1)
        val player2 = MfPlayer(playerId, 1, "UpdatedPlayer", 20.0, 20.0, false, null)
        val result = repository.upsert(player2)

        assertEquals(playerId, result.id)
        assertEquals("UpdatedPlayer", result.name)
        assertEquals(20.0, result.power)
        assertEquals(2, result.version)
    }

    @Test
    fun `test getPlayer returns existing player`() {
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        val player = MfPlayer(playerId, 0, "TestPlayer", 10.0, 10.0, false, null)

        repository.upsert(player)
        val result = repository.getPlayer(playerId)

        assertNotNull(result)
        assertEquals(playerId, result?.id)
        assertEquals("TestPlayer", result?.name)
    }

    @Test
    fun `test getPlayer returns null for non-existent player`() {
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        val result = repository.getPlayer(playerId)
        assertNull(result)
    }

    @Test
    fun `test getPlayers returns all players`() {
        val player1 = MfPlayer(MfPlayerId(UUID.randomUUID().toString()), 0, "Player1", 10.0, 10.0, false, null)
        val player2 = MfPlayer(MfPlayerId(UUID.randomUUID().toString()), 0, "Player2", 15.0, 15.0, false, null)

        repository.upsert(player1)
        repository.upsert(player2)

        val players = repository.getPlayers()

        assertEquals(2, players.size)
        assertTrue(players.any { it.name == "Player1" })
        assertTrue(players.any { it.name == "Player2" })
    }

    @Test
    fun `test getPlayers returns empty list when no players`() {
        val players = repository.getPlayers()
        assertTrue(players.isEmpty())
    }

    @Test
    fun `test upsert with zero power`() {
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        val player = MfPlayer(playerId, 0, "TestPlayer", 0.0, 0.0, false, null)

        val result = repository.upsert(player)

        assertEquals(0.0, result.power)
        assertEquals(0.0, result.powerAtLogout)
    }

    @Test
    fun `test upsert with high power values`() {
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        val player = MfPlayer(playerId, 0, "TestPlayer", 1000.0, 1000.0, false, null)

        val result = repository.upsert(player)

        assertEquals(1000.0, result.power)
    }

    @Test
    fun `test upsert with bypass enabled`() {
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        val player = MfPlayer(playerId, 0, "TestPlayer", 10.0, 10.0, true, null)

        val result = repository.upsert(player)

        assertTrue(result.isBypassEnabled)
    }

    @Test
    fun `test multiple concurrent upserts`() {
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        val threads = (1..10).map { index ->
            Thread {
                val player = MfPlayer(playerId, 0, "Player$index", index * 10.0, index * 10.0, false, null)
                repository.upsert(player)
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val result = repository.getPlayer(playerId)
        assertNotNull(result)
        // Should have some version incremented
        assertTrue(result!!.version > 0)
    }

    @Test
    fun `test player with special characters in name`() {
        val playerId = MfPlayerId(UUID.randomUUID())
        val player = MfPlayer(playerId, 0, "Player™_😀_Special", 10.0, 10.0, false, null)

        val result = repository.upsert(player)

        assertEquals("Player™_😀_Special", result.name)
    }
}
