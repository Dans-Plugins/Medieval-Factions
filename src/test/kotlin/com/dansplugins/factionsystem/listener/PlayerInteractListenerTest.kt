
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
import dev.forkhandles.result4k.Success
import org.bukkit.Material
import org.bukkit.Server
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
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.*
import org.bukkit.block.data.type.Gate as FenceGateData

class PlayerInteractListenerTest {
    companion object {
        private lateinit var mockedBukkit: org.mockito.MockedStatic<org.bukkit.Bukkit>

        @org.junit.jupiter.api.BeforeAll
        @JvmStatic
        fun setUpBukkit() {
            mockedBukkit = org.mockito.Mockito.mockStatic(org.bukkit.Bukkit::class.java) { invocation ->
                if (invocation.method.name == "getRegistry") {
                    java.lang.reflect.Proxy.newProxyInstance(
                        org.bukkit.Registry::class.java.classLoader,
                        arrayOf(org.bukkit.Registry::class.java),
                        java.lang.reflect.InvocationHandler { _, m, _ ->
                            if (m.name == "iterator") {
                                ArrayList<org.bukkit.Keyed>().iterator()
                            } else {
                                null
                            }
                        }
                    )
                } else {
                    org.mockito.Mockito.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        }

        @org.junit.jupiter.api.AfterAll
        @JvmStatic
        fun tearDownBukkit() {
            mockedBukkit.close()
        }
    }

    private val testUtils = TestUtils()

    private lateinit var fixture: PlayerInteractListenerTestFixture
    private lateinit var medievalFactions: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var interactionService: MfInteractionService
    private lateinit var lockService: MfLockService
    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var factionService: com.dansplugins.factionsystem.faction.MfFactionService
    private lateinit var scheduler: BukkitScheduler
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
        mockScheduler()
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
    fun onPlayerInteract_FenceGateWithNonMembersCanInteractWithDoorsEnabled_ShouldAllowInteraction() {
        // Arrange
        mockBlockData<FenceGateData>()
        setupConfigForDoorInteraction(enabled = true)
        setupPlayerMocks(fixture.player)
        setupClaimAndFaction(fixture.block)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because fence gates are allowed
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_FenceGateWithNonMembersCanInteractWithDoorsDisabled_ShouldBlockInteraction() {
        // Arrange
        mockBlockData<FenceGateData>()
        setupConfigForDoorInteraction(enabled = false)
        val (_, playerId) = setupPlayerMocks(fixture.player, bypassEnabled = false)
        val (claim, _) = setupClaimAndFaction(fixture.block)

        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(fixture.player.hasPermission("mf.bypass")).thenReturn(false)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should be cancelled because fence gates are NOT allowed and interaction is not allowed
        verifyEventCancelled()
        verifyPlayerNotified()
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

        // Assert - event should NOT be cancelled because player has bypass enabled, and the bypass
        // notice is sent once for the deliberate right-click
        verifyEventNotCancelled()
        runScheduledAsyncTasks()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_LockedBlock_NonOwnerWithoutBypass_ShouldCancelAndNotifyPlayer() {
        // Arrange
        mockBlockData<Door>()
        setupConfigForDoorInteraction(enabled = false)
        setupPlayerMocks(fixture.player, bypassEnabled = false)

        // Create a locked block owned by a different player
        setupLockedBlock(ownedByPlayer = false, playerIsAccessor = false)

        `when`(fixture.player.hasPermission("mf.bypass")).thenReturn(false)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - lock protection applies and the player is told why
        verifyEventCancelled()
        runScheduledAsyncTasks()
        verifyPlayerNotified()
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

        // Assert - event should NOT be cancelled because player is an accessor, and no notice is sent
        verifyEventNotCancelled()
        verifyNoAsyncTaskScheduled()
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
    fun onPlayerInteract_LadderInHand_ClickingChest_ShouldBlockInteraction() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        // Set up an interactable block (chest)
        val chestMaterial = mock(Material::class.java)
        `when`(chestMaterial.isSolid).thenReturn(true)
        `when`(chestMaterial.isInteractable).thenReturn(true) // Chest is interactable
        `when`(block.type).thenReturn(chestMaterial)

        val blockData = mock(org.bukkit.block.data.BlockData::class.java)
        `when`(block.blockData).thenReturn(blockData)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val claim = mock(MfClaimedChunk::class.java)
        val item = mock(ItemStack::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.item).thenReturn(item)
        val itemMaterial = mock(Material::class.java)
        `when`(itemMaterial.isEdible).thenReturn(false)
        `when`(item.type).thenReturn(itemMaterial)
        `when`(event.hasItem()).thenReturn(true)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(mock(MfFaction::class.java))
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)

        // isPlacingLadder should be false because block is interactable
        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, false)).thenReturn(false)

        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))

