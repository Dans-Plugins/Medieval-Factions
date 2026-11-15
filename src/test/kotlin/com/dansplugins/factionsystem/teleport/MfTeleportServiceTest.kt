package com.dansplugins.factionsystem.teleport

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

/** Covers `factions.factionHomeTeleportCooldown` (#1625). */
class MfTeleportServiceTest {

    private var now = 1_000_000L
    private lateinit var config: FileConfiguration
    private lateinit var player: Player
    private lateinit var location: Location
    private lateinit var uut: MfTeleportService

    @BeforeEach
    fun setUp() {
        config = mock(FileConfiguration::class.java)
        // factions.factionHomeTeleportDelay is left at 0, so teleports happen immediately.
        val plugin = mock(MedievalFactions::class.java)
        `when`(plugin.config).thenReturn(config)
        player = mock(Player::class.java)
        `when`(player.uniqueId).thenReturn(UUID.randomUUID())
        location = mock(Location::class.java)
        uut = MfTeleportService(plugin) { now }
    }

    @Test
    fun `there is no cooldown by default`() {
        uut.teleport(player, location, "done")

        verify(player).teleport(location)
        assertEquals(0, uut.getRemainingCooldownSeconds(player))
    }

    @Test
    fun `a player who has not teleported has no cooldown`() {
        `when`(config.getLong("factions.factionHomeTeleportCooldown")).thenReturn(60)

        assertEquals(0, uut.getRemainingCooldownSeconds(player))
    }

    @Test
    fun `a completed teleport starts the cooldown, which counts down and expires`() {
        `when`(config.getLong("factions.factionHomeTeleportCooldown")).thenReturn(60)

        uut.teleport(player, location, "done")
        assertEquals(60, uut.getRemainingCooldownSeconds(player))

        now += 59_500
        assertEquals(1, uut.getRemainingCooldownSeconds(player), "a part second left rounds up")

        now += 500
        assertEquals(0, uut.getRemainingCooldownSeconds(player))
    }

    @Test
    fun `the cooldown is per player`() {
        `when`(config.getLong("factions.factionHomeTeleportCooldown")).thenReturn(60)
        val other = mock(Player::class.java)
        `when`(other.uniqueId).thenReturn(UUID.randomUUID())

        uut.teleport(player, location, "done")

        assertEquals(0, uut.getRemainingCooldownSeconds(other))
    }
}
