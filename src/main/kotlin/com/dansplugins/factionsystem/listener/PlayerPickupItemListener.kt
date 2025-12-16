package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerPickupItemEvent
import java.util.logging.Level.SEVERE

@Suppress("DEPRECATION") // PlayerPickupItemEvent is deprecated but still used by many plugins
class PlayerPickupItemListener(
    private val plugin: MedievalFactions
) : Listener {
    @EventHandler
    fun onPlayerPickupItem(event: PlayerPickupItemEvent) {
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(event.player)
        if (mfPlayer == null) {
            event.isCancelled = true
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    playerService.save(MfPlayer(plugin, event.player)).onFailure {
                        event.player.sendMessage("$RED${plugin.language["PlayerPickupItemFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
            return
        }

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(event.item.location.chunk)
        if (claim == null) {
            if (plugin.config.getBoolean("wilderness.interaction.prevent", false)) {
                event.isCancelled = true
                if (plugin.config.getBoolean("wilderness.interaction.alert", true)) {
                    event.player.sendMessage("$RED${plugin.language["CannotPickupItemInWilderness"]}")
                }
            }
            return
        }

        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return

        if (!claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            if (mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) {
                event.player.sendMessage("$RED${plugin.language["FactionTerritoryProtectionBypassed"]}")
            } else {
                event.isCancelled = true
                event.player.sendMessage("$RED${plugin.language["CannotPickupItemInFactionTerritory", claimFaction.name]}")
            }
        }
    }
}
