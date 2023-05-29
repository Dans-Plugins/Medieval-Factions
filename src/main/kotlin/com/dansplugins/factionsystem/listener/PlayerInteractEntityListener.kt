package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import java.util.logging.Level

class PlayerInteractEntityListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(event.player)
        if (mfPlayer == null) {
            event.isCancelled = true
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    playerService.save(MfPlayer(plugin, event.player)).onFailure {
                        event.player.sendMessage("${ChatColor.RED}${plugin.language["PlayerInteractEntityFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
            return
        }

        val clickedEntity = event.rightClicked
        val clickedEntityType = clickedEntity.type
        if (clickedEntityType == EntityType.VILLAGER) {
            val claimService = plugin.services.claimService
            val claim = claimService.getClaim(clickedEntity.location.chunk) ?: return
            val factionService = plugin.services.factionService
            val claimFaction = factionService.getFaction(claim.factionId) ?: return

            val protectVillagerFlagValue = claimFaction.flags[plugin.flags.protectVillagerTrade]
            if (protectVillagerFlagValue && !claimService.isInteractionAllowed(mfPlayer.id, claim)) {
                if (mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) {
                    event.player.sendMessage("${ChatColor.RED}${plugin.language["FactionTerritoryProtectionBypassed"]}")
                } else {
                    event.isCancelled = true
                    event.player.sendMessage("${ChatColor.RED}${plugin.language["PlayerInteractEntityCannotTradeWithVillager", claimFaction.name]}")
                    return
                }
            }
        }
    }
}
