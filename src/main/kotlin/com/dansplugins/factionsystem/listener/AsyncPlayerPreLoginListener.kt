package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import java.util.logging.Level.SEVERE

class AsyncPlayerPreLoginListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        val playerService = plugin.services.playerService
        val playerId = MfPlayerId(event.uniqueId.toString())
        val player = playerService.getPlayer(playerId)
            ?: playerService.save(MfPlayer(plugin, playerId, event.name))
                .onFailure {
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return
                }
        if (player.name != event.name) {
            playerService.save(player.copy(name = event.name)).onFailure {
                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return
            }
        }

        val interactionService = plugin.services.interactionService
        interactionService.loadInteractionStatus(player.id)
    }
}
