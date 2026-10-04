package com.dansplugins.factionsystem.listener

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFertilizeEvent

/**
 * Protects claims from bone meal applied by a player (#2002). Bone meal from a dispenser has no
 * player and is left alone, as dispensers are elsewhere.
 */
class BlockFertilizeListener(private val protection: WorldActionProtection) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onBlockFertilize(event: BlockFertilizeEvent) {
        val player = event.player ?: return
        protection.protect(event, player, event.block, Material.BONE_MEAL)
    }
}
