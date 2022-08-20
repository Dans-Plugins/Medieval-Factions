package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent

class AsyncPlayerPreLoginListener(private val playerService: MfPlayerService) : Listener {

    @EventHandler
    fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        val playerId = MfPlayerId(event.uniqueId.toString())
        if (playerService.getPlayer(playerId) == null) {
            playerService.save(MfPlayer(playerId))
        }
    }

}