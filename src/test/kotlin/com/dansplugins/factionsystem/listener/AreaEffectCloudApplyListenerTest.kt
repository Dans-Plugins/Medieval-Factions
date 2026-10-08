package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.duel.MfDuel
import com.dansplugins.factionsystem.duel.MfDuelId
import com.dansplugins.factionsystem.duel.MfDuelService
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.potion.MfPotionService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.AreaEffectCloudApplyEvent
import org.bukkit.potion.PotionData
import org.bukkit.potion.PotionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class AreaEffectCloudApplyListenerTest {

    private lateinit var medievalFactions: MedievalFactions
    private lateinit var uut: AreaEffectCloudApplyListener

    @BeforeEach
    fun setUp() {
        medievalFactions = mock(MedievalFactions::class.java)
        uut = AreaEffectCloudApplyListener(medievalFactions)
    }

    @Test
    fun onAreaEffectCloudApply_BasePotionDataIsNull_ShouldReturnWithoutError() {
        val areaEffectCloud = mock(AreaEffectCloud::class.java)
        val event = mock(AreaEffectCloudApplyEvent::class.java)

        `when`(event.entity).thenReturn(areaEffectCloud)
        `when`(areaEffectCloud.basePotionData).thenReturn(null)

        uut.onAreaEffectCloudApply(event)

        verify(event, never()).affectedEntities
    }

    @Test
    fun onAreaEffectCloudApply_BasePotionDataIsNotHarmful_ShouldReturn() {
        val areaEffectCloud = mock(AreaEffectCloud::class.java)
        val event = mock(AreaEffectCloudApplyEvent::class.java)
        val potionData = mock(PotionData::class.java)

        `when`(event.entity).thenReturn(areaEffectCloud)
        `when`(areaEffectCloud.basePotionData).thenReturn(potionData)
        `when`(potionData.type).thenReturn(PotionType.WATER) // Water is benign

        uut.onAreaEffectCloudApply(event)

        verify(event, never()).affectedEntities
    }

    @Test
    fun onAreaEffectCloudApply_BasePotionDataIsHarmful_ShouldProcess() {
        val areaEffectCloud = mock(AreaEffectCloud::class.java)
        val event = mock(AreaEffectCloudApplyEvent::class.java)
        val potionData = mock(PotionData::class.java)

        `when`(event.entity).thenReturn(areaEffectCloud)
        `when`(areaEffectCloud.basePotionData).thenReturn(potionData)
        `when`(potionData.type).thenReturn(PotionType.POISON) // Poison is harmful

        val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
        val potionService = mock(com.dansplugins.factionsystem.potion.MfPotionService::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.potionService).thenReturn(potionService)
        `when`(potionService.getLingeringPotionEffectThrower(areaEffectCloud)).thenReturn(null)

        uut.onAreaEffectCloudApply(event)

        verify(potionService).getLingeringPotionEffectThrower(areaEffectCloud)
        verify(event, never()).affectedEntities
    }

    @Test
    fun harmfulPotionTypes_ResolvesEveryNamePresentInTheServerApi() {
        val expected = PotionType.values().filter { it.name in AreaEffectCloudApplyListener.HARMFUL_POTION_TYPE_NAMES }

        assertEquals(expected, uut.harmfulPotionTypes)
        assertTrue(PotionType.LONG_POISON in uut.harmfulPotionTypes)
        assertTrue(PotionType.STRONG_HARMING in uut.harmfulPotionTypes)
        assertFalse(PotionType.WATER in uut.harmfulPotionTypes)
    }

    @Test
    fun onAreaEffectCloudApply_FactionlessPvpDisabled_ShouldRemoveEveryAffectedPlayer() {
        val fixture = harmfulCloudFixture()
        val first = fixture.mockPlayer()
        val second = fixture.mockPlayer()
        val affectedEntities = mutableListOf<LivingEntity>(first, second)
        `when`(fixture.event.affectedEntities).thenReturn(affectedEntities)

        uut.onAreaEffectCloudApply(fixture.event)

        assertEquals(emptyList<LivingEntity>(), affectedEntities)
    }

    @Test
    fun onAreaEffectCloudApply_FirstPlayerInThrowersDuel_ShouldStillCheckTheOthers() {
        val fixture = harmfulCloudFixture()
        val opponent = fixture.mockPlayer()
        val bystander = fixture.mockPlayer()
        val duel = mock(MfDuel::class.java)
        `when`(duel.id).thenReturn(MfDuelId("duel"))
        val throwerId = fixture.playerService.getPlayer(fixture.thrower)!!.id
        val opponentId = fixture.playerService.getPlayer(opponent)!!.id
        `when`(fixture.duelService.getDuel(throwerId)).thenReturn(duel)
        `when`(fixture.duelService.getDuel(opponentId)).thenReturn(duel)
        val affectedEntities = mutableListOf<LivingEntity>(opponent, bystander)
        `when`(fixture.event.affectedEntities).thenReturn(affectedEntities)

        uut.onAreaEffectCloudApply(fixture.event)

        assertEquals(listOf<LivingEntity>(opponent), affectedEntities)
    }

    private class HarmfulCloudFixture(
        val event: AreaEffectCloudApplyEvent,
        val thrower: Player,
        val playerService: MfPlayerService,
        val duelService: MfDuelService
    ) {
        fun mockPlayer(): Player {
            val player = mock(Player::class.java)
            val id = UUID.randomUUID()
            `when`(player.uniqueId).thenReturn(id)
            val mfPlayer = mock(MfPlayer::class.java)
            `when`(mfPlayer.id).thenReturn(MfPlayerId(id.toString()))
            `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
            return player
        }
    }

    // A poison cloud thrown by a factionless player, on a server where factionless players can't fight
    private fun harmfulCloudFixture(): HarmfulCloudFixture {
        val areaEffectCloud = mock(AreaEffectCloud::class.java)
        val event = mock(AreaEffectCloudApplyEvent::class.java)
        val potionData = mock(PotionData::class.java)
        `when`(event.entity).thenReturn(areaEffectCloud)
        `when`(areaEffectCloud.basePotionData).thenReturn(potionData)
        `when`(potionData.type).thenReturn(PotionType.POISON)

        val services = mock(Services::class.java)
        val potionService = mock(MfPotionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val duelService = mock(MfDuelService::class.java)
        val config = mock(FileConfiguration::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(medievalFactions.config).thenReturn(config)
        `when`(services.potionService).thenReturn(potionService)
        `when`(services.playerService).thenReturn(playerService)
        // Unstubbed, getFaction returns null, so every player is factionless
        `when`(services.factionService).thenReturn(mock(MfFactionService::class.java))
        `when`(services.duelService).thenReturn(duelService)
        `when`(config.getBoolean("pvp.enabledForFactionlessPlayers")).thenReturn(false)

        val thrower = mock(Player::class.java)
        val fixture = HarmfulCloudFixture(event, thrower, playerService, duelService)
        val throwerId = UUID.randomUUID()
        `when`(thrower.uniqueId).thenReturn(throwerId)
        val throwerMfPlayer = mock(MfPlayer::class.java)
        `when`(throwerMfPlayer.id).thenReturn(MfPlayerId(throwerId.toString()))
        `when`(playerService.getPlayer(thrower)).thenReturn(throwerMfPlayer)
        `when`(potionService.getLingeringPotionEffectThrower(areaEffectCloud)).thenReturn(thrower)
        return fixture
    }
}
