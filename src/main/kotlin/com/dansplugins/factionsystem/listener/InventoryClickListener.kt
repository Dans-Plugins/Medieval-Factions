package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.BlockInventoryHolder
import java.util.logging.Level.SEVERE

class InventoryClickListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (player !is org.bukkit.entity.Player) return

        val inventory = event.inventory
        val holder = inventory.holder

        // Check if the inventory belongs to a block in claimed territory
        val block: Block? = when (holder) {
            is BlockInventoryHolder -> holder.block
            is BlockState -> holder.block
            else -> null
        }

        if (block == null) return

        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(player)
        if (mfPlayer == null) {
            event.isCancelled = true
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    playerService.save(MfPlayer(plugin, player)).onFailure {
                        player.sendMessage("$RED${plugin.language["InventoryClickFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
            return
        }

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(block.chunk)
        if (claim == null) {
            if (plugin.config.getBoolean("wilderness.interaction.prevent", false)) {
                event.isCancelled = true
                if (plugin.config.getBoolean("wilderness.interaction.alert", true)) {
                    player.sendMessage("$RED${plugin.language["CannotInteractWithInventoryInWilderness"]}")
                }
            }
            return
        }

        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return

        if (!claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            if (mfPlayer.isBypassEnabled && player.hasPermission("mf.bypass")) {
                player.sendMessage("$RED${plugin.language["FactionTerritoryProtectionBypassed"]}")
            } else {
                event.isCancelled = true
                player.sendMessage("$RED${plugin.language["CannotInteractWithInventoryInFactionTerritory", claimFaction.name]}")
            }
        }
    }
}
