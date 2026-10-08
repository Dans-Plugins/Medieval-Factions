package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.duel.MfDuel
import com.dansplugins.factionsystem.duel.MfDuelId
import com.dansplugins.factionsystem.duel.MfDuelService
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.potion.PotionEffect
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class PotionSplashListenerTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var duelService: MfDuelService
    private lateinit var config: FileConfiguration
    private lateinit var uut: PotionSplashListener

    // PotionEffectType's constants can't be loaded without a server, so effects are mocks and
    // the listener is told which of them are harmful
    private val harmfulEffect = mock(PotionEffect::class.java)
    private val harmlessEffect = mock(PotionEffect::class.java)

    private lateinit var thrower: Player
    private lateinit var potion: ThrownPotion
    private lateinit var event: PotionSplashEvent

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        val services = mock(Services::class.java)
        playerService = mock(MfPlayerService::class.java)
        duelService = mock(MfDuelService::class.java)
        config = mock(FileConfiguration::class.java)

        `when`(plugin.services).thenReturn(services)
        `when`(services.playerService).thenReturn(playerService)
        // Unstubbed, getFaction returns null, so every player is factionless
        `when`(services.factionService).thenReturn(mock(MfFactionService::class.java))
        `when`(services.duelService).thenReturn(duelService)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("pvp.enabledForFactionlessPlayers")).thenReturn(false)

        thrower = mockPlayer()
        potion = mock(ThrownPotion::class.java)
        `when`(potion.shooter).thenReturn(thrower)
        `when`(potion.effects).thenReturn(listOf(harmfulEffect))
        event = mock(PotionSplashEvent::class.java)
        `when`(event.potion).thenReturn(potion)

        uut = PotionSplashListener(plugin) { it === harmfulEffect }
    }

    @Test
    fun onPotionSplash_FactionlessPvpDisabled_ShouldZeroIntensityForEveryAffectedPlayer() {
        val first = mockPlayer()
        val second = mockPlayer()
        `when`(event.affectedEntities).thenReturn(listOf(first, second))

        uut.onPotionSplash(event)

        verify(event).setIntensity(first, 0.0)
        verify(event).setIntensity(second, 0.0)
    }

    @Test
    fun onPotionSplash_FirstPlayerInThrowersDuel_ShouldStillCheckTheOthers() {
        val opponent = mockPlayer()
        val bystander = mockPlayer()
        val duel = mock(MfDuel::class.java)
        `when`(duel.id).thenReturn(MfDuelId("duel"))
        val throwerId = playerService.getPlayer(thrower)!!.id
        val opponentId = playerService.getPlayer(opponent)!!.id
        `when`(duelService.getDuel(throwerId)).thenReturn(duel)
        `when`(duelService.getDuel(opponentId)).thenReturn(duel)
        `when`(event.affectedEntities).thenReturn(listOf(opponent, bystander))

        uut.onPotionSplash(event)

        verify(event, never()).setIntensity(eq(opponent), anyDouble())
        verify(event).setIntensity(bystander, 0.0)
    }

    @Test
    fun onPotionSplash_FactionlessPvpEnabled_ShouldLeaveIntensityAlone() {
        `when`(config.getBoolean("pvp.enabledForFactionlessPlayers")).thenReturn(true)
        val first = mockPlayer()
        val second = mockPlayer()
        `when`(event.affectedEntities).thenReturn(listOf(first, second))

        uut.onPotionSplash(event)

        verify(event, never()).setIntensity(eq(first), anyDouble())
        verify(event, never()).setIntensity(eq(second), anyDouble())
    }

    @Test
    fun onPotionSplash_NoHarmfulEffect_ShouldNotCheckAffectedPlayers() {
        `when`(potion.effects).thenReturn(listOf(harmlessEffect))

        uut.onPotionSplash(event)

        verify(event, never()).affectedEntities
    }

    private fun mockPlayer(): Player {
        val player = mock(Player::class.java)
        val id = UUID.randomUUID()
        `when`(player.uniqueId).thenReturn(id)
        val mfPlayer = mock(MfPlayer::class.java)
        `when`(mfPlayer.id).thenReturn(MfPlayerId(id.toString()))
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        return player
    }
}
