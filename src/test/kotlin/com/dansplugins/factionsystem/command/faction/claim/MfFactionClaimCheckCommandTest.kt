package com.dansplugins.factionsystem.command.faction.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.service.Services
import org.bukkit.ChatColor
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Server
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
class MfFactionClaimCheckCommandTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var claimService: MfClaimService
    private lateinit var language: Language
    private lateinit var pendingTasks: MutableList<Runnable>
    private lateinit var chunks: MutableMap<MfChunkPosition, Chunk>
    private lateinit var uut: MfFactionClaimCheckCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        pendingTasks = mutableListOf()
        chunks = mutableMapOf()
        mockServices()
        mockLanguageSystem()
        mockScheduler()
        mockLogger()
        uut = MfFactionClaimCheckCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(language["CommandFactionCheckClaimNoPermission"]).thenReturn("No permission")

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
        `when`(sender.hasPermission("mf.claim.check")).thenReturn(true)
        `when`(language["CommandFactionCheckClaimNotAPlayer"]).thenReturn("Not a player")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Not a player")
    }

    @Test
    fun testOnCommand_namesTheFactionHoldingTheChunkTheSenderIsStandingIn() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        `when`(player.hasPermission("mf.claim.check")).thenReturn(true)
        stubSenderChunk(player, worldId, 0, 0)
        stubClaim(worldId, 0, 0, "Bandits")
        `when`(language["CommandFactionCheckClaimClaimed", "Bandits"]).thenReturn("Claimed by Bandits")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.GREEN}Claimed by Bandits")
    }

    @Test
    fun testOnCommand_reportsAnUnclaimedChunkAsUnclaimed() {
        // prepare — no claim is stubbed for the chunk the sender is standing in
        val player = fixture.player
        val command = fixture.command
        `when`(player.hasPermission("mf.claim.check")).thenReturn(true)
        stubSenderChunk(player, UUID.randomUUID(), 0, 0)
        `when`(language["CommandFactionCheckClaimNotClaimed"]).thenReturn("Not claimed")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        runPendingTasks()

        // verify
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.GREEN}Not claimed")
    }

    @Test
    fun testOnCommand_reportsTheChunkTheSenderStoodInWhenTheCommandWasRun() {
        // prepare — the sender walks into a neighbouring faction's claim while the lookup is still queued.
        // See https://github.com/Dans-Plugins/Medieval-Factions/issues/2006.
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        `when`(player.hasPermission("mf.claim.check")).thenReturn(true)
        stubSenderChunk(player, worldId, 0, 0)
        stubClaim(worldId, 0, 0, "Bandits")
        stubClaim(worldId, 8, 8, "Wanderers")
        `when`(language["CommandFactionCheckClaimClaimed", "Bandits"]).thenReturn("Claimed by Bandits")
        `when`(language["CommandFactionCheckClaimClaimed", "Wanderers"]).thenReturn("Claimed by Wanderers")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        stubSenderChunk(player, worldId, 8, 8)
        runPendingTasks()

        // verify — the chunk the sender occupied when the command was run is the one reported on
        assertTrue(result)
        verify(claimService).getClaim(MfChunkPosition(worldId, 0, 0))
        verify(claimService, never()).getClaim(MfChunkPosition(worldId, 8, 8))
        verify(player).sendMessage("${ChatColor.GREEN}Claimed by Bandits")
    }

    // Helper functions

    /**
     * Stubs the claim held over one chunk, together with the faction holding it. Both ways of asking for that claim
     * are stubbed — by chunk and by chunk position — so a test asserts which chunk was consulted rather than which
     * overload happened to be called.
     */
    private fun stubClaim(worldId: UUID, chunkX: Int, chunkZ: Int, factionName: String) {
        val factionId = MfFactionId.generate()
        val faction = mock(MfFaction::class.java)
        `when`(faction.name).thenReturn(factionName)
        `when`(factionService.getFaction(factionId)).thenReturn(faction)
        val claim = MfClaimedChunk(worldId, chunkX, chunkZ, factionId)
        `when`(claimService.getClaim(chunkOf(worldId, chunkX, chunkZ))).thenReturn(claim)
        `when`(claimService.getClaim(MfChunkPosition(worldId, chunkX, chunkZ))).thenReturn(claim)
    }

    private fun stubSenderChunk(player: Player, worldId: UUID, chunkX: Int, chunkZ: Int) {
        val location = mock(Location::class.java)
        `when`(location.chunk).thenReturn(chunkOf(worldId, chunkX, chunkZ))
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

        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language.locale).thenReturn(Locale.ENGLISH)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        // The dispatched task is queued rather than run, so a test can move the sender in between the command
        // being run and the task executing — which is the case this command has to get right.
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            pendingTasks += invocation.arguments[1] as Runnable
            null
        }
    }

    private fun mockLogger() {
        val logger = mock(Logger::class.java)
        `when`(plugin.logger).thenReturn(logger)
    }
}
