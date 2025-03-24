package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.ChatColor
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlockBreakListenerTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: BlockBreakListenerTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var gateService: MfGateService
    private lateinit var claimService: MfClaimService
    private lateinit var lockService: MfLockService
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var uut: BlockBreakListener

    @BeforeEach
    fun setUp() {
        fixture = createFixture()
        plugin = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        mockScheduler()
        uut = BlockBreakListener(plugin)
    }

    @Test
    fun onBlockBreak_BlockIsInGate_ShouldCancelAndInformPlayer() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val blockPosition = MfBlockPosition.fromBukkitBlock(block)

        `when`(gateService.getGatesAt(blockPosition)).thenReturn(listOf(mock(MfGate::class.java)))

        // Act
        uut.onBlockBreak(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}Cannot break block in gate")
    }

    @Test
    fun onBlockBreak_BlockInWilderness_WildernessPreventBlockBreakSetToTrue_ShouldCancelAndInformPlayer() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(plugin.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(plugin.config.getBoolean("wilderness.break.prevent", false)).thenReturn(true)
        `when`(plugin.config.getBoolean("wilderness.break.alert", true)).thenReturn(true)

        // Act
        uut.onBlockBreak(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}Cannot break block in wilderness")
    }

    @Test
    fun onBlockBreak_BlockInWilderness_WildernessPreventBlockBreakSetToFalse_ShouldReturn() {
        // Arrange
        val block = fixture.block
        val event = fixture.event

        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(plugin.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(plugin.config.getBoolean("wilderness.break.prevent", false)).thenReturn(false)

        // Act
        uut.onBlockBreak(event)

        // Assert
        verify(event, never()).isCancelled = true
        verify(event, never()).isCancelled = false
    }

    @Test
    fun onBlockBreak_PlayerIsNotInDatabase_ShouldCancel() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val claim = mock(com.dansplugins.factionsystem.claim.MfClaimedChunk::class.java)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)

        val faction = mock(com.dansplugins.factionsystem.faction.MfFaction::class.java)
        `when`(factionService.getFaction(claim.factionId)).thenReturn(faction)

        val runnable = Runnable {
            playerService.save(com.dansplugins.factionsystem.player.MfPlayer(plugin, player))
        }
        `when`(
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                runnable
            )
        ).thenReturn(mock(org.bukkit.scheduler.BukkitTask::class.java))

        // Act
        uut.onBlockBreak(event)

        // Assert
        verify(event).isCancelled = true
    }

    @Test
    fun onBlockBreak_Claimed_InteractionNotAllowed_ShouldCancelAndInformPlayer() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val claim = mock(com.dansplugins.factionsystem.claim.MfClaimedChunk::class.java)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)

        val faction = mock(com.dansplugins.factionsystem.faction.MfFaction::class.java)
        `when`(factionService.getFaction(claim.factionId)).thenReturn(faction)
        `when`(faction.name).thenReturn("test")

        val mfPlayer = mock(com.dansplugins.factionsystem.player.MfPlayer::class.java)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)

        `when`(claimService.isInteractionAllowed(mfPlayer.id, claim)).thenReturn(false)

        // Act
        uut.onBlockBreak(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}Cannot break block in faction territory")
    }

    // Helper functions

    private fun createFixture(): BlockBreakListenerTestFixture {
        val world = testUtils.createMockWorld()
        val block = testUtils.createMockBlock(world)
        val player = mock(Player::class.java)
        val event = testUtils.createBlockBreakEvent(block, player)
        return BlockBreakListenerTestFixture(world, block, player, event)
    }

    private data class BlockBreakListenerTestFixture(
        val world: World,
        val block: Block,
        val player: Player,
        val event: BlockBreakEvent
    )

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        gateService = mock(MfGateService::class.java)
        `when`(services.gateService).thenReturn(gateService)

        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)

        lockService = mock(MfLockService::class.java)
        `when`(services.lockService).thenReturn(lockService)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
    }

    private fun mockLanguageSystem() {
        val language = mock(Language::class.java)
        `when`(language["CannotBreakBlockInGate"]).thenReturn("Cannot break block in gate")
        `when`(language["CannotBreakBlockInWilderness"]).thenReturn("Cannot break block in wilderness")
        `when`(language["CannotBreakBlockInFactionTerritory", "test"]).thenReturn("Cannot break block in faction territory")
        `when`(plugin.language).thenReturn(language)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        val scheduler = mock(BukkitScheduler::class.java)
        `when`(plugin.server).thenReturn(server)
        `when`(server.scheduler).thenReturn(scheduler)
    }
}
