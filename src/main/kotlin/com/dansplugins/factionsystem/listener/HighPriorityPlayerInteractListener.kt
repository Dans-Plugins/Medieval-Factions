package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.logging.Level.SEVERE

class HighPriorityPlayerInteractListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Only check protection if event hasn't been cancelled by other protection systems
        if (event.isCancelled) return

        val clickedBlock = event.clickedBlock ?: return
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(event.player)
        if (mfPlayer == null) {
            event.isCancelled = true
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    playerService.save(MfPlayer(plugin, event.player)).onFailure {
                        event.player.sendMessage("$RED${plugin.language["BlockInteractFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
            return
        }

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(clickedBlock.chunk)
        if (claim == null) {
            if (plugin.config.getBoolean("wilderness.interaction.prevent", false)) {
                event.isCancelled = true
                if (plugin.config.getBoolean("wilderness.interaction.alert", true)) {
                    event.player.sendMessage("$RED${plugin.language["CannotInteractBlockInWilderness"]}")
                }
            }
            return
        }

        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return

        // Check if player is allowed to interact based on faction relationships
        if (!claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            if (mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) {
                event.player.sendMessage("$RED${plugin.language["FactionTerritoryProtectionBypassed"]}")
            } else {
                val playerFaction = factionService.getFaction(mfPlayer.id)
                val relationshipService = plugin.services.factionRelationshipService
                // Check if player is at war and trying to place a ladder
                if (playerFaction != null && 
                    relationshipService.getFactionsAtWarWith(playerFaction.id).contains(claimFaction.id) &&
                    event.action == Action.RIGHT_CLICK_BLOCK &&
                    event.item?.type == Material.LADDER &&
                    plugin.config.getBoolean("factions.laddersPlaceableInEnemyFactionTerritory")) {
                    // Allow ladder placement in enemy territory during wartime
                    return
                }
                event.isCancelled = true
                event.player.sendMessage("$RED${plugin.language["CannotInteractWithBlockInFactionTerritory", claimFaction.name]}")
            }
        }
    }
}
