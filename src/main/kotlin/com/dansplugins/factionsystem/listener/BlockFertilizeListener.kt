package com.dansplugins.factionsystem.listener

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFertilizeEvent

/**
 * Protects claims from bone meal applied by a player (#2002), and keeps what the bone meal grows
 * inside the territory it was applied in (#2084). Bone meal from a dispenser has no player, so only
 * the second rule applies to it.
 */
class BlockFertilizeListener(
    private val protection: WorldActionProtection,
    private val trimmer: GrowthBorderTrimmer
) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onBlockFertilize(event: BlockFertilizeEvent) {
        val player = event.player
        if (player != null && protection.protect(event, player, event.block, Material.BONE_MEAL)) return
        val block = event.block
        trimmer.trim(block.world.uid, block.x, block.z, event.blocks)
    }
}
