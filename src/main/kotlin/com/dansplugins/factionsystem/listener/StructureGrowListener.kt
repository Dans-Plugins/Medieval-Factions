package com.dansplugins.factionsystem.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.StructureGrowEvent

/**
 * Trims trees and huge mushrooms at claim borders (#2084), whether they grow naturally or from bone
 * meal. See [GrowthBorderTrimmer] for the rule.
 */
class StructureGrowListener(private val trimmer: GrowthBorderTrimmer) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        val origin = event.location
        val world = origin.world ?: return
        trimmer.trim(world.uid, origin.blockX, origin.blockZ, event.blocks)
    }
}
