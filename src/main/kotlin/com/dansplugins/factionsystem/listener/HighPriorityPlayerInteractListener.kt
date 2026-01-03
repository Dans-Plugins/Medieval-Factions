package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.protection.MfProtectionHelper
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

/**
 * A high-priority listener for PlayerInteractEvents.
 * * This listener runs at HIGHEST priority and is designed to be the last line of defense
 * for territory protection. It will only run if other plugins haven't cancelled the event,
 * providing protection for faction territories even if lower priority handlers allow it.
 * * The regular PlayerInteractListener handles interaction modes, locks, and special blocks,
 * while this one focuses solely on territory protection at a higher priority.
 */
class HighPriorityPlayerInteractListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Only check protection if event hasn't been cancelled by other protection systems
        if (event.isCancelled) return

        val clickedBlock = event.clickedBlock ?: return

        // Apply territory protection using shared helper
        MfProtectionHelper.applyTerritoryProtection(
            plugin = plugin,
            player = event.player,
            playerId = MfPlayerId(event.player.uniqueId.toString()),
            block = clickedBlock,
            item = event.item,
            event = event
        )
    }
}
