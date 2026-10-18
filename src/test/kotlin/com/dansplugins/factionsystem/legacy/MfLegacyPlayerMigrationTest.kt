package com.dansplugins.factionsystem.legacy

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import com.google.gson.Gson
import dev.forkhandles.result4k.Success
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.File
import java.nio.file.Files
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * MF4 power records are migrated through [MfPlayerService.update], so a player row that already exists (an earlier
 * migration that stopped part-way and is run again) gets the migrated power instead of the save being refused as stale.
 */
class MfLegacyPlayerMigrationTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var bukkitPlayer: OfflinePlayer
    private val uuid = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e")
    private val gson = Gson()

    @BeforeTest
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("MfLegacyPlayerMigrationTest"))
        val config = mock(FileConfiguration::class.java)
        `when`(config.getDouble("players.initialPower")).thenReturn(5.0)
        `when`(plugin.config).thenReturn(config)
        playerService = mock(MfPlayerService::class.java)
        val services = mock(Services::class.java)
        `when`(services.playerService).thenReturn(playerService)
        `when`(plugin.services).thenReturn(services)
        bukkitPlayer = mock(OfflinePlayer::class.java)
        `when`(bukkitPlayer.uniqueId).thenReturn(uuid)
        `when`(bukkitPlayer.name).thenReturn("Alice")
        val server = mock(Server::class.java)
        `when`(server.offlinePlayers).thenReturn(arrayOf(bukkitPlayer))
        `when`(plugin.server).thenReturn(server)
        `when`(playerService.update(anyMfPlayer(), anyTransform())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val transform = invocation.arguments[1] as (MfPlayer) -> MfPlayer
            Success(transform(invocation.arguments[0] as MfPlayer))
        }
    }

    /** MF4's player power file: the uuid is stored JSON-encoded inside the string. */
    private fun powerRecords(power: String): File {
        val file = Files.createTempFile("playerpowerrecords", ".json").toFile()
        file.deleteOnExit()
        file.writeText(gson.toJson(listOf(MfLegacyPowerRecord(gson.toJson(uuid.toString()), power))))
        return file
    }

    private fun migratedBase(): Pair<MfPlayer, (MfPlayer) -> MfPlayer> {
        val base = ArgumentCaptor.forClass(MfPlayer::class.java)

        @Suppress("UNCHECKED_CAST")
        val transform = ArgumentCaptor.forClass(Function1::class.java) as ArgumentCaptor<(MfPlayer) -> MfPlayer>
        verify(playerService).update(base.capture() ?: placeholder(), transform.capture() ?: { it })
        verify(playerService, never()).save(anyMfPlayer())
        return base.value to transform.value
    }

    private fun placeholder() = MfPlayer(MfPlayerId(uuid.toString()))

    @Test
    fun anExistingRowGetsTheMigratedPower() {
        val existing = MfPlayer(MfPlayerId(uuid.toString()), version = 4, name = "Alice", power = 5.0, powerAtLogout = 5.0)
        `when`(playerService.getPlayer(bukkitPlayer)).thenReturn(existing)

        MfLegacyDataMigrator(plugin).migratePlayers(powerRecords("17.5"), gson)

        val (base, transform) = migratedBase()
        assertEquals(4, base.version, "the update starts from the stored row's version, not 0")
        val migrated = transform(base)
        assertEquals(17.5, migrated.power)
        assertEquals(17.5, migrated.powerAtLogout)
    }

    @Test
    fun aPlayerWithNoRowIsCreatedWithTheMigratedPower() {
        `when`(playerService.getPlayer(bukkitPlayer)).thenReturn(null)

        MfLegacyDataMigrator(plugin).migratePlayers(powerRecords("12"), gson)

        val (base, transform) = migratedBase()
        assertEquals(0, base.version)
        assertEquals(MfPlayerId(uuid.toString()), base.id)
        assertEquals(12.0, transform(base).power)
    }

    @Test
    fun aPlayerWithoutAPowerRecordStartsAtInitialPower() {
        `when`(playerService.getPlayer(bukkitPlayer)).thenReturn(null)

        MfLegacyDataMigrator(plugin).migratePlayers(powerRecords("not-a-number"), gson)

        val (base, transform) = migratedBase()
        assertEquals(5.0, transform(base).power)
    }

    private fun <T> anyTransform(): T {
        ArgumentMatchers.any<(MfPlayer) -> MfPlayer>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    private fun <T> anyMfPlayer(): T {
        ArgumentMatchers.any<MfPlayer>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}
