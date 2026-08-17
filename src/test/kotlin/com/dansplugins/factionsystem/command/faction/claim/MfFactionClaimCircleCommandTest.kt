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
import dev.forkhandles.result4k.Success
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
class MfFactionClaimCircleCommandTest {
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
    private lateinit var uut: MfFactionClaimCircleCommand

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
        uut = MfFactionClaimCircleCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(language["CommandFactionClaimNoPermission"]).thenReturn("No permission")

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
        `when`(sender.hasPermission("mf.claim")).thenReturn(true)
        `when`(language["CommandFactionClaimNotAPlayer"]).thenReturn("Not a player")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Not a player")
    }

    @Test
    fun testOnCommand_claimsTheChunkTheSenderIsStandingIn() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        stubMembershipOf(player)
        stubSenderChunk(player, 0, 0)
        val claim = stubClaimSave(0, 0)
        `when`(language["CommandFactionClaimSuccess", "1"]).thenReturn("Claimed 1 chunk")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(claimService).save(claim)
        verify(player).sendMessage("${ChatColor.GREEN}Claimed 1 chunk")
    }

    @Test
    fun testOnCommand_claimsTheChunkTheSenderStoodInWhenTheCommandWasRun() {
        // prepare — the sender walks away while the claim is still queued. Both chunks are unclaimed and saving
        // either one is stubbed, so either could be claimed, and only the chunk they were standing in when the
        // command was run should be. See https://github.com/Dans-Plugins/Medieval-Factions/issues/2006.
        val player = fixture.player
        val command = fixture.command
        stubMembershipOf(player)
        stubSenderChunk(player, 0, 0)
        val standingClaim = stubClaimSave(0, 0)
        val neighbouringClaim = stubClaimSave(8, 8)
        `when`(language["CommandFactionClaimSuccess", "1"]).thenReturn("Claimed 1 chunk")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        stubSenderChunk(player, 8, 8)
        runPendingTasks()

        // verify — the chunk the sender stood on is claimed, not the one they walked into
        assertTrue(result)
        verify(claimService).save(standingClaim)
        verify(claimService, never()).save(neighbouringClaim)
        verify(player).sendMessage("${ChatColor.GREEN}Claimed 1 chunk")
    }

    // Helper functions

    /**
     * Stubs the save of the claim over one chunk, whether or not the test expects that chunk to be claimed. A chunk
     * the command was never meant to touch is therefore reported by the `never()` verification rather than by an
     * exception on an unstubbed call, which keeps the failure legible if this protection ever regresses.
     */
    private fun stubClaimSave(chunkX: Int, chunkZ: Int): MfClaimedChunk {
        val claim = MfClaimedChunk(worldId, chunkX, chunkZ, factionId)
        `when`(claimService.save(claim)).thenReturn(Success(claim))
        return claim
    }

    private fun stubMembershipOf(player: Player) {
        val role = mock(MfFactionRole::class.java)
        val claimPermission = MfFactionPermission("CLAIM", "Claim", false)
        val factionPermissions = mock(MfFactionPermissions::class.java)
        `when`(player.hasPermission("mf.claim")).thenReturn(true)
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
        `when`(config.getInt("factions.maxClaimRadius")).thenReturn(10)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        // Every task the command schedules, on either thread, is queued rather than run, so a test can move the
        // sender in between the command being run and the task executing — which is the case this command has to
        // get right. `runPendingTasks` then drains the queue in the order the tasks were dispatched.
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
