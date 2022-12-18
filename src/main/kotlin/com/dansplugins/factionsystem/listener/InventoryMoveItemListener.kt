package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.BlockInventoryHolder

class InventoryMoveItemListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        // Stop hoppers from taking items from locked blocks.
        val sourceInventoryHolder = event.source.holder
        if (sourceInventoryHolder !is BlockInventoryHolder) return
        val lockService = plugin.services.lockService
        val lockedBlock = lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(sourceInventoryHolder.block))
        if (lockedBlock != null) {
            event.isCancelled = true
            return
        }
    }
}
