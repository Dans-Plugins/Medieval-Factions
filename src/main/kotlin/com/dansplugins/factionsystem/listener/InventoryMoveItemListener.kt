package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.BlockInventoryHolder

class InventoryMoveItemListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        // Stop hoppers from taking items from locked blocks.
        val sourceInventoryHolder = event.source.holder
        val lockService = plugin.services.lockService
        
        // Handle both single block inventories and double chests
        val blocksToCheck = when (sourceInventoryHolder) {
            is BlockInventoryHolder -> listOf(sourceInventoryHolder.block)
            is DoubleChest -> {
                val left = sourceInventoryHolder.leftSide as? Chest
                val right = sourceInventoryHolder.rightSide as? Chest
                listOfNotNull(left?.block, right?.block)
            }
            else -> return
        }
        
        // Check if any of the blocks are locked
        for (block in blocksToCheck) {
            val lockedBlock = lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(block))
            if (lockedBlock != null) {
                event.isCancelled = true
                return
            }
        }
    }
}
