package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class BlockPlaceListenerTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: BlockPlaceListenerTestFixture
    private lateinit var medievalFactions: MedievalFactions
    private lateinit var gateService: MfGateService
    private lateinit var claimService: MfClaimService
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var uut: BlockPlaceListener

    @BeforeEach
    fun setUp() {
        fixture = createBasicFixture()
        medievalFactions = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        uut = BlockPlaceListener(medievalFactions)
    }

    @Test
    fun onBlockPlace_BlockIsInGate_ShouldCancelAndInformPlayer() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val blockPosition = MfBlockPosition.fromBukkitBlock(block)
        `when`(gateService.getGatesAt(blockPosition)).thenReturn(listOf(mock(MfGate::class.java)))

        // Act
        uut.onBlockPlace(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}Cannot place block in gate")
    }

    @Test
    fun onBlockPlace_BlockInWilderness_WildernessPreventBlockPlaceSetToTrue_ShouldCancelAndInformPlayer() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("wilderness.place.prevent", false)).thenReturn(true)
        `when`(medievalFactions.config.getBoolean("wilderness.place.alert", true)).thenReturn(true)

        // Act
        uut.onBlockPlace(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}Cannot place block in wilderness")
    }

    @Test
    fun onBlockPlace_BlockInWilderness_WildernessPreventBlockPlaceSetToFalse_ShouldReturn() {
        // Arrange
        val block = fixture.block
        val event = fixture.event

        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("wilderness.place.prevent", false)).thenReturn(false)

        // Act
        uut.onBlockPlace(event)

        // Assert
        verify(event, never()).isCancelled = true
        verify(event, never()).isCancelled = false
    }

    @Test
    fun onBlockPlace_Claimed_AtWar_WartimePlaceableBlock_ShouldAllow() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val playerId = UUID.randomUUID()
        `when`(player.uniqueId).thenReturn(playerId)

        val claimFactionId = MfFactionId("claim-faction")
        val claim = mock(MfClaimedChunk::class.java)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)

        val claimFaction = mock(MfFaction::class.java)
        `when`(claimFaction.id).thenReturn(claimFactionId)
        `when`(claimFaction.name).thenReturn("Enemy Faction")
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)

        val mfPlayer = mock(MfPlayer::class.java)
        val mfPlayerId = MfPlayerId(playerId.toString())
        `when`(mfPlayer.id).thenReturn(mfPlayerId)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)

        val playerFactionId = MfFactionId("player-faction")
        val playerFaction = mock(MfFaction::class.java)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(factionService.getFaction(mfPlayerId)).thenReturn(playerFaction)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(listOf(claimFactionId))

        `when`(claimService.isInteractionAllowed(mfPlayerId, claim)).thenReturn(false)
        `when`(claimService.isWartimePlaceableBlock(mfPlayerId, claim, block.type)).thenReturn(true)

        // Act
        uut.onBlockPlace(event)

        // Assert - event should NOT be cancelled because block is in wartime placeable list
        verify(event, never()).isCancelled = true
    }

    // Helper functions

    private fun createBasicFixture(): BlockPlaceListenerTestFixture {
        val world = testUtils.createMockWorld()
        val block = testUtils.createMockBlock(world)
        val player = mock(Player::class.java)
        val event = testUtils.createBlockPlaceEvent(block, player)
        return BlockPlaceListenerTestFixture(world, block, player, event)
    }

    private data class BlockPlaceListenerTestFixture(
        val world: World,
        val block: Block,
        val player: Player,
        val event: BlockPlaceEvent
    )

    private fun mockServices() {
        gateService = mock(MfGateService::class.java)
        claimService = mock(MfClaimService::class.java)
        factionService = mock(MfFactionService::class.java)
        playerService = mock(MfPlayerService::class.java)
        relationshipService = mock(MfFactionRelationshipService::class.java)

        val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.gateService).thenReturn(gateService)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.factionRelationshipService).thenReturn(relationshipService)
    }

    private fun mockLanguageSystem() {
        val language = mock(Language::class.java)
        `when`(language["CannotPlaceBlockInGate"]).thenReturn("Cannot place block in gate")
        `when`(language["CannotPlaceBlockInWilderness"]).thenReturn("Cannot place block in wilderness")
        `when`(medievalFactions.language).thenReturn(language)
    }
}
