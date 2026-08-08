package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

/**
 * Covers the territory protection [PlayerInteractEntityListener] applies to entities other than
 * villagers, the villager path it leaves alone, and the way it cooperates with
 * [PlayerInteractAtEntityListener] when a single right-click raises both events.
 */
class PlayerInteractEntityListenerTest {

    private val claimFactionId = MfFactionId("claim-faction-id")

    private lateinit var medievalFactions: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var factionService: MfFactionService
    private lateinit var config: FileConfiguration
    private lateinit var player: Player
    private lateinit var mfPlayer: MfPlayer
    private lateinit var playerUuid: UUID
    private lateinit var uut: PlayerInteractEntityListener

    // Resolved from the stored UUID rather than from the player mock: reading a mock part-way through
    // a stubbing raises UnfinishedStubbingException.
    private val playerId: MfPlayerId
        get() = MfPlayerId(playerUuid.toString())

    @BeforeEach
    fun setUp() {
        medievalFactions = mock(MedievalFactions::class.java)
        mockServices()
        mockConfig()
        mockLanguageSystem()
        mockScheduler()
        mockPlayer()
        uut = PlayerInteractEntityListener(medievalFactions)
    }

    @Test
    fun onPlayerInteractEntity_ChestMinecartInClaimWithoutInteractionRights_ShouldCancelAndNotify() {
        assertEntityIsProtected(EntityType.CHEST_MINECART)
    }

    @Test
    fun onPlayerInteractEntity_HopperMinecartInClaimWithoutInteractionRights_ShouldCancelAndNotify() {
        assertEntityIsProtected(EntityType.HOPPER_MINECART)
    }

    @Test
    fun onPlayerInteractEntity_ItemFrameInClaimWithoutInteractionRights_ShouldCancelAndNotify() {
        assertEntityIsProtected(EntityType.ITEM_FRAME)
    }

    @Test
    fun onPlayerInteractEntity_BoatInClaimWithoutInteractionRights_ShouldCancelAndNotify() {
        assertEntityIsProtected(EntityType.OAK_BOAT)
    }

    @Test
    fun onPlayerInteractEntity_MinecartInClaimWithoutInteractionRights_ShouldCancelAndNotify() {
        assertEntityIsProtected(EntityType.MINECART)
    }

    @Test
    fun onPlayerInteractEntity_AnimalInClaimWithoutInteractionRights_ShouldCancelAndNotify() {
        assertEntityIsProtected(EntityType.COW)
    }

    @Test
    fun onPlayerInteractEntity_ArmorStandInClaimWithoutInteractionRights_ShouldCancelAndNotify() {
        assertEntityIsProtected(EntityType.ARMOR_STAND)
    }

    @Test
    fun onPlayerInteractEntity_NonMembersCanInteractWithEntitiesEnabled_ShouldAllowInteraction() {
        // Arrange
        val entity = createEntity(EntityType.CHEST_MINECART)
        val event = createEvent(entity)
        setUpClaim(entity, interactionAllowed = false)
        `when`(config.getBoolean("factions.nonMembersCanInteractWithEntities")).thenReturn(true)

        // Act
        uut.onPlayerInteractEntity(event)

        // Assert
        verify(event, never()).isCancelled = true
        verify(player, never()).sendMessage(any(String::class.java))
    }

    @Test
    fun onPlayerInteractEntity_PlayerWithInteractionRights_ShouldAllowInteraction() {
        // Arrange
        val entity = createEntity(EntityType.CHEST_MINECART)
        val event = createEvent(entity)
        setUpClaim(entity, interactionAllowed = true)

        // Act
        uut.onPlayerInteractEntity(event)

        // Assert
        verify(event, never()).isCancelled = true
        verify(player, never()).sendMessage(any(String::class.java))
    }

