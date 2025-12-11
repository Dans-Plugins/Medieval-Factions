package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.BlockInventoryHolder

class InventoryMoveItemListener(
    private val plugin: MedievalFactions,
) : Listener {
    @EventHandler
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        // Stop hoppers from taking items from or putting items into locked blocks.
        val lockService = plugin.services.lockService

        // Check source inventory (where items are being taken from)
        val sourceInventoryHolder = event.source.holder
        val sourceBlocksToCheck =
            when (sourceInventoryHolder) {
                is BlockInventoryHolder -> listOf(sourceInventoryHolder.block)
                is DoubleChest -> {
                    val left = sourceInventoryHolder.leftSide as? Chest
                    val right = sourceInventoryHolder.rightSide as? Chest
                    listOfNotNull(left?.block, right?.block)
                }
                else -> emptyList()
            }

        // Check if any of the source blocks are locked
        for (block in sourceBlocksToCheck) {
            val lockedBlock = lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(block))
            if (lockedBlock != null) {
                event.isCancelled = true
                return
            }
        }

        // Check destination inventory (where items are being put into)
        val destinationInventoryHolder = event.destination.holder
        val destinationBlocksToCheck =
            when (destinationInventoryHolder) {
                is BlockInventoryHolder -> listOf(destinationInventoryHolder.block)
                is DoubleChest -> {
                    val left = destinationInventoryHolder.leftSide as? Chest
                    val right = destinationInventoryHolder.rightSide as? Chest
                    listOfNotNull(left?.block, right?.block)
                }
                else -> emptyList()
            }

        // Check if any of the destination blocks are locked
        for (block in destinationBlocksToCheck) {
            val lockedBlock = lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(block))
            if (lockedBlock != null) {
                event.isCancelled = true
                return
            }
        }
    }
}
