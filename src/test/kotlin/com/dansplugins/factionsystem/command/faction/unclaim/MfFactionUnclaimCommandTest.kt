package com.dansplugins.factionsystem.command.faction.unclaim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfChunkPosition
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
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Server
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
class MfFactionUnclaimCommandTest {
    private val testUtils = TestUtils()
    private val factionId = MfFactionId.generate()

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var language: Language
    private lateinit var config: FileConfiguration
    private lateinit var faction: MfFaction
    private lateinit var pendingTasks: MutableList<Runnable>
    private lateinit var chunks: MutableMap<MfChunkPosition, Chunk>
    private lateinit var uut: MfFactionUnclaimCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        pendingTasks = mutableListOf()
        chunks = mutableMapOf()
        mockServices()
        mockLanguageSystem()
        mockConfig()
        mockScheduler()
        mockLogger()
        uut = MfFactionUnclaimCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(language["CommandFactionUnclaimNoPermission"]).thenReturn("No permission")

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
        `when`(sender.hasPermission("mf.unclaim")).thenReturn(true)
        `when`(language["CommandFactionUnclaimNotAPlayer"]).thenReturn("Not a player")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Not a player")
    }

    @Test
    fun testOnCommand_unclaimsTheChunkTheSenderIsStandingIn() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        stubMembershipOf(player)
        stubSenderChunk(player, worldId, 0, 0)
        val claim = stubOwnClaim(worldId, 0, 0)
        `when`(language["CommandFactionUnclaimSuccess", "1"]).thenReturn("Unclaimed 1 chunk")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(claimService).delete(claim)
        verify(player).sendMessage("${ChatColor.GREEN}Unclaimed 1 chunk")
    }

    @Test
    fun testOnCommand_leavesAnotherFactionsClaimAlone() {
        // prepare — the sender stands in a chunk held by a faction other than their own
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        stubMembershipOf(player)
        stubSenderChunk(player, worldId, 0, 0)
        val claim = stubClaimOwnedBy(worldId, 0, 0, MfFactionId.generate())
        `when`(language["CommandFactionUnclaimNoUnclaimableChunks"]).thenReturn("Nothing to unclaim")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(claimService, never()).delete(claim)
        verify(player).sendMessage("${ChatColor.RED}Nothing to unclaim")
    }

    @Test
    fun testOnCommand_unclaimsTheChunkTheSenderStoodInWhenTheCommandWasRun() {
        // prepare — the sender walks one chunk over while the unclaim is still queued. Both chunks are held by the
        // sender's own faction, so either one could be deleted, and only the chunk they were standing in when the
        // command was run should be. See https://github.com/Dans-Plugins/Medieval-Factions/issues/2006.
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        stubMembershipOf(player)
        stubSenderChunk(player, worldId, 0, 0)
        val standingClaim = stubOwnClaim(worldId, 0, 0)
        val neighbouringClaim = stubOwnClaim(worldId, 4, 4)
        `when`(language["CommandFactionUnclaimSuccess", "1"]).thenReturn("Unclaimed 1 chunk")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        stubSenderChunk(player, worldId, 4, 4)
        runPendingTasks()

        // verify — the claim the sender was standing on is deleted, not the one they walked into
        assertTrue(result)
        verify(claimService).delete(standingClaim)
        verify(claimService, never()).delete(neighbouringClaim)
        verify(player).sendMessage("${ChatColor.GREEN}Unclaimed 1 chunk")
    }

    @Test
    fun testOnCommand_forceUnclaimRemovesANamedFactionsClaimWithoutMembership() {
        // prepare — an admin who is in no faction names another faction; mf.force.unclaim replaces that faction's
        // role check. See https://github.com/Dans-Plugins/Medieval-Factions/issues/1987.
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        val rivals = stubNamedFaction("Rivals")
        stubAdminWithoutFaction(player, forceUnclaim = true)
        stubSenderChunk(player, worldId, 0, 0)
        val claim = stubClaimOwnedBy(worldId, 0, 0, rivals.id)
        `when`(language["CommandFactionUnclaimSuccess", "1"]).thenReturn("Unclaimed 1 chunk")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("Rivals"))
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(claimService).delete(claim)
        verify(player).sendMessage("${ChatColor.GREEN}Unclaimed 1 chunk")
    }

    @Test
    fun testOnCommand_namingAFactionWithoutForcePermissionLeavesItsClaimAlone() {
        // prepare — the same command without mf.force.unclaim keeps acting on the sender's own faction only
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        val rivals = stubNamedFaction("Rivals")
        stubMembershipOf(player)
        stubSenderChunk(player, worldId, 0, 0)
        val claim = stubClaimOwnedBy(worldId, 0, 0, rivals.id)
        `when`(language["CommandFactionUnclaimNoUnclaimableChunks"]).thenReturn("Nothing to unclaim")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("Rivals"))
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(claimService, never()).delete(claim)
        verify(player).sendMessage("${ChatColor.RED}Nothing to unclaim")
    }

    @Test
    fun testOnCommand_forceUnclaimWithARadiusOnlyRemovesTheNamedFactionsClaims() {
        // prepare — two factions hold land within the radius; only the named one loses it
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        val rivals = stubNamedFaction("Rivals")
        stubAdminWithoutFaction(player, forceUnclaim = true)
        stubSenderChunk(player, worldId, 0, 0)
        val rivalClaim = stubClaimOwnedBy(worldId, 1, 0, rivals.id)
        val bystanderClaim = stubClaimOwnedBy(worldId, 0, 1, MfFactionId.generate())
        `when`(language["CommandFactionUnclaimSuccess", "1"]).thenReturn("Unclaimed 1 chunk")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("Rivals", "1"))
        runPendingTasks()

        // verify — the message counts the one claim removed, not the five chunks in the radius
        assertTrue(result)
        verify(claimService).delete(rivalClaim)
        verify(claimService, never()).delete(bystanderClaim)
        verify(player).sendMessage("${ChatColor.GREEN}Unclaimed 1 chunk")
    }

    @Test
    fun testOnCommand_successMessageCountsTheClaimsRemovedRatherThanTheChunksInTheRadius() {
        // prepare — a radius of 2 covers 13 chunks, of which the sender's faction holds only two
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        stubMembershipOf(player)
        stubSenderChunk(player, worldId, 0, 0)
        val firstClaim = stubOwnClaim(worldId, 0, 0)
        val secondClaim = stubOwnClaim(worldId, 2, 0)
        `when`(language["CommandFactionUnclaimSuccess", "2"]).thenReturn("Unclaimed 2 chunks")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("2"))
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(claimService).delete(firstClaim)
        verify(claimService).delete(secondClaim)
        verify(player).sendMessage("${ChatColor.GREEN}Unclaimed 2 chunks")
        verify(language, never()).get("CommandFactionUnclaimSuccess", "13")
    }

    @Test
    fun testOnCommand_forceUnclaimOnlyRemovesTheNamedFactionsClaimsEvenInBypassMode() {
        // prepare — bypass mode on its own unclaims every faction's land in range; naming a faction narrows it
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        val rivals = stubNamedFaction("Rivals")
        stubAdminWithoutFaction(player, forceUnclaim = true, bypassEnabled = true)
        stubSenderChunk(player, worldId, 0, 0)
        val rivalClaim = stubClaimOwnedBy(worldId, 1, 0, rivals.id)
        val bystanderClaim = stubClaimOwnedBy(worldId, 0, 1, MfFactionId.generate())

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("Rivals", "1"))
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(claimService).delete(rivalClaim)
        verify(claimService, never()).delete(bystanderClaim)
    }

    @Test
    fun testOnCommand_forceUnclaimRefusesAFactionThatDoesNotExist() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        stubAdminWithoutFaction(player, forceUnclaim = true)
        stubSenderChunk(player, worldId, 0, 0)
        val claim = stubClaimOwnedBy(worldId, 0, 0, MfFactionId.generate())
        `when`(language["CommandFactionUnclaimInvalidFaction", "Nobody"]).thenReturn("No such faction")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("Nobody"))
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(claimService, never()).delete(claim)
        verify(player).sendMessage("${ChatColor.RED}No such faction")
    }

    @Test
    fun testOnCommand_aLoneNumberIsStillARadiusWithForcePermission() {
        // prepare — `/f unclaim 0` from a force-permission holder still unclaims around them for their own faction
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        stubMembershipOf(player)
        `when`(player.hasPermission("mf.force.unclaim")).thenReturn(true)
        stubSenderChunk(player, worldId, 0, 0)
        val claim = stubOwnClaim(worldId, 0, 0)
        `when`(language["CommandFactionUnclaimSuccess", "1"]).thenReturn("Unclaimed 1 chunk")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("0"))
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(claimService).delete(claim)
        verify(player).sendMessage("${ChatColor.GREEN}Unclaimed 1 chunk")
    }

    // Helper functions

    private fun stubOwnClaim(worldId: UUID, chunkX: Int, chunkZ: Int) = stubClaimOwnedBy(worldId, chunkX, chunkZ, factionId)

    /**
     * Stubs the claim held over one chunk, and stubs its deletion whether or not the test expects it to be deleted. A
     * claim the command was never meant to touch is therefore reported by the `never()` verification rather than by an
     * exception on an unstubbed call, which keeps the failure legible if this protection ever regresses. Both ways of
     * asking for the claim are stubbed — by chunk and by chunk position — so a test asserts which chunk was
     * consulted rather than which overload happened to be called.
     */
    private fun stubClaimOwnedBy(worldId: UUID, chunkX: Int, chunkZ: Int, owningFactionId: MfFactionId): MfClaimedChunk {
        val claim = MfClaimedChunk(worldId, chunkX, chunkZ, owningFactionId)
        val chunk = chunkOf(worldId, chunkX, chunkZ)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(claimService.getClaim(MfChunkPosition(worldId, chunkX, chunkZ))).thenReturn(claim)
        `when`(claimService.delete(claim)).thenReturn(Success(Unit))
        return claim
    }

    private fun stubNamedFaction(name: String): MfFaction {
        val namedFaction = mock(MfFaction::class.java)
        `when`(namedFaction.id).thenReturn(MfFactionId.generate())
        `when`(namedFaction.name).thenReturn(name)
        `when`(factionService.getFaction(name)).thenReturn(namedFaction)
        return namedFaction
    }

    /**
     * Stubs a sender who holds `mf.unclaim` (and, optionally, `mf.force.unclaim`) but belongs to no faction, so any
     * faction they act on can only have been reached through the force permission.
     */
    private fun stubAdminWithoutFaction(player: Player, forceUnclaim: Boolean, bypassEnabled: Boolean = false) {
        val mfPlayer = mock(MfPlayer::class.java)
        `when`(mfPlayer.isBypassEnabled).thenReturn(bypassEnabled)
        `when`(player.hasPermission("mf.unclaim")).thenReturn(true)
        `when`(player.hasPermission("mf.force.unclaim")).thenReturn(forceUnclaim)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
    }

    private fun stubMembershipOf(player: Player) {
        val mfPlayer = mock(MfPlayer::class.java)
        val role = mock(MfFactionRole::class.java)
        val unclaim = MfFactionPermission("UNCLAIM", "Unclaim", false)
        val factionPermissions = mock(MfFactionPermissions::class.java)
        `when`(player.hasPermission("mf.unclaim")).thenReturn(true)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.getRole(mfPlayer.id)).thenReturn(role)
        `when`(plugin.factionPermissions).thenReturn(factionPermissions)
        `when`(factionPermissions.unclaim).thenReturn(unclaim)
        `when`(role.hasPermission(faction, unclaim)).thenReturn(true)
    }

    private fun stubSenderChunk(player: Player, worldId: UUID, chunkX: Int, chunkZ: Int) {
        val chunk = chunkOf(worldId, chunkX, chunkZ)
        val location = mock(Location::class.java)
        `when`(location.chunk).thenReturn(chunk)
        `when`(player.location).thenReturn(location)
    }

    /**
     * Returns one mock chunk per set of coordinates, so that the chunk a claim is stubbed against and the chunk the
     * sender is standing in are the same object.
     */
    private fun chunkOf(worldId: UUID, chunkX: Int, chunkZ: Int) = chunks.getOrPut(MfChunkPosition(worldId, chunkX, chunkZ)) {
        testUtils.createMockChunk(testUtils.createMockWorld(worldId), chunkX, chunkZ)
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
