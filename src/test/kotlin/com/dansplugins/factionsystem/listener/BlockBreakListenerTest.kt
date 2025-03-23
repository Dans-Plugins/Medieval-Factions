package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
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
    private lateinit var fixture: BlockBreakListenerTestFixture

    private lateinit var medievalFactions: MedievalFactions

    private lateinit var gateService: MfGateService
    private lateinit var claimService: MfClaimService
    private lateinit var lockService: MfLockService
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService

    private lateinit var blockBreakListener: BlockBreakListener

    @BeforeEach
    fun setUp() {
        fixture = createBasicFixture()
        medievalFactions = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        mockScheduler()
        blockBreakListener = BlockBreakListener(medievalFactions)
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
        blockBreakListener.onBlockBreak(event)

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
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("wilderness.preventBlockBreak", false)).thenReturn(true)

        // Act
        blockBreakListener.onBlockBreak(event)

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
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("wilderness.preventBlockBreak", false)).thenReturn(false)

        // Act
        blockBreakListener.onBlockBreak(event)

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
            playerService.save(com.dansplugins.factionsystem.player.MfPlayer(medievalFactions, player))
        }
        `when`(
            medievalFactions.server.scheduler.runTaskAsynchronously(
                medievalFactions,
                runnable
            )
        ).thenReturn(mock(org.bukkit.scheduler.BukkitTask::class.java))

        // Act
        blockBreakListener.onBlockBreak(event)

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
        blockBreakListener.onBlockBreak(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}Cannot break block in faction territory")
    }

    // Helper functions

    private fun createMockBlock(world: World = mock(World::class.java)): Block {
        val block = mock(Block::class.java)
        `when`(block.world).thenReturn(world)
        `when`(block.x).thenReturn(0)
        `when`(block.y).thenReturn(0)
        `when`(block.z).thenReturn(0)
        `when`(block.chunk).thenReturn(mock(org.bukkit.Chunk::class.java))
        return block
    }

    private fun createMockWorld(): World {
        val world = mock(World::class.java)
        val worldUid = mock(java.util.UUID::class.java)
        `when`(world.uid).thenReturn(worldUid)
        return world
    }

    private fun createBlockBreakEvent(block: Block, player: Player): BlockBreakEvent {
        val event = mock(BlockBreakEvent::class.java)
        `when`(event.block).thenReturn(block)
        `when`(event.player).thenReturn(player)
        return event
    }

    private fun createBasicFixture(): BlockBreakListenerTestFixture {
        val world = createMockWorld()
        val block = createMockBlock(world)
        val player = mock(Player::class.java)
        val event = createBlockBreakEvent(block, player)
        return BlockBreakListenerTestFixture(world, block, player, event)
    }

    private fun mockServices() {
        gateService = mock(MfGateService::class.java)
        claimService = mock(MfClaimService::class.java)
        lockService = mock(MfLockService::class.java)
        factionService = mock(MfFactionService::class.java)
        playerService = mock(MfPlayerService::class.java)

        val services = mock(Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.gateService).thenReturn(gateService)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.lockService).thenReturn(lockService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.playerService).thenReturn(playerService)
    }

    private fun mockLanguageSystem() {
        val language = mock(Language::class.java)
        `when`(language["CannotBreakBlockInGate"]).thenReturn("Cannot break block in gate")
        `when`(language["CannotBreakBlockInWilderness"]).thenReturn("Cannot break block in wilderness")
        `when`(language["CannotBreakBlockInFactionTerritory", "test"]).thenReturn("Cannot break block in faction territory")
        `when`(medievalFactions.language).thenReturn(language)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        val scheduler = mock(BukkitScheduler::class.java)
        `when`(medievalFactions.server).thenReturn(server)
        `when`(server.scheduler).thenReturn(scheduler)
    }

    private data class BlockBreakListenerTestFixture(
        val world: World,
        val block: Block,
        val player: Player,
        val event: BlockBreakEvent
    )

}
