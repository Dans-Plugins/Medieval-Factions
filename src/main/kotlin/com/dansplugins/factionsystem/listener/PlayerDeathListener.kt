package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
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

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService

            val victim = event.entity
            if (powerLostOnDeath != 0) {
                removePowerFromVictim(playerService, victim, powerLostOnDeath)
            }

            val killer = victim.killer ?: return@Runnable
            if (powerGainedOnKill != 0) {
                addPowerToKiller(playerService, killer, powerGainedOnKill)
            }
        })
    }

    private fun removePowerFromVictim(
        playerService: MfPlayerService,
        victim: Player,
        powerLostOnDeath: Int
    ) {
        val mfPlayerVictim = playerService.getPlayer(victim) ?: MfPlayer(plugin, victim)
        val newPower = (mfPlayerVictim.power - powerLostOnDeath)
            .coerceAtLeast(0)
            .coerceAtMost(plugin.config.getInt("players.maxPower"))
        if (newPower == mfPlayerVictim.power) {
            return
        }
        playerService.save(
            mfPlayerVictim.copy(power = newPower)
        ).onFailure {
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return
        }

        victim.sendMessage("$RED${plugin.language["PowerLostOnDeath", powerLostOnDeath.toString()]}")
    }

    private fun addPowerToKiller(
        playerService: MfPlayerService,
        killer: Player,
        powerGainedOnKill: Int
    ) {
        val mfPlayerKiller = playerService.getPlayer(killer) ?: MfPlayer(plugin, killer)
        val newPower = (mfPlayerKiller.power + powerGainedOnKill)
            .coerceAtLeast(0)
            .coerceAtMost(plugin.config.getInt("players.maxPower"))
        if (newPower == mfPlayerKiller.power) {
            return
        }
        playerService.save(
            mfPlayerKiller.copy(power = newPower)
        ).onFailure {
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return
        }

        killer.sendMessage("$GREEN${plugin.language["PowerGainedOnKill", powerGainedOnKill.toString()]}")
    }
}