    @Test
    fun onPlayerInteractEntity_PlayerWithBypassEnabled_ShouldAllowInteractionAndWarn() {
        // Arrange
        val entity = createEntity(EntityType.CHEST_MINECART)
        val event = createEvent(entity)
        setUpClaim(entity, interactionAllowed = false)
        `when`(mfPlayer.isBypassEnabled).thenReturn(true)
        `when`(player.hasPermission("mf.bypass")).thenReturn(true)

        // Act
        uut.onPlayerInteractEntity(event)

        // Assert
        verify(event, never()).isCancelled = true
        verify(player).sendMessage(any(String::class.java))
    }

    @Test
    fun onPlayerInteractEntity_InWildernessWithPreventionEnabled_ShouldCancelAndNotify() {
        // Arrange
        val entity = createEntity(EntityType.CHEST_MINECART)
        val event = createEvent(entity)
        `when`(claimService.getClaim(entity.location.chunk)).thenReturn(null)
        `when`(config.getBoolean("wilderness.interaction.prevent", false)).thenReturn(true)
        `when`(config.getBoolean("wilderness.interaction.alert", true)).thenReturn(true)

        // Act
        uut.onPlayerInteractEntity(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage(any(String::class.java))
    }

    @Test
    fun onPlayerInteractEntity_InWildernessWithPreventionDisabled_ShouldAllowInteraction() {
        // Arrange
        val entity = createEntity(EntityType.CHEST_MINECART)
        val event = createEvent(entity)
        `when`(claimService.getClaim(entity.location.chunk)).thenReturn(null)

        // Act
        uut.onPlayerInteractEntity(event)

        // Assert
        verify(event, never()).isCancelled = true
        verify(player, never()).sendMessage(any(String::class.java))
    }

    @Test
    fun onPlayerInteractEntity_VillagerWithProtectVillagerTradeEnabled_ShouldCancelAndNotify() {
        // Arrange
        val entity = createEntity(EntityType.VILLAGER)
        val event = createEvent(entity)
        setUpClaim(entity, interactionAllowed = false, protectVillagerTrade = true)

        // Act
        uut.onPlayerInteractEntity(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage(any(String::class.java))
    }

    @Test
    fun onPlayerInteractEntity_VillagerWithProtectVillagerTradeDisabled_ShouldAllowInteraction() {
        // Arrange - the general entity check must not take over from the flag for villagers
        val entity = createEntity(EntityType.VILLAGER)
        val event = createEvent(entity)
        setUpClaim(entity, interactionAllowed = false, protectVillagerTrade = false)

        // Act
        uut.onPlayerInteractEntity(event)

        // Assert
        verify(event, never()).isCancelled = true
        verify(player, never()).sendMessage(any(String::class.java))
    }

    @Test
    fun onPlayerInteractEntity_PlayerInteractAtEntityEvent_ShouldBeLeftToTheAtEntityListener() {
        // Arrange
        val entity = createEntity(EntityType.CHEST_MINECART)
        val event = mock(PlayerInteractAtEntityEvent::class.java)
        `when`(event.player).thenReturn(player)
        `when`(event.rightClicked).thenReturn(entity)
        setUpClaim(entity, interactionAllowed = false)

        // Act
        uut.onPlayerInteractEntity(event)

        // Assert - the subclass carries its own handler list, so handling it here would double up
        verify(event, never()).isCancelled = true
        verify(player, never()).sendMessage(any(String::class.java))
    }

    @Test
    fun onPlayerInteractEntity_BothEventsForOneClick_ShouldCancelBothButNotifyOnce() {
        // Arrange - a single right-click can raise an "interact at" event followed by an "interact" event
        val entity = createEntity(EntityType.CHEST_MINECART)
        setUpClaim(entity, interactionAllowed = false)

        val entityInteractionProtection = EntityInteractionProtection(medievalFactions)
        val atEntityListener = PlayerInteractAtEntityListener(medievalFactions, entityInteractionProtection)
        val entityListener = PlayerInteractEntityListener(medievalFactions, entityInteractionProtection)

        val atEntityEvent = mock(PlayerInteractAtEntityEvent::class.java)
        `when`(atEntityEvent.player).thenReturn(player)
        `when`(atEntityEvent.rightClicked).thenReturn(entity)
        val entityEvent = createEvent(entity)

        // Act
        atEntityListener.onPlayerInteractAtEntity(atEntityEvent)
        entityListener.onPlayerInteractEntity(entityEvent)

        // Assert - both events have to be cancelled, but the player is only told once
        verify(atEntityEvent).isCancelled = true
        verify(entityEvent).isCancelled = true
        verify(player).sendMessage(any(String::class.java))
    }

    private fun assertEntityIsProtected(entityType: EntityType) {
        // Arrange
        val entity = createEntity(entityType)
        val event = createEvent(entity)
        setUpClaim(entity, interactionAllowed = false)

        // Act
        uut.onPlayerInteractEntity(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage(any(String::class.java))
    }

    private fun createEntity(entityType: EntityType): Entity {
        val chunk = mock(Chunk::class.java)
        val location = mock(Location::class.java)
        `when`(location.chunk).thenReturn(chunk)

        val entity = mock(Entity::class.java)
        `when`(entity.type).thenReturn(entityType)
        `when`(entity.location).thenReturn(location)
        `when`(entity.uniqueId).thenReturn(UUID.randomUUID())
        return entity
    }

    private fun createEvent(entity: Entity): PlayerInteractEntityEvent {
        val event = mock(PlayerInteractEntityEvent::class.java)
        `when`(event.player).thenReturn(player)
        `when`(event.rightClicked).thenReturn(entity)
        return event
    }

    private fun setUpClaim(
        entity: Entity,
        interactionAllowed: Boolean,
        protectVillagerTrade: Boolean = true
    ): MfClaimedChunk {
        val chunk = entity.location.chunk
        val claim = mock(MfClaimedChunk::class.java)
        `when`(claim.factionId).thenReturn(claimFactionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(interactionAllowed)

        val protectVillagerTradeFlag = MfFlag.boolean(medievalFactions, "protectVillagerTrade", false)
        val flags = mock(MfFlags::class.java)
        `when`(flags.protectVillagerTrade).thenReturn(protectVillagerTradeFlag)
        `when`(medievalFactions.flags).thenReturn(flags)

        val faction = mock(MfFaction::class.java)
        `when`(faction.name).thenReturn("Claim Faction")
        `when`(faction.flags).thenReturn(
            MfFlagValues(medievalFactions, mapOf("protectVillagerTrade" to protectVillagerTrade))
        )
        `when`(factionService.getFaction(claimFactionId)).thenReturn(faction)
        return claim
    }

    private fun mockServices() {
        playerService = mock(MfPlayerService::class.java)
        claimService = mock(MfClaimService::class.java)
        factionService = mock(MfFactionService::class.java)

        val services = mock(Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.factionService).thenReturn(factionService)
    }

    private fun mockConfig() {
        config = mock(FileConfiguration::class.java)
        `when`(medievalFactions.config).thenReturn(config)
    }

    private fun mockLanguageSystem() {
        val language = mock(Language::class.java)
        `when`(language.get(anyString())).thenReturn("Cannot interact with entity")
        `when`(language.get(anyString(), anyString())).thenReturn("Cannot interact with entity")
        `when`(medievalFactions.language).thenReturn(language)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        val scheduler = mock(BukkitScheduler::class.java)
        `when`(medievalFactions.server).thenReturn(server)
        `when`(server.scheduler).thenReturn(scheduler)
    }

    private fun mockPlayer() {
        player = mock(Player::class.java)
        playerUuid = UUID.randomUUID()
        `when`(player.uniqueId).thenReturn(playerUuid)
        mfPlayer = mock(MfPlayer::class.java)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
    }
}
