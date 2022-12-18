package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.BlockInventoryHolder

class InventoryMoveItemListener(private val plugin: MedievalFactions) : Listener {

    /**
     * An event that fires whenever an item moves from one inventory to another.
     *
     * Hi Ren, testing out KDoc comments :D
     */
    @EventHandler
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        // Stop hoppers from taking items from locked blocks.
        val targetInventoryHolder = event.destination.holder
        if (targetInventoryHolder !is BlockInventoryHolder) return
        val lockService = plugin.services.lockService
        val lockedBlock = lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(targetInventoryHolder.block))
        if (lockedBlock != null && event.destination.type == InventoryType.HOPPER) {
            event.isCancelled = true
            return
        }
    }
}