        // Act
        uut.onPlayerInteract(event)

        // Assert - event should be cancelled because clicking an interactable block with ladder shouldn't bypass
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_LadderInHand_LeftClickBlock_ShouldNotBypass() {
        // Arrange
        setupWartimeLadderTest(
            ladderItem = true,
            isWartimeLadderPlacementAllowed = false,
            configEnabled = true,
            atWarWithClaimFaction = true
        )

        // Override action to LEFT_CLICK_BLOCK
        `when`(fixture.event.action).thenReturn(Action.LEFT_CLICK_BLOCK)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - left click with ladder should not bypass protection
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_LadderInHand_PhysicalAction_ShouldNotBypass() {
        // Arrange
        setupWartimeLadderTest(
            ladderItem = true,
            isWartimeLadderPlacementAllowed = false,
            configEnabled = true,
            atWarWithClaimFaction = true
        )

        // Override action to PHYSICAL (like pressure plate)
        `when`(fixture.event.action).thenReturn(Action.PHYSICAL)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - physical action with ladder should not bypass protection.
        // No message is sent because physical interactions repeat every tick (see #1957).
        verifyEventCancelled()
        verifyPlayerNotNotified()
    }

    @Test
    fun onPlayerInteract_WartimeInteractableBlock_ShouldAllowInteraction() {
        // Arrange
        setupWartimeLadderTest(
            ladderItem = false,
            isWartimeLadderPlacementAllowed = false,
            configEnabled = false,
            atWarWithClaimFaction = true
        )

        val block = fixture.block
        val playerId = MfPlayerId(fixture.player.uniqueId.toString())
        val claim = claimService.getClaim(block.chunk)!!

        `when`(claimService.isWartimeInteractableBlock(playerId, claim, block.type)).thenReturn(true)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because block is in wartime interactable list
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_WartimeBreakableBlock_LeftClick_ShouldAllow() {
        // Arrange
        setupWartimeLadderTest(
            ladderItem = false,
            isWartimeLadderPlacementAllowed = false,
            configEnabled = false,
            atWarWithClaimFaction = true
        )

        val event = fixture.event
        `when`(event.action).thenReturn(Action.LEFT_CLICK_BLOCK)
        `when`(event.hasItem()).thenReturn(false)

        val block = fixture.block
        val playerId = MfPlayerId(fixture.player.uniqueId.toString())
        val claim = claimService.getClaim(block.chunk)!!

        `when`(claimService.isWartimeInteractableBlock(playerId, claim, block.type)).thenReturn(false)
        `when`(claimService.isWartimeBreakableBlock(playerId, claim, block.type)).thenReturn(true)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because block is in wartime breakable list
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_WartimePlaceableBlock_RightClick_ShouldAllow() {
        // Arrange
        setupWartimeLadderTest(
            ladderItem = false,
            isWartimeLadderPlacementAllowed = false,
            configEnabled = false,
            atWarWithClaimFaction = true
        )

        val event = fixture.event
        val item = mock(ItemStack::class.java)
        val itemMaterial = mock(Material::class.java)
        `when`(itemMaterial.isEdible).thenReturn(false)
        `when`(item.type).thenReturn(itemMaterial)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.hasItem()).thenReturn(true)
        `when`(event.item).thenReturn(item)

        val block = fixture.block
        val playerId = MfPlayerId(fixture.player.uniqueId.toString())
        val claim = claimService.getClaim(block.chunk)!!

        `when`(claimService.isWartimeInteractableBlock(playerId, claim, block.type)).thenReturn(false)
        `when`(claimService.isWartimePlaceableBlock(playerId, claim, itemMaterial)).thenReturn(true)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should NOT be cancelled because item in hand is in wartime placeable list
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_WartimePlaceableItemHeld_ClickingInteractableBlock_ShouldBlockInteraction() {
        // Regression test: holding an item in wartimePlaceableBlocks must NOT bypass interaction
        // protection for interactive blocks such as chests and levers.
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        // Clicked block is interactable (chest, lever, etc.)
        val interactableMaterial = mock(Material::class.java)
        `when`(interactableMaterial.isSolid).thenReturn(true)
        `when`(interactableMaterial.isInteractable).thenReturn(true)
        `when`(block.type).thenReturn(interactableMaterial)

        val blockData = mock(org.bukkit.block.data.BlockData::class.java)
        `when`(block.blockData).thenReturn(blockData)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)
        val item = mock(ItemStack::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.item).thenReturn(item)
        val itemMaterial = mock(Material::class.java)
        `when`(itemMaterial.isEdible).thenReturn(false)
        `when`(item.type).thenReturn(itemMaterial)
        `when`(event.hasItem()).thenReturn(true)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Enemy Faction")
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(listOf(claimFactionId))
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))

        // isPlacingLadder is false because block is interactable
        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, false)).thenReturn(false)
        `when`(claimService.isWartimeInteractableBlock(playerId, claim, interactableMaterial)).thenReturn(false)
        `when`(claimService.isWartimePlaceableBlock(playerId, claim, itemMaterial)).thenReturn(true)

        // Act
        uut.onPlayerInteract(event)

        // Assert — interaction must be blocked; a placeable item in hand must not unlock interactive blocks
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_WartimePlaceableItemHeld_PlacingOnNonInteractableBlock_ShouldAllow() {
        // Placement on a solid, non-interactive surface must still be allowed when the item is
        // in wartimePlaceableBlocks — this is the intended use-case.
        // Arrange
        setupWartimeLadderTest(
            ladderItem = false,
            isWartimeLadderPlacementAllowed = false,
            configEnabled = false,
            atWarWithClaimFaction = true
        )

        val event = fixture.event
        val item = mock(ItemStack::class.java)
        val itemMaterial = mock(Material::class.java)
        `when`(itemMaterial.isEdible).thenReturn(false)
        `when`(item.type).thenReturn(itemMaterial)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.hasItem()).thenReturn(true)
        `when`(event.item).thenReturn(item)

        val block = fixture.block
        val playerId = MfPlayerId(fixture.player.uniqueId.toString())
        val claim = claimService.getClaim(block.chunk)!!

        `when`(claimService.isWartimeInteractableBlock(playerId, claim, block.type)).thenReturn(false)
        `when`(claimService.isWartimePlaceableBlock(playerId, claim, itemMaterial)).thenReturn(true)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert — placement against a non-interactable surface must be allowed
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_NonListedItemHeld_ClickingInteractableBlock_ShouldBlockInteraction() {
        // A held item that does NOT appear in any wartime list must not change whether the player
        // can interact with blocks in enemy territory.
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val interactableMaterial = mock(Material::class.java)
        `when`(interactableMaterial.isSolid).thenReturn(true)
        `when`(interactableMaterial.isInteractable).thenReturn(true)
        `when`(block.type).thenReturn(interactableMaterial)

        val blockData = mock(org.bukkit.block.data.BlockData::class.java)
        `when`(block.blockData).thenReturn(blockData)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)
        val item = mock(ItemStack::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.item).thenReturn(item)
        val itemMaterial = mock(Material::class.java)
        `when`(itemMaterial.isEdible).thenReturn(false)
        `when`(item.type).thenReturn(itemMaterial)
        `when`(event.hasItem()).thenReturn(true)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Enemy Faction")
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(listOf(claimFactionId))
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))

        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, false)).thenReturn(false)
        `when`(claimService.isWartimeInteractableBlock(playerId, claim, interactableMaterial)).thenReturn(false)
        `when`(claimService.isWartimePlaceableBlock(playerId, claim, itemMaterial)).thenReturn(false)

        // Act
        uut.onPlayerInteract(event)

        // Assert — non-listed item must not grant any interaction bypass
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    // --- Wartime list edge-case tests ---

    @Test
    fun onPlayerInteract_AtWar_ChestInWartimeInteractableBlocks_ShouldAllowInteraction() {
        // Operator explicitly added CHEST to wartimeInteractableBlocks — interaction must be permitted.
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val chestMaterial = mock(Material::class.java)
        `when`(chestMaterial.isSolid).thenReturn(true)
        `when`(chestMaterial.isInteractable).thenReturn(true)
        `when`(block.type).thenReturn(chestMaterial)
        `when`(block.blockData).thenReturn(mock(org.bukkit.block.data.BlockData::class.java))

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.hasItem()).thenReturn(false)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Enemy Faction")
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(listOf(claimFactionId))
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, false)).thenReturn(false)
        // CHEST is in wartimeInteractableBlocks → must be allowed
        `when`(claimService.isWartimeInteractableBlock(playerId, claim, chestMaterial)).thenReturn(true)

        // Act
        uut.onPlayerInteract(event)

        // Assert — interaction must be permitted because operator allowed this block type
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_AtWar_ChestInWartimePlaceableBlocksOnly_ShouldBlockInteraction() {
        // CHEST is only in wartimePlaceableBlocks, not in wartimeInteractableBlocks.
        // The two lists are fully independent — placeable must never unlock interaction.
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val chestMaterial = mock(Material::class.java)
        `when`(chestMaterial.isSolid).thenReturn(true)
        `when`(chestMaterial.isInteractable).thenReturn(true)
        `when`(block.type).thenReturn(chestMaterial)
        `when`(block.blockData).thenReturn(mock(org.bukkit.block.data.BlockData::class.java))

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)
        val item = mock(ItemStack::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.item).thenReturn(item)
        val itemMaterial = mock(Material::class.java)
        `when`(itemMaterial.isEdible).thenReturn(false)
        `when`(item.type).thenReturn(itemMaterial)
        `when`(event.hasItem()).thenReturn(true)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Enemy Faction")
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(listOf(claimFactionId))
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, false)).thenReturn(false)
        // NOT in interactable list
        `when`(claimService.isWartimeInteractableBlock(playerId, claim, chestMaterial)).thenReturn(false)
        // IS in placeable list — but clicked block is interactable so this must not apply
        `when`(claimService.isWartimePlaceableBlock(playerId, claim, itemMaterial)).thenReturn(true)

        // Act
        uut.onPlayerInteract(event)

        // Assert — interaction must be blocked; placeable list must not affect interactive blocks
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_AtWar_LeverInWartimeInteractableBlocks_ShouldAllowInteraction() {
        // Operator explicitly added LEVER to wartimeInteractableBlocks to allow siege redstone.
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val leverMaterial = mock(Material::class.java)
        `when`(leverMaterial.isSolid).thenReturn(true)
        `when`(leverMaterial.isInteractable).thenReturn(true)
        `when`(block.type).thenReturn(leverMaterial)
        `when`(block.blockData).thenReturn(mock(org.bukkit.block.data.BlockData::class.java))

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.hasItem()).thenReturn(false)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Enemy Faction")
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(listOf(claimFactionId))
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, false)).thenReturn(false)
        // LEVER is in wartimeInteractableBlocks → must be allowed
        `when`(claimService.isWartimeInteractableBlock(playerId, claim, leverMaterial)).thenReturn(true)

        // Act
        uut.onPlayerInteract(event)

        // Assert — lever interaction must be permitted because it is in the interactable list
        verifyEventNotCancelled()
    }

    @Test
    fun onPlayerInteract_AtWar_NoWartimeListsConfigured_ShouldBlockInteraction() {
        // Default configuration: all wartime lists empty. No interaction should be permitted.
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val interactableMaterial = mock(Material::class.java)
        `when`(interactableMaterial.isSolid).thenReturn(true)
        `when`(interactableMaterial.isInteractable).thenReturn(true)
        `when`(block.type).thenReturn(interactableMaterial)
        `when`(block.blockData).thenReturn(mock(org.bukkit.block.data.BlockData::class.java))

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.hasItem()).thenReturn(false)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Enemy Faction")
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(listOf(claimFactionId))
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, false)).thenReturn(false)
        // All wartime lists return false (empty config)
        `when`(claimService.isWartimeInteractableBlock(playerId, claim, interactableMaterial)).thenReturn(false)

        // Act
        uut.onPlayerInteract(event)

        // Assert — baseline: empty wartime lists must never permit interaction
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_NotAtWar_ChestInWartimeInteractableBlocks_AlliesCanInteractFalse_ShouldBlockInteraction() {
        // Wartime lists must only apply when factions are actually at war. A non-war player
        // must not benefit from wartime permissions (e.g. allied territory with alliesCanInteractWithLand=false).
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val chestMaterial = mock(Material::class.java)
        `when`(chestMaterial.isSolid).thenReturn(true)
        `when`(chestMaterial.isInteractable).thenReturn(true)
        `when`(block.type).thenReturn(chestMaterial)
        `when`(block.blockData).thenReturn(mock(org.bukkit.block.data.BlockData::class.java))

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.hasItem()).thenReturn(false)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(claimService.getClaim(block.chunk)).thenReturn(claim)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)
        `when`(claimFaction.name).thenReturn("Ally Faction")
        // isInteractionAllowed=false models alliesCanInteractWithLand=false
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(factionService.getFaction(playerId)).thenReturn(playerFaction)
        `when`(playerFaction.id).thenReturn(playerFactionId)
        // Not at war — wartime checks must not apply
        `when`(relationshipService.getFactionsAtWarWith(playerFactionId)).thenReturn(emptyList())
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, false)).thenReturn(false)
        // Even though CHEST appears in wartimeInteractableBlocks, it must not help a non-war player
        `when`(claimService.isWartimeInteractableBlock(playerId, claim, chestMaterial)).thenReturn(false)

        // Act
        uut.onPlayerInteract(event)

        // Assert — non-war player must be blocked regardless of wartime list contents
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_BypassModeEnabled_AllWartimeListsEmpty_ShouldAllowInteraction() {
        // Bypass mode must override all wartime restrictions. A player with bypass enabled and
        // the mf.bypass permission can interact with any block in enemy territory.
        // Arrange
        mockBlockData<Door>()
        setupConfigForDoorInteraction(enabled = false)
        val (_, playerId) = setupPlayerMocks(fixture.player, bypassEnabled = true)
        val (claim, _) = setupClaimAndFaction(fixture.block)

        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(fixture.player.hasPermission("mf.bypass")).thenReturn(true)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert — bypass mode must allow interaction regardless of wartime list configuration
        verifyEventNotCancelled()
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
        val playerId = MfPlayerId(player.uniqueId.toString())
        `when`(mfPlayer.id).thenReturn(playerId)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
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

    // --- Physical interaction tests ---

    @Test
    fun onPlayerInteract_PhysicalActionInFactionTerritory_ShouldCancelWithoutNotifyingPlayer() {
        // Regression test for #1957: a pressure plate fires a PHYSICAL interaction on every tick the
        // player stands on it, so notifying on each one floods chat. The interaction must still be
        // cancelled - only the message is suppressed.
        // Arrange
        mockBlockData<BlockData>()
        setupConfigForDoorInteraction(enabled = false)
        val (_, playerId) = setupPlayerMocks(fixture.player)
        val (claim, _) = setupClaimAndFaction(fixture.block)

        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(fixture.event.action).thenReturn(Action.PHYSICAL)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - protection still applies, but the player is not spammed
        verifyEventCancelled()
        verifyPlayerNotNotified()
    }

    @Test
    fun onPlayerInteract_RightClickInFactionTerritory_ShouldCancelAndNotifyPlayer() {
        // Contrast case for #1957: a deliberate right-click happens once, so the player is still told
        // why the interaction was blocked.
        // Arrange
        mockBlockData<BlockData>()
        setupConfigForDoorInteraction(enabled = false)
        val (_, playerId) = setupPlayerMocks(fixture.player)
        val (claim, _) = setupClaimAndFaction(fixture.block)

        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(fixture.event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert
        verifyEventCancelled()
        verifyPlayerNotified()
    }

    @Test
    fun onPlayerInteract_PhysicalActionInWilderness_WildernessPreventInteractionSetToTrue_ShouldCancelWithoutNotifyingPlayer() {
        // The same per-tick flood applies to pressure plates in wilderness when
        // wilderness.interaction.prevent is enabled.
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val blockData = mock(BlockData::class.java)
        `when`(block.blockData).thenReturn(blockData)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        `when`(mfPlayer.id).thenReturn(playerId)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.PHYSICAL)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(medievalFactions.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("wilderness.interaction.prevent", false)).thenReturn(true)
        `when`(medievalFactions.config.getBoolean("wilderness.interaction.alert", true)).thenReturn(true)

        // Act
        uut.onPlayerInteract(event)

        // Assert
        verifyEventCancelled()
        verifyPlayerNotNotified()
    }

    @Test
    fun onPlayerInteract_PhysicalActionWithBypassEnabled_ShouldNotNotifyPlayer() {
        // The bypass notice is sent on every blocked-then-bypassed interaction, so it floods an
        // admin's chat for exactly the same reason.
        // Arrange
        mockBlockData<BlockData>()
        setupConfigForDoorInteraction(enabled = false)
        val (_, playerId) = setupPlayerMocks(fixture.player, bypassEnabled = true)
        val (claim, _) = setupClaimAndFaction(fixture.block)

        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)
        `when`(fixture.player.hasPermission("mf.bypass")).thenReturn(true)
        `when`(fixture.event.action).thenReturn(Action.PHYSICAL)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - bypass still applies, but without the per-tick notice
        verifyEventNotCancelled()
        verifyPlayerNotNotified()
    }

    @Test
    fun onPlayerInteract_PhysicalActionOnLockedBlock_ShouldCancelWithoutSchedulingOwnerLookup() {
        // Regression test for #1978: a locked pressure plate produced not just a message per tick, but a
        // scheduled async task and an owner lookup per tick. The lock protection itself must not change.
        // Arrange
        mockBlockData<BlockData>()
        setupConfigForDoorInteraction(enabled = false)
        setupPlayerMocks(fixture.player, bypassEnabled = false)
        setupLockedBlock(ownedByPlayer = false, playerIsAccessor = false)

        `when`(fixture.player.hasPermission("mf.bypass")).thenReturn(false)
        `when`(fixture.event.action).thenReturn(Action.PHYSICAL)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - still blocked, but no per-tick task, lookup or message
        verifyEventCancelled()
        verifyNoAsyncTaskScheduled()
        verifyPlayerNotNotified()
    }

    @Test
    fun onPlayerInteract_PhysicalActionOnLockedBlockWithBypassEnabled_ShouldNotScheduleOwnerLookup() {
        // The bypass notice on a locked block floods for the same reason as the blocked notice.
        // Arrange
        mockBlockData<BlockData>()
        setupConfigForDoorInteraction(enabled = false)
        setupPlayerMocks(fixture.player, bypassEnabled = true)
        setupLockedBlock(ownedByPlayer = false, playerIsAccessor = false)

        `when`(fixture.player.hasPermission("mf.bypass")).thenReturn(true)
        `when`(fixture.event.action).thenReturn(Action.PHYSICAL)

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - bypass still applies, without the per-tick task
        verifyEventNotCancelled()
        verifyNoAsyncTaskScheduled()
        verifyPlayerNotNotified()
    }

    @Test
    fun onPlayerInteract_PhysicalActionByUnregisteredPlayer_ShouldOnlyScheduleOneSavePerTick() {
        // Regression test for #1981: applyProtections schedules an async save when the player has no
        // MfPlayer record. Action.PHYSICAL fires once per tick for as long as the player stands on the
        // block, so without de-duplication every tick between the first dispatch and the save completing
        // queues another save. `playerService.getPlayer` returns null by default (unstubbed mock).
        // Arrange
        `when`(fixture.event.action).thenReturn(Action.PHYSICAL)

        // Act - two physical interactions land before the first save has run
        uut.onPlayerInteract(fixture.event)
        uut.onPlayerInteract(fixture.event)

        // Assert - both interactions are cancelled, but only one save is in flight
        verify(fixture.event, org.mockito.Mockito.times(2)).isCancelled = true
        verify(scheduler, org.mockito.Mockito.times(1)).runTaskAsynchronously(eq(medievalFactions), any(Runnable::class.java))
    }

    @Test
    fun onPlayerInteract_PhysicalActionByUnregisteredPlayer_ShouldScheduleAnotherSaveOnceThePriorOneCompletes() {
        // Once the in-flight save finishes (success or failure), the player is no longer "pending", so a
        // later physical interaction (e.g. next tick, if the record still isn't visible yet) may dispatch
        // another save.
        // Arrange
        `when`(fixture.event.action).thenReturn(Action.PHYSICAL)
        // MfPlayer(plugin, event.player) reads plugin.config.getDouble(...), so config must be mocked
        // before the scheduled save runnable is actually executed below.
        setupConfigForDoorInteraction(enabled = false)
        `when`(playerService.save(anyMfPlayer())).thenReturn(Success(mock(MfPlayer::class.java)))

        // Act
        uut.onPlayerInteract(fixture.event)
        runScheduledAsyncTasks()
        uut.onPlayerInteract(fixture.event)

        // Assert - the completed save's player id was released, so the second interaction schedules again
        verify(scheduler, org.mockito.Mockito.times(2)).runTaskAsynchronously(eq(medievalFactions), any(Runnable::class.java))
    }

    // Helper functions

    /**
     * Mockito's [ArgumentMatchers.any] returns null, which trips Kotlin's null-check on the
     * non-nullable [MfPlayer] parameter of [MfPlayerService.save] before the matcher is registered,
     * corrupting Mockito's matcher stack for subsequent tests. This generic indirection avoids the
     * compiler inserting that check.
     */
    private fun <T> anyMfPlayer(): T {
        ArgumentMatchers.any<MfPlayer>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

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

        // Mock the neighbouring block positions as well, since bisected blocks are looked up as a pair
        // with either their upper or their lower half.
        // The position must be resolved before the doReturn(...) call: reading the neighbour mock's
        // coordinates part-way through stubbing lockService raises UnfinishedStubbingException.
        val neighbourPositions = listOf(BlockFace.UP, BlockFace.DOWN)
            .map { face -> MfBlockPosition.fromBukkitBlock(fixture.block.getRelative(face)) }
        neighbourPositions.forEach { position ->
            doReturn(null).`when`(lockService).getLockedBlock(position)
        }
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

        // Mock a material that is explicitly solid and non-interactable for ladder placement logic
        val solidNonInteractableMaterial = mock(Material::class.java)
        `when`(solidNonInteractableMaterial.isSolid).thenReturn(true)
        `when`(solidNonInteractableMaterial.isInteractable).thenReturn(false)
        `when`(block.type).thenReturn(solidNonInteractableMaterial)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(player.uniqueId.toString())
        val playerFaction = mock(MfFaction::class.java)
        val claim = mock(MfClaimedChunk::class.java)
        val claimFaction = mock(MfFaction::class.java)
        val item = mock(ItemStack::class.java)

        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.item).thenReturn(item)
        val itemMaterial = if (ladderItem) {
            Material.LADDER
        } else {
            val mockMat = mock(Material::class.java)
            `when`(mockMat.isEdible).thenReturn(false)
            mockMat
        }
        `when`(item.type).thenReturn(itemMaterial)
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

        `when`(claimService.isWartimeLadderPlacementAllowed(playerId, claim, ladderItem)).thenReturn(isWartimeLadderPlacementAllowed)
    }

    private fun createBasicFixture(): PlayerInteractListenerTestFixture {
        val world = testUtils.createMockWorld()
        val block = testUtils.createMockBlock(world, 0, 0, 0)

        // Mock blocks for UP and DOWN directions. These must have distinct coordinates: MfBlockPosition
        // is a data class over (worldId, x, y, z), so neighbours left at the default (0, 0, 0) would
        // collapse onto the clicked block's position and overwrite its lockService stub.
        val blockAbove = testUtils.createMockBlock(world, 0, 1, 0)
        val blockBelow = testUtils.createMockBlock(world, 0, -1, 0)

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

    private fun verifyPlayerNotNotified() {
        verify(fixture.player, never()).sendMessage(any(String::class.java))
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
        `when`(language["UnknownPlayer"]).thenReturn("Unknown player")
        `when`(medievalFactions.language).thenReturn(language)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        scheduler = mock(BukkitScheduler::class.java)
        `when`(medievalFactions.server).thenReturn(server)
        `when`(server.scheduler).thenReturn(scheduler)
    }

    /**
     * Runs every [Runnable] that was dispatched to the async scheduler, so that the logic inside it is
     * actually exercised rather than only asserted to have been scheduled.
     */
    private fun runScheduledAsyncTasks() {
        val captor = ArgumentCaptor.forClass(Runnable::class.java)
        verify(scheduler, atLeast(0)).runTaskAsynchronously(eq(medievalFactions), captor.capture())
        captor.allValues.forEach { it.run() }
    }

    private fun verifyNoAsyncTaskScheduled() {
        verify(scheduler, never()).runTaskAsynchronously(eq(medievalFactions), any(Runnable::class.java))
    }
}
