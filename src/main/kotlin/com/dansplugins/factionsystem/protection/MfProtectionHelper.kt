package com.dansplugins.factionsystem.protection

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Door
import org.bukkit.block.data.type.TrapDoor
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import java.util.logging.Level.SEVERE

/**
 * Helper class that contains shared territory protection logic.
 * This centralizes the protection code to avoid duplication between listeners.
 */
object MfProtectionHelper {

    // Metadata key to mark events as already processed
    private const val PROTECTION_PROCESSED_KEY = "mf_protection_processed"

    /**
     * Marks a player interact event as having been processed by the protection system.
     * This prevents duplicate protection checks between different listener priorities.
     *
     * The processing state is scoped to the specific event instance, not the entire
     * lifetime of the player, by storing an identifier derived from the event.
     *
     * @param plugin The MedievalFactions plugin instance
     * @param event The event to mark
     */
    fun markEventProcessed(plugin: MedievalFactions, event: PlayerInteractEvent) {
        val eventId = System.identityHashCode(event)
        event.player.setMetadata(PROTECTION_PROCESSED_KEY, FixedMetadataValue(plugin, eventId))
    }

    /**
     * Checks if an event has already been processed by the protection system.
     *
     * The check is performed against an identifier derived from the current event,
     * so metadata from previous events does not cause this method to return true.
     *
     * @param event The event to check
     * @return True if the event has already been processed
     */
    fun isEventProcessed(event: PlayerInteractEvent): Boolean {
        val eventId = System.identityHashCode(event)
        val metadataList = event.player.getMetadata(PROTECTION_PROCESSED_KEY)
        return metadataList.any { it.value() == eventId }
    }

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

        // Allow eating food in faction territory without triggering protection
        if (item != null) {
            if (item.type.isEdible && !block.type.isInteractable) return true
        }

        // Allow door/trapdoor interaction if configured
        if (plugin.config.getBoolean("factions.nonMembersCanInteractWithDoors")) {
            val blockData = block.blockData
            if (blockData is Door || blockData is TrapDoor) {
                return true
            }
        }

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
