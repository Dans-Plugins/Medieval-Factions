package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import org.bukkit.configuration.file.FileConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        // Serve the real schema resource so that schema validation actually runs. Leaving getResource
        // mocked away makes loadSchemaFromResource throw, which silently disables validation and lets
        // mismatches between the schema and the serialized shape go unnoticed.
        `when`(plugin.getResource("schemas/players.json"))
            .thenAnswer { java.io.File("src/main/resources/schemas/players.json").inputStream() }
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
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        val player = MfPlayer(playerId, 0, "Player™_😀_Special", 10.0, 10.0, false, null)

        val result = repository.upsert(player)

        assertEquals("Player™_😀_Special", result.name)
    }

    @Test
    fun `a players file written before fixed power existed loads with no fixed power`() {
        // The shape of players.json as written by releases before #1682: no fixedPower field.
        val playerId = UUID.randomUUID().toString()
        tempDir.resolve("players.json").toFile().writeText(
            """
            {
              "players": [
                {
                  "id": "$playerId",
                  "version": 3,
                  "name": "OldTimer",
                  "power": 7.5,
                  "powerAtLogout": 6.0,
                  "isBypassEnabled": false
                }
              ]
            }
            """.trimIndent()
        )

        val loaded = repository.getPlayer(MfPlayerId(playerId))

        assertNotNull(loaded)
        assertNull(loaded!!.fixedPower)
        assertFalse(loaded.isPowerFixed)
        assertEquals(7.5, loaded.power)
        assertEquals(3, loaded.version)
        // And it saves again without tripping the schema.
        val saved = repository.upsert(loaded.copy(power = 8.0))
        assertEquals(8.0, saved.power)
        assertNull(repository.getPlayer(MfPlayerId(playerId))!!.fixedPower)
    }

    @Test
    fun `an unfixed player is saved without a fixedPower field`() {
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        repository.upsert(MfPlayer(playerId, 0, "TestPlayer", 10.0, 10.0, false, null))

        val json = tempDir.resolve("players.json").toFile().readText()

        assertFalse(json.contains("fixedPower"), "an unfixed player must keep the old file shape, was: $json")
    }

    @Test
    fun `fixed power round trips`() {
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        repository.upsert(MfPlayer(playerId, power = 12.0, powerAtLogout = 12.0, fixedPower = 12.0))

        val loaded = repository.getPlayer(playerId)!!

        assertEquals(12.0, loaded.fixedPower)
        assertTrue(loaded.isPowerFixed)

        val cleared = repository.upsert(loaded.copy(fixedPower = null))
        assertNull(cleared.fixedPower)
        assertNull(repository.getPlayer(playerId)!!.fixedPower)
    }

    @Test
    fun `the power cycle skips players whose power is fixed`() {
        val config = mock(FileConfiguration::class.java)
        `when`(config.getDouble("players.minPower")).thenReturn(0.0)
        `when`(config.getDouble("players.maxPower")).thenReturn(20.0)
        `when`(config.getDouble("players.hoursToReachMaxPower")).thenReturn(12.0)
        `when`(config.getDouble("players.hoursToReachMinPower")).thenReturn(72.0)
        `when`(plugin.config).thenReturn(config)
        val fixedOnline = repository.upsert(MfPlayer(MfPlayerId(UUID.randomUUID().toString()), power = 5.0, powerAtLogout = 5.0, fixedPower = 5.0))
        val normalOnline = repository.upsert(MfPlayer(MfPlayerId(UUID.randomUUID().toString()), power = 5.0, powerAtLogout = 5.0))
        val fixedOffline = repository.upsert(MfPlayer(MfPlayerId(UUID.randomUUID().toString()), power = 15.0, powerAtLogout = 15.0, fixedPower = 15.0))
        val normalOffline = repository.upsert(MfPlayer(MfPlayerId(UUID.randomUUID().toString()), power = 15.0, powerAtLogout = 15.0))

        repository.increaseOnlinePlayerPower(listOf(fixedOnline.id, normalOnline.id))
        repository.decreaseOfflinePlayerPower(listOf(fixedOffline.id, normalOffline.id))

        assertEquals(fixedOnline, repository.getPlayer(fixedOnline.id), "a fixed player must not gain power or be touched")
        assertEquals(fixedOffline, repository.getPlayer(fixedOffline.id), "a fixed player must not lose power or be touched")
        assertTrue(repository.getPlayer(normalOnline.id)!!.power > 5.0, "an unfixed online player still gains power")
        assertTrue(repository.getPlayer(normalOffline.id)!!.power < 15.0, "an unfixed offline player still loses power")
    }
}
