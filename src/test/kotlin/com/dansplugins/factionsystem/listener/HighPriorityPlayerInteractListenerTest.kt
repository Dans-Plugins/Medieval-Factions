package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.*

class HighPriorityPlayerInteractListenerTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: HighPriorityPlayerInteractListenerTestFixture
    private lateinit var medievalFactions: MedievalFactions
    private lateinit var claimService: MfClaimService
    private lateinit var playerService: MfPlayerService
    private lateinit var factionService: MfFactionService
    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var uut: HighPriorityPlayerInteractListener

    @BeforeEach
    fun setUp() {
        fixture = createBasicFixture()
        medievalFactions = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        uut = HighPriorityPlayerInteractListener(medievalFactions)
    }

    @Test
    fun onPlayerInteract_WartimeLadderPlacement_ConfigEnabled_ShouldAllow() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event
        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val playerFactionId = MfFactionId("player-faction-id")
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)
        val claimFactionId = MfFactionId("claim-faction-id")
        val ladderItem = mock(ItemStack::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.item).thenReturn(ladderItem)
        `when`(ladderItem.type).thenReturn(Material.LADDER)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Enemy Faction")
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(listOf(claimFactionId))
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("factions.laddersPlaceableInEnemyFactionTerritory")).thenReturn(true)
        // Add this line to mock the wartime ladder placement check
        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, true)).thenReturn(true)

        // Act
        uut.onPlayerInteract(event)

        // Assert
        verify(event, never()).isCancelled = true
        verify(player, never()).sendMessage("${ChatColor.RED}You cannot interact with blocks in Enemy Faction territory.")
    }

    @Test
    fun onPlayerInteract_WartimeLadderPlacement_ConfigDisabled_ShouldCancel() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event
        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val playerFactionId = MfFactionId("player-faction-id")
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)
        val claimFactionId = MfFactionId("claim-faction-id")
        val ladderItem = mock(ItemStack::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.item).thenReturn(ladderItem)
        `when`(ladderItem.type).thenReturn(Material.LADDER)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Enemy Faction")
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(listOf(claimFactionId))
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("factions.laddersPlaceableInEnemyFactionTerritory")).thenReturn(false)

        // Act
        uut.onPlayerInteract(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}You cannot interact with blocks in Enemy Faction territory.")
    }

    @Test
    fun onPlayerInteract_WartimeNonLadderPlacement_ConfigEnabled_ShouldCancel() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event
        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val playerFactionId = MfFactionId("player-faction-id")
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)
        val claimFactionId = MfFactionId("claim-faction-id")
        val stoneItem = mock(ItemStack::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.item).thenReturn(stoneItem)
        `when`(stoneItem.type).thenReturn(Material.STONE)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Enemy Faction")
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(listOf(claimFactionId))
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("factions.laddersPlaceableInEnemyFactionTerritory")).thenReturn(true)

        // Act
        uut.onPlayerInteract(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}You cannot interact with blocks in Enemy Faction territory.")
    }

    @Test
    fun onPlayerInteract_NoWarLadderPlacement_ConfigEnabled_ShouldCancel() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event
        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val playerFactionId = MfFactionId("player-faction-id")
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)
        val claimFactionId = MfFactionId("claim-faction-id")
        val ladderItem = mock(ItemStack::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.item).thenReturn(ladderItem)
        `when`(ladderItem.type).thenReturn(Material.LADDER)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Other Faction")
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(emptyList()) // Not at war
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("factions.laddersPlaceableInEnemyFactionTerritory")).thenReturn(true)

        // Act
        uut.onPlayerInteract(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}You cannot interact with blocks in Other Faction territory.")
    }

    @Test
    fun onPlayerInteract_BlockInWilderness_WildernessPreventInteractionSetToTrue_ShouldCancelAndInformPlayer() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("wilderness.interaction.prevent", false)).thenReturn(true)
        `when`(medievalFactions.config.getBoolean("wilderness.interaction.alert", true)).thenReturn(true)

        // Act
        uut.onPlayerInteract(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}You cannot interact with blocks in the wilderness.")
    }

    // Helper functions

    private fun createBasicFixture(): HighPriorityPlayerInteractListenerTestFixture {
        val world = testUtils.createMockWorld()
        val block = testUtils.createMockBlock(world)
        val player = mock(Player::class.java)
        val playerId = UUID.randomUUID()
        `when`(player.uniqueId).thenReturn(playerId)
        `when`(player.hasPermission("mf.bypass")).thenReturn(false)
        val event = mock(PlayerInteractEvent::class.java)
        `when`(event.player).thenReturn(player)
        return HighPriorityPlayerInteractListenerTestFixture(world, block, player, event)
    }

    private data class HighPriorityPlayerInteractListenerTestFixture(
        val world: World,
        val block: Block,
        val player: Player,
        val event: PlayerInteractEvent
    )

    private fun mockServices() {
        claimService = mock(MfClaimService::class.java)
        playerService = mock(MfPlayerService::class.java)
        factionService = mock(MfFactionService::class.java)
        relationshipService = mock(MfFactionRelationshipService::class.java)

        val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.factionRelationshipService).thenReturn(relationshipService)
    }

    private fun mockLanguageSystem() {
        val language = mock(Language::class.java)
        `when`(language["CannotInteractBlockInWilderness"]).thenReturn("You cannot interact with blocks in the wilderness.")
        `when`(language["CannotInteractWithBlockInFactionTerritory", "Enemy Faction"]).thenReturn("You cannot interact with blocks in Enemy Faction territory.")
        `when`(language["CannotInteractWithBlockInFactionTerritory", "Other Faction"]).thenReturn("You cannot interact with blocks in Other Faction territory.")
        `when`(medievalFactions.language).thenReturn(language)
    }
}