package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.claim.MfClaim
import com.dansplugins.factionsystem.claim.MfClaimId
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.interaction.MfInteractionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
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
import java.util.UUID

class PlayerInteractListenerTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: PlayerInteractListenerTestFixture
    private lateinit var medievalFactions: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var factionService: MfFactionService
    private lateinit var interactionService: MfInteractionService
    private lateinit var uut: PlayerInteractListener

    @BeforeEach
    fun setUp() {
        medievalFactions = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        uut = PlayerInteractListener(medievalFactions)
    }

    @Test
    fun applyProtections_WithBowAndNonInteractableBlock_ShouldAllowInteraction() {
        // Arrange
        fixture = createFixtureWithProjectileWeapon(Material.BOW, isBlockInteractable = false)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should not be cancelled (projectile allowed)
        verify(fixture.event, never()).isCancelled = true
    }

    @Test
    fun applyProtections_WithCrossbowAndNonInteractableBlock_ShouldAllowInteraction() {
        // Arrange
        fixture = createFixtureWithProjectileWeapon(Material.CROSSBOW, isBlockInteractable = false)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should not be cancelled (projectile allowed)
        verify(fixture.event, never()).isCancelled = true
    }

    @Test
    fun applyProtections_WithTridentAndNonInteractableBlock_ShouldAllowInteraction() {
        // Arrange
        fixture = createFixtureWithProjectileWeapon(Material.TRIDENT, isBlockInteractable = false)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should not be cancelled (projectile allowed)
        verify(fixture.event, never()).isCancelled = true
    }

    @Test
    fun applyProtections_WithSnowballAndNonInteractableBlock_ShouldAllowInteraction() {
        // Arrange
        fixture = createFixtureWithProjectileWeapon(Material.SNOWBALL, isBlockInteractable = false)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should not be cancelled (projectile allowed)
        verify(fixture.event, never()).isCancelled = true
    }

    @Test
    fun applyProtections_WithEggAndNonInteractableBlock_ShouldAllowInteraction() {
        // Arrange
        fixture = createFixtureWithProjectileWeapon(Material.EGG, isBlockInteractable = false)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should not be cancelled (projectile allowed)
        verify(fixture.event, never()).isCancelled = true
    }

    @Test
    fun applyProtections_WithEnderPearlAndNonInteractableBlock_ShouldAllowInteraction() {
        // Arrange
        fixture = createFixtureWithProjectileWeapon(Material.ENDER_PEARL, isBlockInteractable = false)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should not be cancelled (projectile allowed)
        verify(fixture.event, never()).isCancelled = true
    }

    @Test
    fun applyProtections_WithSplashPotionAndNonInteractableBlock_ShouldAllowInteraction() {
        // Arrange
        fixture = createFixtureWithProjectileWeapon(Material.SPLASH_POTION, isBlockInteractable = false)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should not be cancelled (projectile allowed)
        verify(fixture.event, never()).isCancelled = true
    }

    @Test
    fun applyProtections_WithLingeringPotionAndNonInteractableBlock_ShouldAllowInteraction() {
        // Arrange
        fixture = createFixtureWithProjectileWeapon(Material.LINGERING_POTION, isBlockInteractable = false)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should not be cancelled (projectile allowed)
        verify(fixture.event, never()).isCancelled = true
    }

    @Test
    fun applyProtections_WithBowAndInteractableBlock_ShouldBlockInteraction() {
        // Arrange
        fixture = createFixtureWithProjectileWeapon(Material.BOW, isBlockInteractable = true)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should be cancelled (interactable block prevents projectile)
        verify(fixture.event).isCancelled = true
        verify(fixture.player).sendMessage("${ChatColor.RED}Cannot interact with block in faction territory TestFaction")
    }

    @Test
    fun applyProtections_WithNonProjectileWeaponAndNonInteractableBlock_ShouldBlockInteraction() {
        // Arrange
        fixture = createFixtureWithProjectileWeapon(Material.DIAMOND_SWORD, isBlockInteractable = false)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should be cancelled (not a projectile weapon)
        verify(fixture.event).isCancelled = true
        verify(fixture.player).sendMessage("${ChatColor.RED}Cannot interact with block in faction territory TestFaction")
    }

    @Test
    fun applyProtections_NoItemInHand_ShouldBlockInteraction() {
        // Arrange
        fixture = createFixtureWithNoItem(isBlockInteractable = false)
        setupProtectedTerritory()

        // Act
        uut.onPlayerInteract(fixture.event)

        // Assert - event should be cancelled (no item)
        verify(fixture.event).isCancelled = true
        verify(fixture.player).sendMessage("${ChatColor.RED}Cannot interact with block in faction territory TestFaction")
    }

    // Helper functions

    private fun createFixtureWithProjectileWeapon(
        material: Material,
        isBlockInteractable: Boolean
    ): PlayerInteractListenerTestFixture {
        val world = testUtils.createMockWorld()
        val block = testUtils.createMockBlock(world)
        val blockData = mock(BlockData::class.java)
        `when`(block.blockData).thenReturn(blockData)
        `when`(block.type).thenReturn(if (isBlockInteractable) Material.CHEST else Material.STONE)
        `when`(block.type.isInteractable).thenReturn(isBlockInteractable)

        val player = mock(Player::class.java)
        val playerId = UUID.randomUUID()
        `when`(player.uniqueId).thenReturn(playerId)

        val itemStack = mock(ItemStack::class.java)
        `when`(itemStack.type).thenReturn(material)

        val event = testUtils.createPlayerInteractEvent(
            player = player,
            action = Action.RIGHT_CLICK_BLOCK,
            item = itemStack,
            clickedBlock = block
        )

        return PlayerInteractListenerTestFixture(world, block, player, event, itemStack)
    }

    private fun createFixtureWithNoItem(
        isBlockInteractable: Boolean
    ): PlayerInteractListenerTestFixture {
        val world = testUtils.createMockWorld()
        val block = testUtils.createMockBlock(world)
        val blockData = mock(BlockData::class.java)
        `when`(block.blockData).thenReturn(blockData)
        `when`(block.type).thenReturn(if (isBlockInteractable) Material.CHEST else Material.STONE)
        `when`(block.type.isInteractable).thenReturn(isBlockInteractable)

        val player = mock(Player::class.java)
        val playerId = UUID.randomUUID()
        `when`(player.uniqueId).thenReturn(playerId)

        val event = testUtils.createPlayerInteractEvent(
            player = player,
            action = Action.RIGHT_CLICK_BLOCK,
            item = null,
            clickedBlock = block
        )

        return PlayerInteractListenerTestFixture(world, block, player, event, null)
    }

    private fun setupProtectedTerritory() {
        val playerId = MfPlayerId(fixture.player.uniqueId.toString())
        val mfPlayer = mock(MfPlayer::class.java)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(playerService.getPlayer(fixture.player)).thenReturn(mfPlayer)

        val factionId = MfFactionId("test-faction-id")
        val claim = mock(MfClaim::class.java)
        `when`(claim.factionId).thenReturn(factionId)
        `when`(claim.id).thenReturn(MfClaimId("test-claim-id"))
        `when`(claimService.getClaim(fixture.block.chunk)).thenReturn(claim)

        val faction = mock(MfFaction::class.java)
        `when`(faction.name).thenReturn("TestFaction")
        `when`(factionService.getFaction(factionId)).thenReturn(faction)

        // Player is not allowed to interact (not member of faction)
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)

        `when`(interactionService.getInteractionStatus(playerId)).thenReturn(null)
    }

    private data class PlayerInteractListenerTestFixture(
        val world: World,
        val block: Block,
        val player: Player,
        val event: PlayerInteractEvent,
        val itemStack: ItemStack?
    )

    private fun mockServices() {
        playerService = mock(MfPlayerService::class.java)
        claimService = mock(MfClaimService::class.java)
        factionService = mock(MfFactionService::class.java)
        interactionService = mock(MfInteractionService::class.java)

        val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.interactionService).thenReturn(interactionService)
    }

    private fun mockLanguageSystem() {
        val language = mock(Language::class.java)
        `when`(language["CannotInteractWithBlockInFactionTerritory", "TestFaction"])
            .thenReturn("Cannot interact with block in faction territory TestFaction")
        `when`(medievalFactions.language).thenReturn(language)
    }
}
