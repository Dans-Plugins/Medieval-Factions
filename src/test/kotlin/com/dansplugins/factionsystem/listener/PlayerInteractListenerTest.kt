package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.interaction.MfInteractionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.type.Door
import org.bukkit.block.data.type.TrapDoor
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class PlayerInteractListenerTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: PlayerInteractListenerTestFixture
    private lateinit var medievalFactions: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var interactionService: MfInteractionService
    private lateinit var lockService: MfLockService
    private lateinit var uut: PlayerInteractListener

    @BeforeEach
    fun setUp() {
        fixture = createBasicFixture()
        medievalFactions = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        uut = PlayerInteractListener(medievalFactions)
    }

    @Test
    fun onPlayerInteract_DoorWithNonMembersCanInteractWithDoorsEnabled_ShouldAllowInteraction() {
        // Arrange
        val player = fixture.player
        val event = fixture.event
        val block = fixture.block
        val doorBlockData = mock(Door::class.java)

        `when`(block.blockData).thenReturn(doorBlockData)
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("factions.nonMembersCanInteractWithDoors")).thenReturn(true)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(lockService.getLockedBlock(any(com.dansplugins.factionsystem.area.MfBlockPosition::class.java))).thenReturn(null)

        val factionId = MfFactionId(UUID.randomUUID().toString())
        val claim = MfClaimedChunk(block.chunk, factionId)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)

        val factionService = medievalFactions.services.factionService
        val mockFaction = mock(com.dansplugins.factionsystem.faction.MfFaction::class.java)
        `when`(mockFaction.name).thenReturn("TestFaction")
        `when`(factionService.getFaction(factionId)).thenReturn(mockFaction)

        // Act
        uut.onPlayerInteract(event)

        // Assert - event should NOT be cancelled because doors are allowed
        verify(event, never()).isCancelled = true
    }

    @Test
    fun onPlayerInteract_TrapDoorWithNonMembersCanInteractWithDoorsEnabled_ShouldAllowInteraction() {
        // Arrange
        val player = fixture.player
        val event = fixture.event
        val block = fixture.block
        val trapDoorBlockData = mock(TrapDoor::class.java)

        `when`(block.blockData).thenReturn(trapDoorBlockData)
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("factions.nonMembersCanInteractWithDoors")).thenReturn(true)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(lockService.getLockedBlock(any(com.dansplugins.factionsystem.area.MfBlockPosition::class.java))).thenReturn(null)

        val factionId = MfFactionId(UUID.randomUUID().toString())
        val claim = MfClaimedChunk(block.chunk, factionId)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)

        val factionService = medievalFactions.services.factionService
        val mockFaction = mock(com.dansplugins.factionsystem.faction.MfFaction::class.java)
        `when`(mockFaction.name).thenReturn("TestFaction")
        `when`(factionService.getFaction(factionId)).thenReturn(mockFaction)

        // Act
        uut.onPlayerInteract(event)

        // Assert - event should NOT be cancelled because trapdoors are allowed
        verify(event, never()).isCancelled = true
    }

    @Test
    fun onPlayerInteract_DoorWithNonMembersCanInteractWithDoorsDisabled_ShouldBlockInteraction() {
        // Arrange
        val player = fixture.player
        val event = fixture.event
        val block = fixture.block
        val doorBlockData = mock(Door::class.java)

        `when`(block.blockData).thenReturn(doorBlockData)
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("factions.nonMembersCanInteractWithDoors")).thenReturn(false)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(lockService.getLockedBlock(any(com.dansplugins.factionsystem.area.MfBlockPosition::class.java))).thenReturn(null)

        val factionId = MfFactionId(UUID.randomUUID().toString())
        val claim = MfClaimedChunk(block.chunk, factionId)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(player.hasPermission("mf.bypass")).thenReturn(false)

        val factionService = medievalFactions.services.factionService
        val mockFaction = mock(com.dansplugins.factionsystem.faction.MfFaction::class.java)
        `when`(mockFaction.name).thenReturn("TestFaction")
        `when`(factionService.getFaction(factionId)).thenReturn(mockFaction)

        // Act
        uut.onPlayerInteract(event)

        // Assert - event should be cancelled because doors are NOT allowed and interaction is not allowed
        verify(event).isCancelled = true
        verify(player).sendMessage(any(String::class.java))
    }

    @Test
    fun onPlayerInteract_DoorWithNonMembersCanInteractWithDoorsDisabled_MemberCanStillInteract() {
        // Arrange
        val player = fixture.player
        val event = fixture.event
        val block = fixture.block
        val doorBlockData = mock(Door::class.java)

        `when`(block.blockData).thenReturn(doorBlockData)
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("factions.nonMembersCanInteractWithDoors")).thenReturn(false)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(lockService.getLockedBlock(any(com.dansplugins.factionsystem.area.MfBlockPosition::class.java))).thenReturn(null)

        val factionId = MfFactionId(UUID.randomUUID().toString())
        val claim = MfClaimedChunk(block.chunk, factionId)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(true)

        val factionService = medievalFactions.services.factionService
        val mockFaction = mock(com.dansplugins.factionsystem.faction.MfFaction::class.java)
        `when`(mockFaction.name).thenReturn("TestFaction")
        `when`(factionService.getFaction(factionId)).thenReturn(mockFaction)

        // Act
        uut.onPlayerInteract(event)

        // Assert - event should NOT be cancelled because player is allowed to interact
        verify(event, never()).isCancelled = true
    }

    // Helper functions

    private fun createBasicFixture(): PlayerInteractListenerTestFixture {
        val world = testUtils.createMockWorld()
        val block = testUtils.createMockBlock(world)
        val player = mock(Player::class.java)
        val playerId = UUID.randomUUID()
        `when`(player.uniqueId).thenReturn(playerId)

        val event = mock(PlayerInteractEvent::class.java)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.hand).thenReturn(EquipmentSlot.HAND)
        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.player).thenReturn(player)
        `when`(event.item).thenReturn(null)

        `when`(block.type).thenReturn(Material.OAK_DOOR)

        return PlayerInteractListenerTestFixture(world, block, player, event)
    }

    private data class PlayerInteractListenerTestFixture(
        val world: World,
        val block: Block,
        val player: Player,
        val event: PlayerInteractEvent
    )

    private fun mockServices() {
        playerService = mock(MfPlayerService::class.java)
        claimService = mock(MfClaimService::class.java)
        interactionService = mock(MfInteractionService::class.java)
        lockService = mock(MfLockService::class.java)

        val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.interactionService).thenReturn(interactionService)
        `when`(services.lockService).thenReturn(lockService)
        `when`(services.factionService).thenReturn(mock(com.dansplugins.factionsystem.faction.MfFactionService::class.java))
    }

    private fun mockLanguageSystem() {
        val language = mock(Language::class.java)
        `when`(language["CannotInteractWithBlockInFactionTerritory", anyString()]).thenReturn("Cannot interact with block in faction territory")
        `when`(medievalFactions.language).thenReturn(language)
    }
}
