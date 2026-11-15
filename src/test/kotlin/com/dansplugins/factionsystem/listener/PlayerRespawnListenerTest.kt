package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.service.Services
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerRespawnEvent
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

/** Covers `factions.respawnAtFactionHome` (#1742). */
class PlayerRespawnListenerTest {

    private lateinit var config: FileConfiguration
    private lateinit var factionService: MfFactionService
    private lateinit var player: Player
    private lateinit var faction: MfFaction
    private lateinit var worldSpawn: Location
    private lateinit var home: Location
    private var factionHome: Location? = null
    private lateinit var uut: PlayerRespawnListener

    @BeforeEach
    fun setUp() {
        config = mock(FileConfiguration::class.java)
        factionService = mock(MfFactionService::class.java)
        val services = mock(Services::class.java)
        `when`(services.factionService).thenReturn(factionService)
        val plugin = mock(MedievalFactions::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(plugin.services).thenReturn(services)
        player = mock(Player::class.java)
        val uuid = UUID.randomUUID()
        `when`(player.uniqueId).thenReturn(uuid)
        faction = mock(MfFaction::class.java)
        val playerId = MfPlayerId(uuid.toString())
        `when`(factionService.getFaction(playerId)).thenReturn(faction)
        worldSpawn = mock(Location::class.java)
        home = mock(Location::class.java)
        // setRespawnLocation refuses a location without a world.
        `when`(home.world).thenReturn(mock(World::class.java))
        factionHome = home
        uut = PlayerRespawnListener(plugin) { factionHome }
    }

    @Test
    fun `respawn location is unchanged when the option is off`() {
        val event = respawn(isBedSpawn = false, isAnchorSpawn = false)

        uut.onPlayerRespawn(event)

        assertSame(worldSpawn, event.respawnLocation)
    }

    @Test
    fun `player respawns at the faction home when the option is on`() {
        `when`(config.getBoolean("factions.respawnAtFactionHome")).thenReturn(true)
        val event = respawn(isBedSpawn = false, isAnchorSpawn = false)

        uut.onPlayerRespawn(event)

        assertSame(home, event.respawnLocation)
    }

    @Test
    fun `a bed or respawn anchor keeps priority`() {
        `when`(config.getBoolean("factions.respawnAtFactionHome")).thenReturn(true)
        val bed = respawn(isBedSpawn = true, isAnchorSpawn = false)
        val anchor = respawn(isBedSpawn = false, isAnchorSpawn = true)

        uut.onPlayerRespawn(bed)
        uut.onPlayerRespawn(anchor)

        assertSame(worldSpawn, bed.respawnLocation)
        assertSame(worldSpawn, anchor.respawnLocation)
    }

    @Test
    fun `a faction without a home leaves the respawn location alone`() {
        `when`(config.getBoolean("factions.respawnAtFactionHome")).thenReturn(true)
        factionHome = null
        val event = respawn(isBedSpawn = false, isAnchorSpawn = false)

        uut.onPlayerRespawn(event)

        assertSame(worldSpawn, event.respawnLocation)
    }

    @Test
    fun `a factionless player respawns as usual`() {
        `when`(config.getBoolean("factions.respawnAtFactionHome")).thenReturn(true)
        val factionless = mock(Player::class.java)
        `when`(factionless.uniqueId).thenReturn(UUID.randomUUID())
        val event = PlayerRespawnEvent(factionless, worldSpawn, false, false, PlayerRespawnEvent.RespawnReason.DEATH)

        uut.onPlayerRespawn(event)

        assertSame(worldSpawn, event.respawnLocation)
    }

    @Test
    fun `leaving the End does not send the player home`() {
        `when`(config.getBoolean("factions.respawnAtFactionHome")).thenReturn(true)
        val event = PlayerRespawnEvent(player, worldSpawn, false, false, PlayerRespawnEvent.RespawnReason.END_PORTAL)

        uut.onPlayerRespawn(event)

        assertSame(worldSpawn, event.respawnLocation)
    }

    private fun respawn(isBedSpawn: Boolean, isAnchorSpawn: Boolean) =
        PlayerRespawnEvent(player, worldSpawn, isBedSpawn, isAnchorSpawn, PlayerRespawnEvent.RespawnReason.DEATH)
}
