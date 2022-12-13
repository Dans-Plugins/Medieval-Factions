package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPistonExtendEvent

class BlockPistonExtendListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onBlockPistonExtend(event: BlockPistonExtendEvent) {
        val gateService = plugin.services.gateService
        val blockPositions = event.blocks.map(MfBlockPosition::fromBukkitBlock)
        val gates = (blockPositions.flatMap { gateService.getGatesAt(it) } + blockPositions.flatMap { gateService.getGatesByTrigger(it) }).toSet()
        if (gates.isNotEmpty()) {
            event.isCancelled = true
        }
    }
}
