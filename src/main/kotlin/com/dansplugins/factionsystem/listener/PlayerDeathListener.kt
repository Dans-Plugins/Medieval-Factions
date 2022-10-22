package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
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
import java.util.logging.Level.SEVERE

class PlayerDeathListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val powerLostOnDeath = plugin.config.getInt("players.powerLostOnDeath")
        val powerGainedOnKill = plugin.config.getInt("players.powerGainedOnKill")
        val disbandZeroPowerFactions = plugin.config.getBoolean("factions.zeroPowerFactionsGetDisbanded")

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val factionService = plugin.services.factionService
            val claimService = plugin.services.claimService

            val victim = event.entity
            val victimMfPlayer = playerService.getPlayer(victim) ?: MfPlayer(plugin, victim)
            if (powerLostOnDeath != 0) {
                removePowerFromVictim(
                    playerService,
                    victim,
                    victimMfPlayer,
                    powerLostOnDeath
                )
                val victimFaction = factionService.getFaction(victimMfPlayer.id)
                if (disbandZeroPowerFactions && victimFaction != null && victimFaction.power <= 0) {
                    disband(victimFaction, claimService, factionService)
                }
            }

            val killer = victim.killer ?: return@Runnable
            val killerMfPlayer = playerService.getPlayer(killer) ?: MfPlayer(plugin, killer)
            if (powerGainedOnKill != 0) {
                addPowerToKiller(
                    playerService,
                    killer,
                    killerMfPlayer,
                    powerGainedOnKill
                )
                val killerFaction = factionService.getFaction(killerMfPlayer.id)
                if (disbandZeroPowerFactions && killerFaction != null && killerFaction.power <= 0) {
                    disband(killerFaction, claimService, factionService)
                }
            }
        })
    }

    private fun disband(
        killerFaction: MfFaction,
        claimService: MfClaimService,
        factionService: MfFactionService
    ) {
        killerFaction.sendMessage(
            plugin.language["FactionDisbandedZeroPowerNotificationTitle"],
            plugin.language["FactionDisbandedZeroPowerNotificationBody"]
        )
        claimService.deleteAllClaims(killerFaction.id).onFailure {
            plugin.logger.log(SEVERE, "Failed to delete all claims for faction: ${it.reason.message}", it.reason.cause)
            return
        }
        factionService.delete(killerFaction.id).onFailure {
            plugin.logger.log(SEVERE, "Failed to delete faction: ${it.reason.message}", it.reason.cause)
            return
        }
    }

    private fun removePowerFromVictim(
        playerService: MfPlayerService,
        victim: Player,
        victimMfPlayer: MfPlayer,
        powerLostOnDeath: Int
    ) {
        val newPower = (victimMfPlayer.power - powerLostOnDeath)
            .coerceAtLeast(0)
            .coerceAtMost(plugin.config.getInt("players.maxPower"))
        if (newPower == victimMfPlayer.power) {
            return
        }
        playerService.save(
            victimMfPlayer.copy(power = newPower)
        ).onFailure {
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return
        }

        victim.sendMessage("$RED${plugin.language["PowerLostOnDeath", powerLostOnDeath.toString()]}")
    }

    private fun addPowerToKiller(
        playerService: MfPlayerService,
        killer: Player,
        killerMfPlayer: MfPlayer,
        powerGainedOnKill: Int
    ) {
        val newPower = (killerMfPlayer.power + powerGainedOnKill)
            .coerceAtLeast(0)
            .coerceAtMost(plugin.config.getInt("players.maxPower"))
        if (newPower == killerMfPlayer.power) {
            return
        }
        playerService.save(
            killerMfPlayer.copy(power = newPower)
        ).onFailure {
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return
        }

        killer.sendMessage("$GREEN${plugin.language["PowerGainedOnKill", powerGainedOnKill.toString()]}")
    }
}