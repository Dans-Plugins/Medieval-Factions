package com.dansplugins.factionsystem.player

import org.bukkit.configuration.Configuration

/**
 * Reads the settings that control the scheduled power update (`players.powerUpdateIntervalMinutes`,
 * `players.disablePowerGainOverTime` and `players.disablePowerDecayOverTime`).
 */
object MfPowerUpdateSettings {

    const val DEFAULT_INTERVAL_MINUTES = 15

    /**
     * Minutes between power updates. A missing, zero or negative value falls back to the
     * shipped default of 15 minutes, so a typo cannot stop or flood the update.
     */
    fun intervalMinutes(config: Configuration): Int =
        config.getInt("players.powerUpdateIntervalMinutes").takeIf { it > 0 } ?: DEFAULT_INTERVAL_MINUTES

    /** The time step, in hours, that one power update advances the gain and decay curves by. */
    fun intervalHours(config: Configuration): Double = intervalMinutes(config) / 60.0

    fun isGainOverTimeEnabled(config: Configuration): Boolean =
        !config.getBoolean("players.disablePowerGainOverTime")

    fun isDecayOverTimeEnabled(config: Configuration): Boolean =
        !config.getBoolean("players.disablePowerDecayOverTime")
}
