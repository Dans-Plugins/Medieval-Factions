package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.JooqMfPlayerRepositoryTest.Companion.migratedH2Dsl
import com.dansplugins.factionsystem.player.JooqMfPlayerRepositoryTest.Companion.powerConfiguredPlugin
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.configuration.Configuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

/**
 * Covers `players.powerUpdateIntervalMinutes`, `players.disablePowerGainOverTime` and
 * `players.disablePowerDecayOverTime` (#1626, #1678).
 */
class MfPowerUpdateSettingsTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var config: Configuration
    private lateinit var repository: JooqMfPlayerRepository
    private lateinit var service: MfPlayerService

    @BeforeEach
    fun setUp() {
        plugin = powerConfiguredPlugin()
        config = plugin.config
        val services = mock(Services::class.java)
        `when`(services.mapService).thenReturn(null)
        `when`(plugin.services).thenReturn(services)
        repository = JooqMfPlayerRepository(plugin, migratedH2Dsl())
        service = MfPlayerService(plugin, repository)
    }

    @Test
    fun `an unset or invalid interval falls back to 15 minutes`() {
        assertEquals(15, MfPowerUpdateSettings.intervalMinutes(config))
        `when`(config.getInt("players.powerUpdateIntervalMinutes")).thenReturn(-3)
        assertEquals(15, MfPowerUpdateSettings.intervalMinutes(config))
        assertEquals(0.25, MfPowerUpdateSettings.intervalHours(config))
    }

    @Test
    fun `a configured interval sets the time step of one update`() {
        `when`(config.getInt("players.powerUpdateIntervalMinutes")).thenReturn(30)
        assertEquals(30, MfPowerUpdateSettings.intervalMinutes(config))
        assertEquals(0.5, MfPowerUpdateSettings.intervalHours(config))
    }

    @Test
    fun `gain and decay over time are on unless disabled`() {
        assertTrue(MfPowerUpdateSettings.isGainOverTimeEnabled(config))
        assertTrue(MfPowerUpdateSettings.isDecayOverTimeEnabled(config))
        `when`(config.getBoolean("players.disablePowerGainOverTime")).thenReturn(true)
        `when`(config.getBoolean("players.disablePowerDecayOverTime")).thenReturn(true)
        assertFalse(MfPowerUpdateSettings.isGainOverTimeEnabled(config))
        assertFalse(MfPowerUpdateSettings.isDecayOverTimeEnabled(config))
    }

    @Test
    fun `one 30-minute update moves power as far as two 15-minute updates`() {
        val stepped = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))
        repository.increaseOnlinePlayerPower(listOf(stepped.id))
        repository.increaseOnlinePlayerPower(listOf(stepped.id))
        val afterTwoQuarterHours = repository.getPlayer(stepped.id)!!.power

        `when`(config.getInt("players.powerUpdateIntervalMinutes")).thenReturn(30)
        val single = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))
        repository.increaseOnlinePlayerPower(listOf(single.id))
        val afterOneHalfHour = repository.getPlayer(single.id)!!.power

        assertTrue(afterTwoQuarterHours > 5.0, "power should have grown, was $afterTwoQuarterHours")
        assertEquals(afterTwoQuarterHours, afterOneHalfHour, 0.0001)
    }

    @Test
    fun `by default the update raises online power and lowers offline power`() {
        val online = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))
        val offline = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))

        service.updatePlayerPower(listOf(online.id))

        assertTrue(repository.getPlayer(online.id)!!.power > 5.0)
        assertTrue(repository.getPlayer(offline.id)!!.power < 5.0)
    }

    @Test
    fun `disabling decay over time keeps offline power and still raises online power`() {
        `when`(config.getBoolean("players.disablePowerDecayOverTime")).thenReturn(true)
        val online = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))
        val offline = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))

        service.updatePlayerPower(listOf(online.id))

        assertTrue(repository.getPlayer(online.id)!!.power > 5.0)
        assertEquals(5.0, repository.getPlayer(offline.id)!!.power)
    }

    @Test
    fun `disabling gain over time keeps online power and still lowers offline power`() {
        `when`(config.getBoolean("players.disablePowerGainOverTime")).thenReturn(true)
        val online = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))
        val offline = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))

        service.updatePlayerPower(listOf(online.id))

        assertEquals(5.0, repository.getPlayer(online.id)!!.power)
        assertTrue(repository.getPlayer(offline.id)!!.power < 5.0)
    }

    private fun saved(player: MfPlayer) = (service.save(player) as Success).value

    private fun newId() = MfPlayerId(UUID.randomUUID().toString())
}
