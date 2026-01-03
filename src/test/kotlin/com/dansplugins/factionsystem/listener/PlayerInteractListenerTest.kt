
package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.interaction.MfInteractionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.locks.MfLockedBlock
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Door
import org.bukkit.block.data.type.TrapDoor
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.*

class PlayerInteractListenerTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: PlayerInteractListenerTestFixture
    private lateinit var medievalFactions: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var interactionService: MfInteractionService
    private lateinit var lockService: MfLockService
    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var factionService: com.dansplugins.factionsystem.faction.MfFactionService
    private lateinit var uut: PlayerInteractListener

    // Common test constants
    private val playerFactionId = MfFactionId("player-faction-id")
    private val claimFactionId = MfFactionId("claim-faction-id")

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
        mockBlockData<Door>()
        setupConfigForDoorInteraction(enabled = true)
        setupPlayerMocks(fixture.player)
        setupClaimAndFaction(fixture.block)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because doors are allowed
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_TrapDoorWithNonMembersCanInteractWithDoorsEnabled_ShouldAllowInteraction() {
        // Arrange
        mockBlockData<TrapDoor>()
        setupConfigForDoorInteraction(enabled = true)
        setupPlayerMocks(fixture.player)
        setupClaimAndFaction(fixture.block)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because trapdoors are allowed
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_DoorWithNonMembersCanInteractWithDoorsDisabled_ShouldBlockInteraction() {
        // Arrange
        mockBlockData<Door>()
        setupConfigForDoorInteraction(enabled = false)
        val (_, playerId) = setupPlayerMocks(fixture.player, bypassEnabled = false)
        val (claim, _) = setupClaimAndFaction(fixture.block)

        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(fixture.player.hasPermission("mf.bypass")).thenReturn(false)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should be cancelled because doors are NOT allowed and interaction is not allowed
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_DoorWithNonMembersCanInteractWithDoorsDisabled_MemberCanStillInteract() {
        // Arrange
        mockBlockData<Door>()
        setupConfigForDoorInteraction(enabled = false)
        val (_, playerId) = setupPlayerMocks(fixture.player)
        val (claim, _) = setupClaimAndFaction(fixture.block)

        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(true)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because player is allowed to interact
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_LockedBlock_NonOwnerWithBypassEnabled_ShouldAllowInteraction() {
        // Arrange
        mockBlockData<Door>()
        setupConfigForDoorInteraction(enabled = false)
        val (_, _) = setupPlayerMocks(fixture.player, bypassEnabled = true)

        // Create a locked block owned by a different player
        setupLockedBlock(ownedByPlayer = false, playerIsAccessor = false)

        // Player has bypass permission
        `when`(fixture.player.hasPermission("mf.bypass")).thenReturn(true)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because player has bypass enabled
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_LockedBlock_PlayerIsAccessor_ShouldAllowInteraction() {
        // Arrange
        mockBlockData<Door>()
        setupConfigForDoorInteraction(enabled = false)
        val (_, playerId) = setupPlayerMocks(fixture.player, bypassEnabled = false)

        // Create a locked block where player is an accessor
        setupLockedBlock(ownedByPlayer = false, playerIsAccessor = true, playerIdForAccess = playerId)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because player is an accessor
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_ClaimProtection_PlayerWithBypassEnabled_ShouldAllowInteraction() {
        // Arrange
        mockBlockData<Door>()
        setupConfigForDoorInteraction(enabled = false)
        val (_, playerId) = setupPlayerMocks(fixture.player, bypassEnabled = true)
        val (claim, _) = setupClaimAndFaction(fixture.block)

        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(fixture.player.hasPermission("mf.bypass")).thenReturn(true)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because player has bypass enabled with permission
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_DoorWithConfigEnabled_OverridesClaimProtection() {
        // Arrange
        mockBlockData<Door>()
        setupConfigForDoorInteraction(enabled = true)
        val (_, playerId) = setupPlayerMocks(fixture.player)
        val (claim, _) = setupClaimAndFaction(fixture.block)

        // Even though interaction is not allowed on the claim, door config should override
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because door config overrides claim protection
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_WartimeLadderPlacement_ConfigEnabled_ShouldAllow() {
        // Arrange
        setupWartimeLadderTest(
            ladderItem = true,
            isWartimeLadderPlacementAllowed = true,
            configEnabled = true,
            atWarWithClaimFaction = true
        )

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert
        verifyEventNotCancelled()
        verify(fixture.player, never()).sendMessage(any(String::class.java))
    }

    @Test
    fun onPlayerInteract_WartimeLadderPlacement_ConfigDisabled_ShouldCancel() {
        // Arrange
        setupWartimeLadderTest(
            ladderItem = true,
            isWartimeLadderPlacementAllowed = false,
            configEnabled = false,
            atWarWithClaimFaction = true
        )

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_WartimeNonLadderPlacement_ConfigEnabled_ShouldCancel() {
        // Arrange
        setupWartimeLadderTest(
            ladderItem = false,
            isWartimeLadderPlacementAllowed = false,
            configEnabled = true,
            atWarWithClaimFaction = true
        )

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_NoWarLadderPlacement_ConfigEnabled_ShouldCancel() {
        // Arrange
        setupWartimeLadderTest(
            ladderItem = true,
            isWartimeLadderPlacementAllowed = false,
            configEnabled = true,
            atWarWithClaimFaction = false
        )

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_BlockInWilderness_WildernessPreventInteractionSetToTrue_ShouldCancelAndInformPlayer() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val blockData = mock(org.bukkit.block.data.BlockData::class.java)
        `when`(block.blockData).thenReturn(blockData)

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
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    // Helper functions

    private inline fun <reified T> mockBlockData() {
        val blockData = mock(T::class.java)
        `when`(fixture.block.blockData).thenReturn(blockData as BlockData?)
    }

    private fun setupConfigForDoorInteraction(enabled: Boolean) {
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("factions.nonMembersCanInteractWithDoors")).thenReturn(enabled)
    }

    private fun setupPlayerMocks(player: Player, bypassEnabled: Boolean = false): Pair<MfPlayer, MfPlayerId> {
        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(bypassEnabled)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        val anyBlockPosition = mock(MfBlockPosition::class.java)
        `when`(lockService.getLockedBlock(anyBlockPosition)).thenReturn(null)
        return Pair(mfPlayer, playerId)
    }

    private fun setupLockedBlock(
        ownedByPlayer: Boolean = false,
        playerIsAccessor: Boolean = false,
        playerIdForAccess: MfPlayerId? = null
    ) {
        val ownerPlayerId = if (ownedByPlayer && playerIdForAccess != null) {
            playerIdForAccess
        } else {
            MfPlayerId(UUID.randomUUID().toString())
        }

        val accessors = if (playerIsAccessor && playerIdForAccess != null) {
            listOf(playerIdForAccess)
        } else {
            emptyList()
        }

        val blockPosition = MfBlockPosition.fromBukkitBlock(fixture.block)
        val lockedBlock = MfLockedBlock(
            block = blockPosition,
            chunkX = 0,
            chunkZ = 0,
            playerId = ownerPlayerId,
            accessors = accessors
        )

        // Mock the lockService to return our locked block for the specific block position
        doReturn(lockedBlock).`when`(lockService).getLockedBlock(blockPosition)

        // Mock the upward block position as well for bisected blocks
        val upBlock = fixture.block.getRelative(BlockFace.UP)
        val upBlockPosition = MfBlockPosition.fromBukkitBlock(upBlock)
        doReturn(null).`when`(lockService).getLockedBlock(upBlockPosition)
    }

    private fun setupClaimAndFaction(block: Block): Pair<MfClaimedChunk, MfFactionId> {
        // Create a faction ID first
        val factionId = MfFactionId(UUID.randomUUID().toString())

        // Create a properly mocked chunk
        val mockChunk = mock(org.bukkit.Chunk::class.java)
        val mockWorld = mock(org.bukkit.World::class.java)
        val worldId = UUID.randomUUID()

        // Set up the world UUID
        `when`(mockWorld.uid).thenReturn(worldId)

        // Set up the chunk to return our mock world
        `when`(mockChunk.world).thenReturn(mockWorld)
        `when`(mockChunk.x).thenReturn(0)
        `when`(mockChunk.z).thenReturn(0)

        // Mock the block's chunk to return our mock chunk
        `when`(block.chunk).thenReturn(mockChunk)

        // Create the claimed chunk with our mock chunk
        val claim = MfClaimedChunk(mockChunk, factionId)

        // Set up claim service to return our claim
        `when`(claimService.getClaim(mockChunk)).thenReturn(claim)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)

        // Set up faction service
        val factionService = medievalFactions.services.factionService
        val mockFaction = mock(com.dansplugins.factionsystem.faction.MfFaction::class.java)
        `when`(mockFaction.name).thenReturn("TestFaction")
        `when`(factionService.getFaction(factionId)).thenReturn(mockFaction)

        return Pair(claim, factionId)
    }


    private fun setupWartimeLadderTest(
        ladderItem: Boolean,
        isWartimeLadderPlacementAllowed: Boolean,
        configEnabled: Boolean,
        atWarWithClaimFaction: Boolean
    ) {
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val blockData = mock(org.bukkit.block.data.BlockData::class.java)
        `when`(block.blockData).thenReturn(blockData)
        `when`(block.type).thenReturn(Material.STONE) // Set a solid block type for ladder placement

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)
        val item = mock(ItemStack::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.item).thenReturn(item)
        `when`(item.type).thenReturn(if (ladderItem) Material.LADDER else Material.STONE)
        `when`(event.hasItem()).thenReturn(true)
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

        val warFactions = if (atWarWithClaimFaction) listOf(claimFactionId) else emptyList()
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(warFactions)

        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("factions.laddersPlaceableInEnemyFactionTerritory")).thenReturn(configEnabled)

        // Mock the isWartimeLadderPlacementAllowed with the actual parameter that will be used (isPlacingLadder)
        // isPlacingLadder is calculated as: event.hasItem() && event.item?.type == Material.LADDER && clickedBlock.type.isSolid
        val isPlacingLadder = ladderItem && block.type.isSolid
        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, isPlacingLadder)).thenReturn(isWartimeLadderPlacementAllowed)
    }

    private fun createBasicFixture(): PlayerInteractListenerTestFixture {
        val world = testUtils.createMockWorld()
        val block = testUtils.createMockBlock(world)

        // Mock blocks for UP and DOWN directions
        val blockAbove = testUtils.createMockBlock(world)
        val blockBelow = testUtils.createMockBlock(world)

        // Set up relative block retrieval
        `when`(block.getRelative(BlockFace.UP)).thenReturn(blockAbove)
        `when`(block.getRelative(BlockFace.DOWN)).thenReturn(blockBelow)

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

    private fun verifyEventCancelled() {
        verify(fixture.event).isCancelled = true
    }

    private fun verifyEventNotCancelled() {
        verify(fixture.event, never()).isCancelled = true
    }

    private fun verifyPlayerNotified() {
        verify(fixture.player).sendMessage(any(String::class.java))
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
        relationshipService = mock(MfFactionRelationshipService::class.java)
        factionService = mock(com.dansplugins.factionsystem.faction.MfFactionService::class.java)

        val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.interactionService).thenReturn(interactionService)
        `when`(services.lockService).thenReturn(lockService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.factionRelationshipService).thenReturn(relationshipService)
    }

    private fun mockLanguageSystem() {
        val language = mock(Language::class.java)
        `when`(language.get(anyString(), anyString())).thenReturn("Cannot interact with block in faction territory")
        `when`(medievalFactions.language).thenReturn(language)
    }
}
