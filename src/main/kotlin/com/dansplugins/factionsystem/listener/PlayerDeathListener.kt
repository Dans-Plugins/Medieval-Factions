package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.player.MfPlayer
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
                    removePowerFromVictim(
                        playerService,
                        victim,
                        victimMfPlayer,
                        powerLostOnDeath
                    )
                    val victimFaction = factionService.getFaction(victimMfPlayer.id)
                    if (disbandZeroPowerFactions && victimFaction != null && victimFaction.power <= 0.0) {
                        disband(victimFaction, factionService)
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

    private fun removePowerFromVictim(
        playerService: MfPlayerService,
        victim: Player,
        victimMfPlayer: MfPlayer,
        powerLostOnDeath: Double
    ) {
        val newPower = (victimMfPlayer.power - powerLostOnDeath)
            .coerceAtLeast(0.0)
            .coerceAtMost(plugin.config.getDouble("players.maxPower"))
        if (abs(newPower - victimMfPlayer.power) < 0.00001) {
            return
        }
        playerService.save(
            victimMfPlayer.copy(power = newPower)
        ).onFailure {
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return
        }

        victim.sendMessage("$RED${plugin.language["PowerLostOnDeath", decimalFormat.format(powerLostOnDeath)]}")
    }

    private fun addPowerToKiller(
        playerService: MfPlayerService,
        killer: Player,
        killerMfPlayer: MfPlayer,
        powerGainedOnKill: Double
    ) {
        val newPower = (killerMfPlayer.power + powerGainedOnKill)
            .coerceAtLeast(0.0)
            .coerceAtMost(plugin.config.getDouble("players.maxPower"))
        if (abs(newPower - killerMfPlayer.power) < 0.00001) {
            return
        }
        playerService.save(
            killerMfPlayer.copy(power = newPower)
        ).onFailure {
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return
        }

        killer.sendMessage("$GREEN${plugin.language["PowerGainedOnKill", decimalFormat.format(powerGainedOnKill)]}")
    }
}
