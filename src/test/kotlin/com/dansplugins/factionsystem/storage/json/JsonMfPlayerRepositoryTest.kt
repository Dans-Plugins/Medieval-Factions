package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JsonMfPlayerRepositoryTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfPlayerRepository
    
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        val config = mock(org.bukkit.configuration.file.FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getString("storage.json.path")).thenReturn(tempDir.toString())
        
        repository = JsonMfPlayerRepository(plugin)
    }

    @AfterEach
    fun cleanup() {
        tempDir.toFile().listFiles()?.forEach { it.deleteRecursively() }
    }

    @Test
    fun testUpsert_NewPlayer() {
        // prepare
        val playerId = MfPlayerId("test-player-1")
        val player = MfPlayer(playerId, 10.0, 10.0, 5)

        // execute
        repository.upsert(player)
        val result = repository.getPlayer(playerId)

        // verify
        assertNotNull(result)
        assertEquals(playerId, result.id)
        assertEquals(10.0, result.power)
        assertEquals(10.0, result.powerAtLogout)
        assertEquals(5, result.minutesOffline)
    }

    @Test
    fun testUpsert_UpdateExistingPlayer() {
        // prepare
        val playerId = MfPlayerId("test-player-2")
        val player1 = MfPlayer(playerId, 10.0, 10.0, 0)
        repository.upsert(player1)

        // execute
        val player2 = MfPlayer(playerId, 15.0, 12.0, 3, version = 2)
        repository.upsert(player2)
        val result = repository.getPlayer(playerId)

        // verify
        assertNotNull(result)
        assertEquals(15.0, result.power)
        assertEquals(12.0, result.powerAtLogout)
        assertEquals(3, result.minutesOffline)
        assertEquals(2, result.version)
    }

    @Test
    fun testGetPlayer_NonExistent() {
        // prepare
        val playerId = MfPlayerId("nonexistent-player")

        // execute
        val result = repository.getPlayer(playerId)

        // verify
        assertNull(result)
    }

    @Test
    fun testGetAllPlayers_Empty() {
        // execute
        val result = repository.getAllPlayers()

        // verify
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun testGetAllPlayers_MultiplePlayers() {
        // prepare
        val player1 = MfPlayer(MfPlayerId("player-1"), 10.0, 10.0, 0)
        val player2 = MfPlayer(MfPlayerId("player-2"), 15.0, 15.0, 0)
        val player3 = MfPlayer(MfPlayerId("player-3"), 20.0, 20.0, 0)
        repository.upsert(player1)
        repository.upsert(player2)
        repository.upsert(player3)

        // execute
        val result = repository.getAllPlayers()

        // verify
        assertEquals(3, result.size)
        val playerIds = result.map { it.id.value }.toSet()
        assert(playerIds.contains("player-1"))
        assert(playerIds.contains("player-2"))
        assert(playerIds.contains("player-3"))
    }

    @Test
    fun testDelete_ExistingPlayer() {
        // prepare
        val playerId = MfPlayerId("player-to-delete")
        val player = MfPlayer(playerId, 10.0, 10.0, 0)
        repository.upsert(player)

        // execute
        repository.delete(playerId)
        val result = repository.getPlayer(playerId)

        // verify
        assertNull(result)
    }

    @Test
    fun testDelete_NonExistentPlayer() {
        // prepare
        val playerId = MfPlayerId("nonexistent")

        // execute - should not throw exception
        repository.delete(playerId)
        val result = repository.getPlayer(playerId)

        // verify
        assertNull(result)
    }

    @Test
    fun testPlayerWithZeroPower() {
        // prepare
        val playerId = MfPlayerId("zero-power-player")
        val player = MfPlayer(playerId, 0.0, 0.0, 10)

        // execute
        repository.upsert(player)
        val result = repository.getPlayer(playerId)

        // verify
        assertNotNull(result)
        assertEquals(0.0, result.power)
        assertEquals(0.0, result.powerAtLogout)
    }

    @Test
    fun testPlayerWithNegativePower() {
        // prepare
        val playerId = MfPlayerId("negative-power-player")
        val player = MfPlayer(playerId, -5.0, -5.0, 0)

        // execute
        repository.upsert(player)
        val result = repository.getPlayer(playerId)

        // verify
        assertNotNull(result)
        assertEquals(-5.0, result.power)
        assertEquals(-5.0, result.powerAtLogout)
    }

    @Test
    fun testPlayerWithHighMinutesOffline() {
        // prepare
        val playerId = MfPlayerId("long-offline-player")
        val player = MfPlayer(playerId, 10.0, 10.0, 100000)

        // execute
        repository.upsert(player)
        val result = repository.getPlayer(playerId)

        // verify
        assertNotNull(result)
        assertEquals(100000, result.minutesOffline)
    }

    @Test
    fun testVersionIncrement() {
        // prepare
        val playerId = MfPlayerId("versioned-player")
        val player = MfPlayer(playerId, 10.0, 10.0, 0, version = 1)
        repository.upsert(player)

        // execute
        val updatedPlayer = player.copy(power = 12.0, version = 2)
        repository.upsert(updatedPlayer)
        val result = repository.getPlayer(playerId)

        // verify
        assertNotNull(result)
        assertEquals(2, result.version)
        assertEquals(12.0, result.power)
    }

    @Test
    fun testPlayerIdWithSpecialCharacters() {
        // prepare
        val playerId = MfPlayerId("player-with-特殊-chars-🎉")
        val player = MfPlayer(playerId, 10.0, 10.0, 0)

        // execute
        repository.upsert(player)
        val result = repository.getPlayer(playerId)

        // verify
        assertNotNull(result)
        assertEquals(playerId.value, result.id.value)
    }

    @Test
    fun testConcurrentOperations() {
        // prepare
        val players = List(50) { index ->
            MfPlayer(MfPlayerId("concurrent-player-$index"), 10.0 + index, 10.0, 0)
        }

        // execute - simulate concurrent writes
        val threads = players.map { player ->
            Thread {
                repository.upsert(player)
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // verify
        val allPlayers = repository.getAllPlayers()
        assertEquals(50, allPlayers.size)
    }
}
