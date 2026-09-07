package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.logging.Level.SEVERE

class PlayerQuitListener(
    private val plugin: MedievalFactions,
    private val entityInteractionProtection: EntityInteractionProtection
) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        entityInteractionProtection.forgetPlayer(event.player.uniqueId)

        val teleportService = plugin.services.teleportService
        teleportService.cancelTeleportation(event.player)

        val playerService = plugin.services.playerService
        // The player is snapshotted on the server thread: the Bukkit player is unloaded once this
        // listener returns, and powerAtLogout has to record the power held at the moment of the quit.
        val player = playerService.getPlayer(event.player) ?: MfPlayer(plugin, event.player)
        val playerToSave = player.copy(powerAtLogout = player.power)

        // Unloading is kept on the server thread. It only evicts an in-memory entry, so it does not
        // depend on the save, and doing it here keeps it ordered before the loadInteractionStatus
        // that a reconnect performs in AsyncPlayerPreLoginListener.
        val interactionService = plugin.services.interactionService
        interactionService.unloadInteractionStatus(player.id)

        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                playerService.save(playerToSave).onFailure {
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            }
        )
    }
}
