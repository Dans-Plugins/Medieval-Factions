package com.dansplugins.factionsystem.protection

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.logging.Level.SEVERE

/**
 * Helper class that contains shared territory protection logic.
 * This centralizes the protection code to avoid duplication between listeners.
 */
object MfProtectionHelper {

    /**
     * Applies territory protection logic for player interactions with blocks
     *
     * @param plugin The MedievalFactions plugin instance
     * @param player The player interacting with a block
     * @param playerId The ID of the player interacting with a block
     * @param block The block being interacted with
     * @param item The item the player is holding (may be null)
     * @param event The PlayerInteractEvent to potentially cancel
     * @return true if the interaction was allowed, false if it was denied
     */
    fun applyTerritoryProtection(
        plugin: MedievalFactions,
        player: Player,
        playerId: MfPlayerId,
        block: Block,
        item: ItemStack?,
        event: PlayerInteractEvent
    ): Boolean {
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(player)

        if (mfPlayer == null) {
            event.isCancelled = true
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    playerService.save(MfPlayer(plugin, player)).onFailure {
                        player.sendMessage("$RED${plugin.language["BlockInteractFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
            return false
        }

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(block.chunk)

        if (claim == null) {
            if (plugin.config.getBoolean("wilderness.interaction.prevent", false)) {
                event.isCancelled = true
                if (plugin.config.getBoolean("wilderness.interaction.alert", true)) {
                    player.sendMessage("$RED${plugin.language["CannotInteractBlockInWilderness"]}")
                }
                return false
            }
            return true
        }

        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return true

        // Check if player is allowed to interact based on faction relationships
        if (!claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            if (mfPlayer.isBypassEnabled && player.hasPermission("mf.bypass")) {
                player.sendMessage("$RED${plugin.language["FactionTerritoryProtectionBypassed"]}")
                return true
            } else {
                // Check if player is at war and trying to place a ladder
                if (claimService.isWartimeLadderPlacementAllowed(
                        mfPlayer.id,
                        claim,
                        item?.type == Material.LADDER
                    )
                ) {
                    // Allow ladder placement in enemy territory during wartime
                    return true
                }
                event.isCancelled = true
                player.sendMessage("$RED${plugin.language["CannotInteractWithBlockInFactionTerritory", claimFaction.name]}")
                return false
            }
        }

        return true
    }
}
