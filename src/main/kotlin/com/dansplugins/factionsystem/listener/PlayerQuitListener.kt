package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.logging.Level.SEVERE

class PlayerQuitListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerService = plugin.services.playerService
        val player = playerService.getPlayer(event.player) ?: playerService.save(MfPlayer.fromBukkit(event.player)).onFailure {
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return
        }

        val interactionService = plugin.services.interactionService
        interactionService.unloadInteractionStatus(player.id)
    }

}