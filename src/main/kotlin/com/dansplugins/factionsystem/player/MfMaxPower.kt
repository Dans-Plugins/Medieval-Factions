package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.isLeader
import org.bukkit.configuration.Configuration

/**
 * The single place that decides a player's maximum power (#1945).
 *
 * Every player is capped at `players.maxPower`, except faction leaders (members holding their faction's Owner role,
 * see [isLeader]) when `players.leaderMaxPower` is set: they are capped at that value instead. With
 * `players.leaderMaxPower` absent (the default), every player, leaders included, is capped at `players.maxPower`
 * exactly as before.
 *
 * A player who stops being a leader keeps the power they had; on the next scheduled power update anything above
 * their new maximum is clamped down to it ([MfPlayerRepository.clampPowerToMax]). The same applies to a leader whose
 * power is above a `players.leaderMaxPower` that was lowered.
 */
object MfMaxPower {

    const val MAX_POWER_PATH = "players.maxPower"
    const val LEADER_MAX_POWER_PATH = "players.leaderMaxPower"

    /** `players.maxPower`: the maximum power of every player who is not a leader. */
    fun maxPower(config: Configuration): Double = config.getDouble(MAX_POWER_PATH)

    /**
     * `players.leaderMaxPower`, or null when leaders use `players.maxPower`. The setting counts as unset when it is
     * absent, not a number, or not above 0 (power gain divides by the maximum, so 0 or less cannot be used).
     */
    fun leaderMaxPower(config: Configuration): Double? =
        (config.get(LEADER_MAX_POWER_PATH) as? Number)?.toDouble()?.takeIf { it > 0.0 }

    /** The maximum power of [playerId], whose faction is [faction] (null when the player is in no faction). */
    fun forPlayer(config: Configuration, faction: MfFaction?, playerId: MfPlayerId): Double {
        val leaderMaxPower = leaderMaxPower(config) ?: return maxPower(config)
        return if (faction != null && faction.isLeader(playerId)) leaderMaxPower else maxPower(config)
    }

    /**
     * The players in [factions] whose maximum power is `players.leaderMaxPower`; empty when the setting is unset,
     * so callers can pass it straight to the power update.
     */
    fun leaderIds(config: Configuration, factions: Collection<MfFaction>): Set<MfPlayerId> {
        if (leaderMaxPower(config) == null) return emptySet()
        return factions.flatMap { faction ->
            faction.members.map { it.playerId }.filter(faction::isLeader)
        }.toSet()
    }
}
