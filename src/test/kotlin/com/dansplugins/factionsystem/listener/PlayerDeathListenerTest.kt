package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doCallRealMethod
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Locale
import java.util.UUID
import java.util.logging.Logger

/**
 * Covers `factions.leaderZeroPowerDisbandsFaction` (#1945): a faction is disbanded when a member holding its Owner role
 * is brought to 0 power by a death, and never by offline power decay.
 */
class PlayerDeathListenerTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var playerService: MfPlayerService
    private lateinit var factionService: MfFactionService
    private lateinit var faction: MfFaction
    private lateinit var victim: Player
    private val victimId = MfPlayerId(UUID.randomUUID().toString())
    private val factionId = MfFactionId.generate()

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        // Messages are their keys; the death message formatter asks the language for its locale.
        val language = mock(Language::class.java) { invocation ->
            if (invocation.method.returnType == Locale::class.java) Locale.US else invocation.arguments.firstOrNull() as? String ?: ""
        }
        `when`(plugin.language).thenReturn(language)

        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getDouble("players.powerLostOnDeath")).thenReturn(1.0)
        `when`(config.getDouble("players.maxPower")).thenReturn(20.0)
        `when`(config.getBoolean("pvp.grantPowerToKillerIfVictimHasZeroPower")).thenReturn(true)
        `when`(config.getBoolean("factions.leaderZeroPowerDisbandsFaction")).thenReturn(true)

        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }

        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
        `when`(playerService.getMaxPower(victimId)).thenReturn(20.0)
        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        `when`(factionService.delete(factionId)).thenReturn(Success(Unit))

        faction = mock(MfFaction::class.java)
        `when`(faction.id).thenReturn(factionId)
        `when`(faction.name).thenReturn("Wessex")
        `when`(faction.power).thenReturn(10.0)
        `when`(factionService.getFaction(victimId)).thenReturn(faction)

        victim = mock(Player::class.java)
        `when`(victim.name).thenReturn("Alfred")
    }

    @Test
    fun anOwnerBroughtToZeroPowerByADeathDisbandsTheFaction() {
        givenVictim(role = "Owner", power = 1.0)

        die()

        verify(factionService).delete(factionId)
        verify(faction).sendMessage("FactionDisbandedLeaderDeathNotificationTitle", "FactionDisbandedLeaderDeathNotificationBody")
    }

    @Test
    fun theOwnerRoleIsMatchedRegardlessOfCase() {
        givenVictim(role = "OWNER", power = 0.5)

        die()

        verify(factionService).delete(factionId)
    }

    @Test
    fun theSettingIsOffByDefaultAndThenNothingIsDisbanded() {
        `when`(config.getBoolean("factions.leaderZeroPowerDisbandsFaction")).thenReturn(false)
        givenVictim(role = "Owner", power = 1.0)

        die()

        verify(factionService, never()).delete(factionId)
    }

    @Test
    fun aMemberWithoutTheOwnerRoleDoesNotDisbandTheFaction() {
        givenVictim(role = "Officer", power = 1.0)

        die()

        verify(factionService, never()).delete(factionId)
    }

    @Test
    fun anOwnerWithPowerLeftAfterDyingDoesNotDisbandTheFaction() {
        givenVictim(role = "Owner", power = 5.0)

        die()

        verify(factionService, never()).delete(factionId)
    }

    @Test
    fun anOwnerAlreadyAtZeroFromOfflineDecayWhoThenDiesDoesNotDisbandTheFaction() {
        // Offline decay took the leader to 0 (or below); the death itself took no power from them
        givenVictim(role = "Owner", power = 0.0)
        die()
        givenVictim(role = "Owner", power = -3.0)
        die()

        verify(factionService, never()).delete(factionId)
    }

    @Test
    fun isLeaderDeathAtZeroPower_needsTheDeathItselfToReachZero() {
        val owner = MfFactionRole(plugin, name = "Owner")
        `when`(faction.getRole(victimId)).thenReturn(owner)

        assertTrue(isLeaderDeathAtZeroPower(faction, victimId, powerBeforeDeath = 2.0, powerAfterDeath = 0.0))
        assertFalse(isLeaderDeathAtZeroPower(faction, victimId, powerBeforeDeath = 0.0, powerAfterDeath = 0.0))
        assertFalse(isLeaderDeathAtZeroPower(faction, victimId, powerBeforeDeath = 2.0, powerAfterDeath = 1.0))
        assertFalse(isLeaderDeathAtZeroPower(faction, victimId, powerBeforeDeath = 2.0, powerAfterDeath = null))
    }

    @Test
    fun offlinePowerDecayNeverDisbandsAFactionWhoseLeaderReachesZero() {
        // The power cycle is where offline decay happens. With only the leader setting on, a leader who has decayed to
        // 0 power must not cost the faction anything.
        `when`(config.getBoolean("factions.zeroPowerFactionsGetDisbanded")).thenReturn(false)
        val ownerRole = MfFactionRole(plugin, name = "Owner")
        `when`(faction.getRole(victimId)).thenReturn(ownerRole)
        `when`(playerService.getPlayer(victimId)).thenReturn(MfPlayer(victimId, power = 0.0))
        `when`(playerService.updatePlayerPower(anyList())).thenReturn(Success(Unit))
        `when`(factionService.factions).thenReturn(listOf(faction))
        doCallRealMethod().`when`(plugin).onPowerCycle(emptyList(), 0.0, emptyList(), false)

        plugin.onPowerCycle(emptyList(), 0.0, emptyList(), disbandZeroPowerFactions = false)

        verify(playerService).updatePlayerPower(emptyList())
        verify(factionService, never()).delete(factionId)
    }

    @Test
    fun aLeaderDeathIsCappedAtTheLeadersOwnMaxPower() {
        // players.leaderMaxPower (#1945): a leader above players.maxPower keeps the power the death leaves them
        `when`(config.getBoolean("factions.leaderZeroPowerDisbandsFaction")).thenReturn(false)
        `when`(playerService.getMaxPower(victimId)).thenReturn(30.0)
        val updated = givenVictim(role = "Owner", power = 25.0)

        die()

        assertEquals(24.0, updated.single().power)
    }

    @Test
    fun aDeathCapsPowerAtThePlayersMaxPower() {
        `when`(config.getBoolean("factions.leaderZeroPowerDisbandsFaction")).thenReturn(false)
        val updated = givenVictim(role = "Owner", power = 25.0)

        die()

        assertEquals(20.0, updated.single().power)
    }

    @Test
    fun aVictimWhosePowerIsFixedLosesNoPowerOnDeath() {
        // #1682: an admin has fixed the victim's power with /power setfixed
        givenVictim(role = "Owner", power = 1.0, fixedPower = 1.0)

        die()

        verify(playerService, never()).update(any(MfPlayer::class.java) ?: MfPlayer(victimId), anyTransform())
        verify(factionService, never()).delete(factionId)
    }

    @Test
    fun aKillerWhosePowerIsFixedGainsNoPowerOnAKill() {
        `when`(config.getDouble("players.powerGainedOnKill")).thenReturn(2.0)
        givenVictim(role = "Officer", power = 10.0)
        val killer = mock(Player::class.java)
        `when`(victim.killer).thenReturn(killer)
        val killerMfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()), power = 5.0, fixedPower = 5.0)
        `when`(playerService.getPlayer(killer)).thenReturn(killerMfPlayer)

        die()

        verify(playerService, never()).update(eqPlayer(killerMfPlayer), anyTransform())
        verify(killer, never()).sendMessage(anyString())
    }

    @Test
    fun aKillerWhosePowerIsNotFixedStillGainsPowerOnAKill() {
        `when`(config.getDouble("players.powerGainedOnKill")).thenReturn(2.0)
        givenVictim(role = "Officer", power = 10.0)
        val killer = mock(Player::class.java)
        `when`(victim.killer).thenReturn(killer)
        val killerMfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()), power = 5.0)
        `when`(playerService.getPlayer(killer)).thenReturn(killerMfPlayer)
        `when`(playerService.update(eqPlayer(killerMfPlayer), anyTransform())).thenReturn(Success(killerMfPlayer.copy(power = 7.0)))

        die()

        verify(playerService).update(eqPlayer(killerMfPlayer), anyTransform())
    }

    /** @return the players the death saved, filled in when [die] runs. */
    private fun givenVictim(role: String, power: Double, fixedPower: Double? = null): List<MfPlayer> {
        val updated = mutableListOf<MfPlayer>()
        val mfPlayer = MfPlayer(victimId, power = power, fixedPower = fixedPower)
        `when`(playerService.getPlayer(victim)).thenReturn(mfPlayer)
        `when`(playerService.update(eqPlayer(mfPlayer), anyTransform())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val transform = invocation.arguments[1] as (MfPlayer) -> MfPlayer
            Success(transform(mfPlayer).also(updated::add))
        }
        val factionRole = MfFactionRole(plugin, name = role)
        `when`(faction.getRole(victimId)).thenReturn(factionRole)
        return updated
    }

    private fun die() {
        val event = mock(PlayerDeathEvent::class.java)
        `when`(event.entity).thenReturn(victim)
        PlayerDeathListener(plugin).onPlayerDeath(event)
    }

    private fun eqPlayer(player: MfPlayer): MfPlayer = eq(player) ?: player

    private fun <T> anyTransform(): T {
        any<Any>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}
