package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.isLeader
import com.dansplugins.factionsystem.faction.role.MfFactionRoles.Companion.OWNER_ROLE_NAME
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.logging.Level.SEVERE
import kotlin.math.abs

class PlayerDeathListener(private val plugin: MedievalFactions) : Listener {

    private val decimalFormat = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(plugin.language.locale))

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val powerLostOnDeath = plugin.config.getDouble("players.powerLostOnDeath")
        val powerGainedOnKill = plugin.config.getDouble("players.powerGainedOnKill")
        val disbandZeroPowerFactions = plugin.config.getBoolean("factions.zeroPowerFactionsGetDisbanded")
        val grantPowerToKillerIfVictimHasZeroPower = plugin.config.getBoolean("pvp.grantPowerToKillerIfVictimHasZeroPower")
        val leaderZeroPowerDisbandsFaction = plugin.config.getBoolean("factions.leaderZeroPowerDisbandsFaction")

        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val factionService = plugin.services.factionService

                val victim = event.entity
                val victimMfPlayer = playerService.getPlayer(victim) ?: MfPlayer(plugin, victim)

                if (!grantPowerToKillerIfVictimHasZeroPower && victimMfPlayer.power < powerLostOnDeath) {
                    return@Runnable
                }

                if (abs(powerLostOnDeath) > 0.00001) {
                    val powerBeforeDeath = victimMfPlayer.power
                    val updatedVictim = removePowerFromVictim(
                        playerService,
                        victim,
                        victimMfPlayer,
                        powerLostOnDeath
                    )
                    val victimFaction = factionService.getFaction(victimMfPlayer.id)
                    if (disbandZeroPowerFactions && victimFaction != null && victimFaction.power <= 0.0) {
                        disband(victimFaction, factionService)
                    } else if (leaderZeroPowerDisbandsFaction && victimFaction != null &&
                        isLeaderDeathAtZeroPower(victimFaction, victimMfPlayer.id, powerBeforeDeath, updatedVictim?.power)
                    ) {
                        disbandForLeaderDeath(victimFaction, victim.name, victimMfPlayer.id, factionService)
                    }
                }

                val killer = victim.killer ?: return@Runnable
                val killerMfPlayer = playerService.getPlayer(killer) ?: MfPlayer(plugin, killer)
                if (abs(powerGainedOnKill) > 0.00001) {
                    addPowerToKiller(
                        playerService,
                        killer,
                        killerMfPlayer,
                        powerGainedOnKill
                    )
                    val killerFaction = factionService.getFaction(killerMfPlayer.id)
                    if (disbandZeroPowerFactions && killerFaction != null && killerFaction.power <= 0.0) {
                        disband(killerFaction, factionService)
                    }
                }
            }
        )
    }

    private fun disband(
        killerFaction: MfFaction,
        factionService: MfFactionService
    ) {
        killerFaction.sendMessage(
            plugin.language["FactionDisbandedZeroPowerNotificationTitle"],
            plugin.language["FactionDisbandedZeroPowerNotificationBody"]
        )
        factionService.delete(killerFaction.id).onFailure {
            plugin.logger.log(SEVERE, "Failed to delete faction: ${it.reason.message}", it.reason.cause)
            return
        }
    }

    /**
     * `factions.leaderZeroPowerDisbandsFaction` (#1945): with it on, a faction is disbanded when a member holding its
     * Owner role is brought to 0 power by this death. Power lost any other way (offline decay over time, `/power set`)
     * never triggers it, because this listener is the only place it is checked; a leader who was already at or below
     * 0 before dying does not trigger it either.
     */
    private fun disbandForLeaderDeath(
        faction: MfFaction,
        leaderName: String,
        leaderId: MfPlayerId,
        factionService: MfFactionService
    ) {
        plugin.logger.info(
            "Disbanding faction ${faction.name} (${faction.id.value}): its leader $leaderName (${leaderId.value}) " +
                "reached 0 power on death and factions.leaderZeroPowerDisbandsFaction is true"
        )
        faction.sendMessage(
            plugin.language["FactionDisbandedLeaderDeathNotificationTitle"],
            plugin.language["FactionDisbandedLeaderDeathNotificationBody", faction.name, leaderName]
        )
        factionService.delete(faction.id).onFailure {
            plugin.logger.log(SEVERE, "Failed to delete faction: ${it.reason.message}", it.reason.cause)
            return
        }
    }

    /** @return the victim after losing power, or null when their power did not change. */
    private fun removePowerFromVictim(
        playerService: MfPlayerService,
        victim: Player,
        victimMfPlayer: MfPlayer,
        powerLostOnDeath: Double
    ): MfPlayer? {
        val maxPower = playerService.getMaxPower(victimMfPlayer.id)
        val newPower = (victimMfPlayer.power - powerLostOnDeath)
            .coerceAtLeast(0.0)
            .coerceAtMost(maxPower)
        if (abs(newPower - victimMfPlayer.power) < 0.00001) {
            return null
        }
        val updatedVictim = playerService.update(victimMfPlayer) { current ->
            current.copy(
                power = (current.power - powerLostOnDeath)
                    .coerceAtLeast(0.0)
                    .coerceAtMost(maxPower)
            )
        }.onFailure {
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return null
        }

        victim.sendMessage("$RED${plugin.language["PowerLostOnDeath", decimalFormat.format(powerLostOnDeath)]}")
        return updatedVictim
    }

    private fun addPowerToKiller(
        playerService: MfPlayerService,
        killer: Player,
        killerMfPlayer: MfPlayer,
        powerGainedOnKill: Double
    ) {
        val maxPower = playerService.getMaxPower(killerMfPlayer.id)
        val newPower = (killerMfPlayer.power + powerGainedOnKill)
            .coerceAtLeast(0.0)
            .coerceAtMost(maxPower)
        if (abs(newPower - killerMfPlayer.power) < 0.00001) {
            return
        }
        playerService.update(killerMfPlayer) { current ->
            current.copy(
                power = (current.power + powerGainedOnKill)
                    .coerceAtLeast(0.0)
                    .coerceAtMost(maxPower)
            )
        }.onFailure {
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return
        }

        killer.sendMessage("$GREEN${plugin.language["PowerGainedOnKill", decimalFormat.format(powerGainedOnKill)]}")
    }
}

/**
 * Whether a death takes a faction leader to 0 power: [playerId] holds the faction's Owner role (the role named
 * [OWNER_ROLE_NAME], matched as `/faction admin setleader` matches it), had more than 0 power before the death, and
 * has 0 or less after it. [powerAfterDeath] is null when the death did not change the player's power.
 */
internal fun isLeaderDeathAtZeroPower(
    faction: MfFaction,
    playerId: MfPlayerId,
    powerBeforeDeath: Double,
    powerAfterDeath: Double?
): Boolean {
    if (powerAfterDeath == null || powerBeforeDeath <= 0.0 || powerAfterDeath > 0.0) return false
    return faction.isLeader(playerId)
}
