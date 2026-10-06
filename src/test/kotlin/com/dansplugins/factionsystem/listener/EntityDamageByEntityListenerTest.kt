package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.duel.MfDuel
import com.dansplugins.factionsystem.duel.MfDuelId
import com.dansplugins.factionsystem.duel.MfDuelService
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.ChatColor
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class EntityDamageByEntityListenerTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var services: Services
    private lateinit var playerService: MfPlayerService
    private lateinit var factionService: MfFactionService
    private lateinit var duelService: MfDuelService
    private lateinit var config: FileConfiguration
    private lateinit var uut: EntityDamageByEntityListener

    private lateinit var damager: Player
    private lateinit var damaged: Player
    private lateinit var damagerMfPlayer: MfPlayer
    private lateinit var damagedMfPlayer: MfPlayer
    private lateinit var event: EntityDamageByEntityEvent
    private var now = 0L

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        services = mock(Services::class.java)
        playerService = mock(MfPlayerService::class.java)
        factionService = mock(MfFactionService::class.java)
        duelService = mock(MfDuelService::class.java)
        config = mock(FileConfiguration::class.java)

        `when`(plugin.services).thenReturn(services)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.duelService).thenReturn(duelService)
        `when`(plugin.config).thenReturn(config)
        // Each language key comes back unformatted, so the tests can assert which message was sent
        val language = mock(Language::class.java) { invocation ->
            if (invocation.method.returnType == String::class.java) invocation.arguments[0] else RETURNS_DEFAULTS.answer(invocation)
        }
        `when`(plugin.language).thenReturn(language)

        // Default: pvp allowed for factionless
        `when`(config.getBoolean("pvp.enabledForFactionlessPlayers")).thenReturn(true)

        damager = mock(Player::class.java)
        damaged = mock(Player::class.java)
        val damagerId = UUID.randomUUID()
        val damagedId = UUID.randomUUID()
        `when`(damager.uniqueId).thenReturn(damagerId)
        `when`(damaged.uniqueId).thenReturn(damagedId)
        `when`(damaged.name).thenReturn("Victim")

        damagerMfPlayer = mock(MfPlayer::class.java)
        damagedMfPlayer = mock(MfPlayer::class.java)
        `when`(damagerMfPlayer.id).thenReturn(MfPlayerId(damagerId.toString()))
        `when`(damagedMfPlayer.id).thenReturn(MfPlayerId(damagedId.toString()))

        `when`(playerService.getPlayer(damager)).thenReturn(damagerMfPlayer)
        `when`(playerService.getPlayer(damaged)).thenReturn(damagedMfPlayer)

        event = mock(EntityDamageByEntityEvent::class.java)
        `when`(event.damager).thenReturn(damager)
        `when`(event.entity).thenReturn(damaged)

        uut = EntityDamageByEntityListener(plugin) { now }
    }

    @Test
    fun onEntityDamage_BothInSameDuel_ShouldNotCancel() {
        val duelId = MfDuelId("shared-duel")
        val duel = mock(MfDuel::class.java)
        `when`(duel.id).thenReturn(duelId)

        `when`(duelService.getDuel(damagerMfPlayer.id)).thenReturn(duel)
        `when`(duelService.getDuel(damagedMfPlayer.id)).thenReturn(duel)

        uut.onEntityDamageByEntity(event)

        verify(event, never()).isCancelled = true
    }

    @Test
    fun onEntityDamage_DamagerInDuelButDamagedIsNot_ShouldCancel() {
        val duel = mock(MfDuel::class.java)
        `when`(duel.id).thenReturn(MfDuelId("damager-duel"))

        `when`(duelService.getDuel(damagerMfPlayer.id)).thenReturn(duel)
        `when`(duelService.getDuel(damagedMfPlayer.id)).thenReturn(null)

        uut.onEntityDamageByEntity(event)

        verify(event).isCancelled = true
    }

    @Test
    fun onEntityDamage_DamagedInDuelButDamagerIsNot_ShouldCancel() {
        val duel = mock(MfDuel::class.java)
        `when`(duel.id).thenReturn(MfDuelId("damaged-duel"))

        `when`(duelService.getDuel(damagerMfPlayer.id)).thenReturn(null)
        `when`(duelService.getDuel(damagedMfPlayer.id)).thenReturn(duel)

        uut.onEntityDamageByEntity(event)

        verify(event).isCancelled = true
    }

    @Test
    fun onEntityDamage_InDifferentDuels_ShouldCancel() {
        val duel1 = mock(MfDuel::class.java)
        val duel2 = mock(MfDuel::class.java)
        `when`(duel1.id).thenReturn(MfDuelId("duel-1"))
        `when`(duel2.id).thenReturn(MfDuelId("duel-2"))

        `when`(duelService.getDuel(damagerMfPlayer.id)).thenReturn(duel1)
        `when`(duelService.getDuel(damagedMfPlayer.id)).thenReturn(duel2)

        uut.onEntityDamageByEntity(event)

        verify(event).isCancelled = true
    }

    @Test
    fun onEntityDamage_NeitherInDuel_ShouldNotCancelDueToDuel() {
        `when`(duelService.getDuel(damagerMfPlayer.id)).thenReturn(null)
        `when`(duelService.getDuel(damagedMfPlayer.id)).thenReturn(null)

        uut.onEntityDamageByEntity(event)

        verify(event, never()).isCancelled = true
    }

    @Test
    fun onEntityDamage_DuelRefusesHit_ShouldTellDamager() {
        val duel = mock(MfDuel::class.java)
        `when`(duel.id).thenReturn(MfDuelId("damaged-duel"))
        `when`(duelService.getDuel(damagedMfPlayer.id)).thenReturn(duel)

        uut.onEntityDamageByEntity(event)

        verify(damager).sendMessage("${ChatColor.RED}CannotAttackPlayerDuringDuel")
        verify(damaged, never()).sendMessage(anyString())
    }

    @Test
    fun onEntityDamage_FactionlessPvpDisabled_ShouldCancelAndTellDamager() {
        `when`(config.getBoolean("pvp.enabledForFactionlessPlayers")).thenReturn(false)

        uut.onEntityDamageByEntity(event)

        verify(event).isCancelled = true
        verify(damager).sendMessage("${ChatColor.RED}CannotAttackPlayerFactionless")
    }

    @Test
    fun onEntityDamage_FactionlessPvpEnabled_ShouldNotTellDamager() {
        uut.onEntityDamageByEntity(event)

        verify(damager, never()).sendMessage(anyString())
    }

    @Test
    fun onEntityDamage_RepeatedRefusals_ShouldTellDamagerOncePerInterval() {
        `when`(config.getBoolean("pvp.enabledForFactionlessPlayers")).thenReturn(false)

        uut.onEntityDamageByEntity(event)
        now += EntityDamageByEntityListener.REFUSAL_MESSAGE_INTERVAL_NANOS - 1
        uut.onEntityDamageByEntity(event)
        verify(damager, times(1)).sendMessage("${ChatColor.RED}CannotAttackPlayerFactionless")

        now += 1
        uut.onEntityDamageByEntity(event)
        verify(damager, times(2)).sendMessage("${ChatColor.RED}CannotAttackPlayerFactionless")
        verify(event, times(3)).isCancelled = true
    }
}
