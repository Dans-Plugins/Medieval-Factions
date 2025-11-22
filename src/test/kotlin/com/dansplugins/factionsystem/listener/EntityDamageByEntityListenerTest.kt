package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class EntityDamageByEntityListenerTest {
    private val testUtils = TestUtils()

    private lateinit var medievalFactions: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var factionService: MfFactionService
    private lateinit var claimService: MfClaimService
    private lateinit var flags: MfFlags
    private lateinit var uut: EntityDamageByEntityListener

    @BeforeEach
    fun setUp() {
        medievalFactions = mock(MedievalFactions::class.java)
        mockServices()
        uut = EntityDamageByEntityListener(medievalFactions)
    }

    @Test
    fun onEntityDamageByEntity_ArmorStandInFactionTerritory_DamagerNotInFaction_ShouldCancel() {
        // Arrange
        val damager = mock(Player::class.java)
        val armorStand = mock(ArmorStand::class.java)
        val event = mock(EntityDamageByEntityEvent::class.java)
        val chunk = mock(Chunk::class.java)
        val location = mock(Location::class.java)

        `when`(event.damager).thenReturn(damager)
        `when`(event.entity).thenReturn(armorStand)
        `when`(armorStand.location).thenReturn(location)
        `when`(location.chunk).thenReturn(chunk)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = mock(MfPlayerId::class.java)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(playerService.getPlayer(damager)).thenReturn(mfPlayer)

        val claim = mock(MfClaimedChunk::class.java)
        val factionId = mock(MfFactionId::class.java)
        `when`(claim.factionId).thenReturn(factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)

        val faction = mock(MfFaction::class.java)
        `when`(factionService.getFaction(factionId)).thenReturn(faction)
        `when`(factionService.getFaction(playerId)).thenReturn(null) // damager has no faction

        val enableMobProtectionFlag = mock(com.dansplugins.factionsystem.faction.flag.MfFlag::class.java) as com.dansplugins.factionsystem.faction.flag.MfFlag<Boolean>
        `when`(flags.enableMobProtection).thenReturn(enableMobProtectionFlag)
        val flagValues = mock(com.dansplugins.factionsystem.faction.flag.MfFlagValues::class.java)
        `when`(faction.flags).thenReturn(flagValues)
        `when`(flagValues.get(enableMobProtectionFlag)).thenReturn(true)

        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(false)

        // Act
        uut.onEntityDamageByEntity(event)

        // Assert
        verify(event).isCancelled = true
    }

    @Test
    fun onEntityDamageByEntity_ArmorStandInFactionTerritory_DamagerIsAlly_ShouldNotCancel() {
        // Arrange
        val damager = mock(Player::class.java)
        val armorStand = mock(ArmorStand::class.java)
        val event = mock(EntityDamageByEntityEvent::class.java)
        val chunk = mock(Chunk::class.java)
        val location = mock(Location::class.java)

        `when`(event.damager).thenReturn(damager)
        `when`(event.entity).thenReturn(armorStand)
        `when`(armorStand.location).thenReturn(location)
        `when`(location.chunk).thenReturn(chunk)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = mock(MfPlayerId::class.java)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(playerService.getPlayer(damager)).thenReturn(mfPlayer)

        val claim = mock(MfClaimedChunk::class.java)
        val factionId = mock(MfFactionId::class.java)
        `when`(claim.factionId).thenReturn(factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)

        val faction = mock(MfFaction::class.java)
        `when`(factionService.getFaction(factionId)).thenReturn(faction)
        
        val damagerFaction = mock(MfFaction::class.java)
        `when`(factionService.getFaction(playerId)).thenReturn(damagerFaction)

        val enableMobProtectionFlag = mock(com.dansplugins.factionsystem.faction.flag.MfFlag::class.java) as com.dansplugins.factionsystem.faction.flag.MfFlag<Boolean>
        `when`(flags.enableMobProtection).thenReturn(enableMobProtectionFlag)
        val flagValues = mock(com.dansplugins.factionsystem.faction.flag.MfFlagValues::class.java)
        `when`(faction.flags).thenReturn(flagValues)
        `when`(flagValues.get(enableMobProtectionFlag)).thenReturn(true)

        // Ally is allowed to interact
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(true)

        // Act
        uut.onEntityDamageByEntity(event)

        // Assert
        verify(event, never()).isCancelled = true
        verify(event, never()).isCancelled = false
    }

    @Test
    fun onEntityDamageByEntity_MonsterInFactionTerritory_DamagerNotInFaction_ShouldNotCancel() {
        // Arrange
        val damager = mock(Player::class.java)
        val zombie = mock(Zombie::class.java)
        val event = mock(EntityDamageByEntityEvent::class.java)
        val chunk = mock(Chunk::class.java)
        val location = mock(Location::class.java)

        `when`(event.damager).thenReturn(damager)
        `when`(event.entity).thenReturn(zombie)
        `when`(zombie.location).thenReturn(location)
        `when`(location.chunk).thenReturn(chunk)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = mock(MfPlayerId::class.java)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(playerService.getPlayer(damager)).thenReturn(mfPlayer)

        val claim = mock(MfClaimedChunk::class.java)
        val factionId = mock(MfFactionId::class.java)
        `when`(claim.factionId).thenReturn(factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)

        val faction = mock(MfFaction::class.java)
        `when`(factionService.getFaction(factionId)).thenReturn(faction)
        `when`(factionService.getFaction(playerId)).thenReturn(null)

        val enableMobProtectionFlag = mock(com.dansplugins.factionsystem.faction.flag.MfFlag::class.java) as com.dansplugins.factionsystem.faction.flag.MfFlag<Boolean>
        `when`(flags.enableMobProtection).thenReturn(enableMobProtectionFlag)
        val flagValues = mock(com.dansplugins.factionsystem.faction.flag.MfFlagValues::class.java)
        `when`(faction.flags).thenReturn(flagValues)
        `when`(flagValues.get(enableMobProtectionFlag)).thenReturn(true)

        // Act
        uut.onEntityDamageByEntity(event)

        // Assert - Monsters should be allowed to be damaged
        verify(event, never()).isCancelled = true
        verify(event, never()).isCancelled = false
    }

    @Test
    fun onEntityDamageByEntity_ArmorStandInFactionTerritory_MobProtectionDisabled_ShouldNotCancel() {
        // Arrange
        val damager = mock(Player::class.java)
        val armorStand = mock(ArmorStand::class.java)
        val event = mock(EntityDamageByEntityEvent::class.java)
        val chunk = mock(Chunk::class.java)
        val location = mock(Location::class.java)

        `when`(event.damager).thenReturn(damager)
        `when`(event.entity).thenReturn(armorStand)
        `when`(armorStand.location).thenReturn(location)
        `when`(location.chunk).thenReturn(chunk)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = mock(MfPlayerId::class.java)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(playerService.getPlayer(damager)).thenReturn(mfPlayer)

        val claim = mock(MfClaimedChunk::class.java)
        val factionId = mock(MfFactionId::class.java)
        `when`(claim.factionId).thenReturn(factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)

        val faction = mock(MfFaction::class.java)
        `when`(factionService.getFaction(factionId)).thenReturn(faction)
        `when`(factionService.getFaction(playerId)).thenReturn(null)

        val enableMobProtectionFlag = mock(com.dansplugins.factionsystem.faction.flag.MfFlag::class.java) as com.dansplugins.factionsystem.faction.flag.MfFlag<Boolean>
        `when`(flags.enableMobProtection).thenReturn(enableMobProtectionFlag)
        val flagValues = mock(com.dansplugins.factionsystem.faction.flag.MfFlagValues::class.java)
        `when`(faction.flags).thenReturn(flagValues)
        `when`(flagValues.get(enableMobProtectionFlag)).thenReturn(false) // Mob protection disabled

        // Act
        uut.onEntityDamageByEntity(event)

        // Assert - Should not cancel since mob protection is disabled
        verify(event, never()).isCancelled = true
        verify(event, never()).isCancelled = false
    }

    // Helper functions

    private fun mockServices() {
        playerService = mock(MfPlayerService::class.java)
        factionService = mock(MfFactionService::class.java)
        claimService = mock(MfClaimService::class.java)
        flags = mock(MfFlags::class.java)

        val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.duelService).thenReturn(mock(com.dansplugins.factionsystem.duel.MfDuelService::class.java))
        `when`(medievalFactions.flags).thenReturn(flags)
    }
}
