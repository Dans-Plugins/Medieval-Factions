package com.dansplugins.factionsystem.command.faction.rival

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.rival.MfFactionRival
import com.dansplugins.factionsystem.relationship.rival.MfFactionRivalRepository
import com.dansplugins.factionsystem.relationship.rival.MfFactionRivalService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger

/** Covers `/faction rival` and `/faction unrival` (#1896). */
class MfFactionRivalCommandTest {

    private val senderId = MfPlayerId(UUID.randomUUID().toString())
    private val declareWar = MfFactionPermission("DECLARE_WAR", "Declare war", false)
    private val stored = mutableListOf<MfFactionRival>()

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var player: Player
    private lateinit var factionService: MfFactionService
    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var rivalService: MfFactionRivalService
    private lateinit var north: MfFaction
    private lateinit var south: MfFaction

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        val language = mock(Language::class.java) { invocation -> invocation.arguments.joinToString("|") }
        `when`(plugin.language).thenReturn(language)
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }
        val permissions = mock(MfFactionPermissions::class.java)
        `when`(plugin.factionPermissions).thenReturn(permissions)
        `when`(permissions.declareWar).thenReturn(declareWar)

        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        val playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
        relationshipService = mock(MfFactionRelationshipService::class.java)
        `when`(services.factionRelationshipService).thenReturn(relationshipService)
        rivalService = MfFactionRivalService(plugin, FakeRivalRepository())
        `when`(services.factionRivalService).thenReturn(rivalService)

        player = mock(Player::class.java)
        `when`(player.hasPermission("mf.rival")).thenReturn(true)
        `when`(playerService.getPlayer(player)).thenReturn(MfPlayer(senderId))

        north = faction("North")
        south = faction("South")
        val role = mock(MfFactionRole::class.java)
        `when`(role.hasPermission(north, declareWar)).thenReturn(true)
        `when`(north.getRole(senderId)).thenReturn(role)
        `when`(factionService.getFaction(senderId)).thenReturn(north)
    }

    @Test
    fun rival_namesTheTargetAsARival() {
        run(remove = false, "South")

        assertEquals(listOf(north.id to south.id), stored.map { it.factionId to it.targetId })
        assertEquals(listOf(south.id), rivalService.getRivals(north.id).map { it.targetId })
        verify(player).sendMessage("${GREEN}CommandFactionRivalSuccess|South")
    }

    @Test
    fun rival_isRefusedForAnAlly() {
        `when`(relationshipService.getRelationships(north.id, south.id)).thenReturn(listOf(MfFactionRelationship(factionId = north.id, targetId = south.id, type = ALLY)))
        `when`(relationshipService.getRelationships(south.id, north.id)).thenReturn(listOf(MfFactionRelationship(factionId = south.id, targetId = north.id, type = ALLY)))

        run(remove = false, "South")

        assertEquals(emptyList<MfFactionRival>(), stored)
        verify(player).sendMessage("${RED}CommandFactionRivalAllied|South")
    }

    @Test
    fun rival_needsTheDeclareWarRolePermission() {
        `when`(north.getRole(senderId)).thenReturn(mock(MfFactionRole::class.java))

        run(remove = false, "South")

        assertEquals(emptyList<MfFactionRival>(), stored)
        verify(player).sendMessage("${RED}CommandFactionRivalNoFactionPermission")
    }

    @Test
    fun unrival_endsTheRivalryWhenThereIsNoMinimum() {
        rivalService.add(MfFactionRival(north.id, south.id))

        run(remove = true, "South")

        assertEquals(emptyList<MfFactionRival>(), stored)
        verify(player).sendMessage("${GREEN}CommandFactionUnrivalSuccess|South")
    }

    @Test
    fun unrival_isRefusedBeforeTheMinimumRivalryTime() {
        `when`(config.getDouble("factions.minimumRivalryHours")).thenReturn(24.0)
        rivalService.add(MfFactionRival(north.id, south.id, Instant.now().minus(Duration.ofHours(20))))

        run(remove = true, "South")

        assertEquals(1, stored.size)
        verify(player).sendMessage("${RED}CommandFactionUnrivalTooSoon|South|4")
    }

    @Test
    fun unrival_isAllowedOnceTheMinimumHasPassed() {
        `when`(config.getDouble("factions.minimumRivalryHours")).thenReturn(24.0)
        rivalService.add(MfFactionRival(north.id, south.id, Instant.now().minus(Duration.ofHours(25))))

        run(remove = true, "South")

        assertEquals(emptyList<MfFactionRival>(), stored)
    }

    @Test
    fun rivalsOfAFactionThatNoLongerExistsAreNotShown() {
        val gone = MfFactionId.generate()
        rivalService.add(MfFactionRival(north.id, gone))
        rivalService.add(MfFactionRival(north.id, south.id))

        assertEquals(listOf(south.id), rivalService.getRivals(north.id).map { it.targetId })
    }

    private fun run(remove: Boolean, vararg args: String) {
        MfFactionRivalCommand(plugin, remove).onCommand(player, mock(Command::class.java), "label", arrayOf(*args))
    }

    private fun faction(name: String): MfFaction {
        val faction = mock(MfFaction::class.java)
        val id = MfFactionId.generate()
        `when`(faction.id).thenReturn(id)
        `when`(faction.name).thenReturn(name)
        `when`(factionService.getFaction(name)).thenReturn(faction)
        `when`(factionService.getFaction(id)).thenReturn(faction)
        return faction
    }

    private inner class FakeRivalRepository : MfFactionRivalRepository {
        override fun getRivals() = stored.toList()
        override fun insert(rival: MfFactionRival): MfFactionRival {
            stored.add(rival)
            return rival
        }
        override fun delete(factionId: MfFactionId, targetId: MfFactionId) {
            stored.removeIf { it.factionId == factionId && it.targetId == targetId }
        }
    }
}
