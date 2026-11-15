package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.locks.MfLockedBlock
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.inventory.Inventory

class InventoryMoveItemListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        // Stop hoppers from taking items from or putting items into locked blocks.
        val sourceLocks = locksOf(event.source)
        val destinationLocks = locksOf(event.destination)
        if (sourceLocks.isEmpty() && destinationLocks.isEmpty()) return
        if (isSameOwnerTransfer(sourceLocks, destinationLocks)) return
        event.isCancelled = true
    }

    // With factions.allowHopperTransfersBetweenSameOwnerLocks enabled, a transfer is allowed when both sides are
    // locked and every lock on either side belongs to the same player, e.g. a player's locked hopper pulling from
    // their own locked chest. An unlocked side never qualifies, so a locked chest still cannot be drained by an
    // unlocked hopper.
    private fun isSameOwnerTransfer(sourceLocks: List<MfLockedBlock>, destinationLocks: List<MfLockedBlock>): Boolean {
        if (!plugin.config.getBoolean("factions.allowHopperTransfersBetweenSameOwnerLocks")) return false
        if (sourceLocks.isEmpty() || destinationLocks.isEmpty()) return false
        return (sourceLocks + destinationLocks).map { it.playerId }.distinct().size == 1
    }

    private fun locksOf(inventory: Inventory): List<MfLockedBlock> {
        val lockService = plugin.services.lockService
        return blocksOf(inventory).mapNotNull { block ->
            lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(block))
        }
    }

    private fun blocksOf(inventory: Inventory): List<Block> = when (val holder = inventory.holder) {
        is BlockInventoryHolder -> listOf(holder.block)
        is DoubleChest -> {
            val left = holder.leftSide as? Chest
            val right = holder.rightSide as? Chest
            listOfNotNull(left?.block, right?.block)
        }
        else -> emptyList()
    }
}
