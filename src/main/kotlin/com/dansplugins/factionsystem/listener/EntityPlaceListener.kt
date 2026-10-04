package com.dansplugins.factionsystem.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPlaceEvent

/**
 * Protects claims from boats, minecarts, armour stands and end crystals placed by a player (#2002).
 * The claim checked is the one holding the block the entity is placed against.
 */
class EntityPlaceListener(private val protection: WorldActionProtection) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onEntityPlace(event: EntityPlaceEvent) {
        val player = event.player ?: return
        val item = player.inventory.getItem(event.hand)?.type
        protection.protect(event, player, event.block, item)
    }
}
