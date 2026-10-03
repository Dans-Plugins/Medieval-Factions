package com.dansplugins.factionsystem.listener

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
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
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
import java.util.UUID
import java.util.logging.Logger

/**
 * Covers the autounclaim branch of [PlayerMoveListener]. Autounclaim deletes claims as a side effect of walking, so
 * every guard on it (the toggle, ownership of the claim, the role permission and the Bukkit permission) is pinned here.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlayerMoveListenerTest {
    private val testUtils = TestUtils()
    private val factionId = MfFactionId.generate()
    private val worldId = UUID.randomUUID()
    private val unclaim = MfFactionPermission("UNCLAIM", "Unclaim", false)

    private lateinit var plugin: MedievalFactions
    private lateinit var claimService: MfClaimService
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var faction: MfFaction
    private lateinit var role: MfFactionRole
    private lateinit var player: Player
    private lateinit var fromChunk: Chunk
    private lateinit var toChunk: Chunk
    private lateinit var event: PlayerMoveEvent
    private lateinit var asyncTasks: MutableList<Runnable>
    private lateinit var syncTasks: MutableList<Runnable>
    private lateinit var uut: PlayerMoveListener

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        asyncTasks = mutableListOf()
        syncTasks = mutableListOf()
        mockServices()
        mockScheduler()
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))

        // The player walks from wilderness (0, 0) into the chunk at (1, 0).
        val world = testUtils.createMockWorld(worldId)
        fromChunk = testUtils.createMockChunk(world, 0, 0)
        toChunk = testUtils.createMockChunk(world, 1, 0)
        val from = mock(Location::class.java)
        val to = mock(Location::class.java)
        `when`(from.chunk).thenReturn(fromChunk)
        `when`(to.chunk).thenReturn(toChunk)
        player = mock(Player::class.java)
        event = mock(PlayerMoveEvent::class.java)
        `when`(event.from).thenReturn(from)
        `when`(event.to).thenReturn(to)
        `when`(event.player).thenReturn(player)

        // The player is a member of `faction`, with a role that may unclaim, and holds mf.unclaim.
        val mfPlayer = mock(MfPlayer::class.java)
        role = mock(MfFactionRole::class.java)
        val factionPermissions = mock(MfFactionPermissions::class.java)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.getRole(mfPlayer.id)).thenReturn(role)
        `when`(plugin.factionPermissions).thenReturn(factionPermissions)
        `when`(factionPermissions.unclaim).thenReturn(unclaim)
        `when`(role.getPermissionValue(unclaim)).thenReturn(true)
        `when`(player.hasPermission("mf.unclaim")).thenReturn(true)

        uut = PlayerMoveListener(plugin)
    }

    @Test
    fun onPlayerMove_autounclaimOn_ownClaim_unclaimsTheChunkWithoutAStaleTerritoryTitle() {
        `when`(faction.autounclaim).thenReturn(true)
        val claim = stubClaimAtDestination(factionId)

        uut.onPlayerMove(event)
        runAsyncTasks()

        verify(claimService).delete(claim)
        // claimService.delete shows the wilderness indicator itself; the listener must not follow it with the
        // faction's territory title for a chunk that is no longer claimed.
        assertTrue(syncTasks.isEmpty(), "No territory title should be scheduled after an autounclaim")
    }

    @Test
    fun onPlayerMove_autounclaimOff_ownClaim_leavesTheClaim() {
        `when`(faction.autounclaim).thenReturn(false)
        val claim = stubClaimAtDestination(factionId)

        uut.onPlayerMove(event)
        runAsyncTasks()

        verify(claimService, never()).delete(claim)
    }

    @Test
    fun onPlayerMove_autounclaimOn_anotherFactionsClaim_leavesTheClaim() {
        `when`(faction.autounclaim).thenReturn(true)
        val claim = stubClaimAtDestination(MfFactionId.generate())

        uut.onPlayerMove(event)
        runAsyncTasks()

        verify(claimService, never()).delete(claim)
    }

    @Test
    fun onPlayerMove_autounclaimOn_roleWithoutUnclaim_leavesTheClaim() {
        `when`(faction.autounclaim).thenReturn(true)
        `when`(role.getPermissionValue(unclaim)).thenReturn(false)
        val claim = stubClaimAtDestination(factionId)

        uut.onPlayerMove(event)
        runAsyncTasks()

        verify(claimService, never()).delete(claim)
    }

    @Test
    fun onPlayerMove_autounclaimOn_playerWithoutMfUnclaim_leavesTheClaim() {
        `when`(faction.autounclaim).thenReturn(true)
        `when`(player.hasPermission("mf.unclaim")).thenReturn(false)
        val claim = stubClaimAtDestination(factionId)

        uut.onPlayerMove(event)
        runAsyncTasks()

        verify(claimService, never()).delete(claim)
    }

    private fun stubClaimAtDestination(owningFactionId: MfFactionId): MfClaimedChunk {
        val claim = MfClaimedChunk(worldId, 1, 0, owningFactionId)
        `when`(claimService.getClaim(toChunk)).thenReturn(claim)
        `when`(claimService.getClaim(fromChunk)).thenReturn(null)
        `when`(claimService.delete(claim)).thenReturn(Success(Unit))
        val owningFaction = if (owningFactionId == factionId) faction else mock(MfFaction::class.java)
        `when`(factionService.getFaction(owningFactionId)).thenReturn(owningFaction)
        return claim
    }

    private fun runAsyncTasks() {
        while (asyncTasks.isNotEmpty()) {
            asyncTasks.removeAt(0).run()
        }
    }

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)
        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
        faction = mock(MfFaction::class.java)
        `when`(faction.id).thenReturn(factionId)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        // Async work (the listener body) is queued and drained by the test. Main-thread work (the territory title) is
        // only recorded, so a test can assert whether it was scheduled without having to stub the title rendering.
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            asyncTasks += invocation.arguments[1] as Runnable
            null
        }
        `when`(scheduler.runTask(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            syncTasks += invocation.arguments[1] as Runnable
            null
        }
    }
}
