package com.dansplugins.factionsystem.command.faction.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
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
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Locale
import java.util.UUID
import java.util.logging.Logger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionClaimFillCommandTest {
    private val testUtils = TestUtils()
    private val factionId = MfFactionId.generate()
    private val worldId = UUID.randomUUID()
    private val mfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()))

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var language: Language
    private lateinit var config: FileConfiguration
    private lateinit var faction: MfFaction
    private lateinit var world: World
    private lateinit var pendingTasks: MutableList<Runnable>
    private lateinit var uut: MfFactionClaimFillCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        pendingTasks = mutableListOf()
        world = testUtils.createMockWorld(worldId)
        mockServices()
        mockLanguageSystem()
        mockConfig()
        mockScheduler()
        mockLogger()
        uut = MfFactionClaimFillCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(language["CommandFactionClaimFillNoPermission"]).thenReturn("No permission")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}No permission")
    }

    @Test
    fun testOnCommand_senderThatIsNotAPlayer() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.claim.fill")).thenReturn(true)
        `when`(language["CommandFactionClaimFillNotAPlayer"]).thenReturn("Not a player")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Not a player")
    }

    @Test
    fun testOnCommand_reportsNothingToClaimWhenTheSenderStandsInTheirOwnClaim() {
        // prepare — the fill starts on a chunk the sender's own faction already holds, so it terminates immediately
        val player = fixture.player
        val command = fixture.command
        stubMembershipOf(player)
        stubSenderChunk(player, 0, 0)
        stubOwnClaim(0, 0)
        `when`(language["CommandFactionClaimFillNoClaimableChunks"]).thenReturn("Nothing to claim")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.RED}Nothing to claim")
    }

    @Test
    fun testOnCommand_fillsFromTheChunkTheSenderStoodInWhenTheCommandWasRun() {
        // prepare — the sender walks out of their own faction's claim while the fill is still queued. The chunk they
        // stood in is held by their own faction, so the fill terminates there and reports nothing to claim; the chunk
        // they walk into is unclaimed and, with the faction out of power, would report a lack of power instead. Which
        // of the two messages arrives therefore says which chunk the fill started from.
        // See https://github.com/Dans-Plugins/Medieval-Factions/issues/2006.
        val player = fixture.player
        val command = fixture.command
        stubMembershipOf(player)
        stubSenderChunk(player, 0, 0)
        stubOwnClaim(0, 0)
        `when`(faction.power).thenReturn(0.0)
        `when`(language["CommandFactionClaimFillNoClaimableChunks"]).thenReturn("Nothing to claim")
        `when`(language["CommandFactionClaimFillNotEnoughPower"]).thenReturn("Not enough power")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        stubSenderChunk(player, 4, 4)
        runPendingTasks()

        // verify — the fill started from the chunk the sender stood on, not the one they walked into
        assertTrue(result)
        verify(claimService).getClaim(worldId, 0, 0)
        verify(claimService, never()).getClaim(worldId, 4, 4)
        verify(player).sendMessage("${ChatColor.RED}Nothing to claim")
        verify(player, never()).sendMessage("${ChatColor.RED}Not enough power")
    }

    // Helper functions

    private fun stubOwnClaim(chunkX: Int, chunkZ: Int): MfClaimedChunk {
        val claim = MfClaimedChunk(worldId, chunkX, chunkZ, factionId)
        `when`(claimService.getClaim(worldId, chunkX, chunkZ)).thenReturn(claim)
        return claim
    }

    private fun stubMembershipOf(player: Player) {
        val role = mock(MfFactionRole::class.java)
        val claimPermission = MfFactionPermission("CLAIM", "Claim", false)
        val factionPermissions = mock(MfFactionPermissions::class.java)
        `when`(player.hasPermission("mf.claim.fill")).thenReturn(true)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.getRole(mfPlayer.id)).thenReturn(role)
        `when`(plugin.factionPermissions).thenReturn(factionPermissions)
        `when`(factionPermissions.claim).thenReturn(claimPermission)
        `when`(role.hasPermission(faction, claimPermission)).thenReturn(true)
    }

    private fun stubSenderChunk(player: Player, chunkX: Int, chunkZ: Int) {
        val chunk = testUtils.createMockChunk(world, chunkX, chunkZ)
        val location = mock(Location::class.java)
        `when`(location.chunk).thenReturn(chunk)
        `when`(location.world).thenReturn(world)
        `when`(player.location).thenReturn(location)
        `when`(player.world).thenReturn(world)
    }

    private fun runPendingTasks() {
        while (pendingTasks.isNotEmpty()) {
            pendingTasks.removeAt(0).run()
        }
    }

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)

        `when`(services.factionRelationshipService).thenReturn(mock(MfFactionRelationshipService::class.java))

        faction = mock(MfFaction::class.java)
        `when`(faction.id).thenReturn(factionId)
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language.locale).thenReturn(Locale.ENGLISH)
    }

    private fun mockConfig() {
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        // Both are read once when the command is constructed, so they have to be stubbed before `uut` is created.
        `when`(config.getInt("factions.claimFillMaxChunks", -1)).thenReturn(-1)
        `when`(config.getInt("factions.claimFillMaxDepth", 50)).thenReturn(50)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        // Every task the command schedules is queued rather than run, so a test can move the sender in between the
        // command being run and the task executing — which is the case this command has to get right.
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            pendingTasks += invocation.arguments[1] as Runnable
            null
        }
        `when`(scheduler.runTask(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            pendingTasks += invocation.arguments[1] as Runnable
            null
        }
    }

    private fun mockLogger() {
        val logger = mock(Logger::class.java)
        `when`(plugin.logger).thenReturn(logger)
    }
}
