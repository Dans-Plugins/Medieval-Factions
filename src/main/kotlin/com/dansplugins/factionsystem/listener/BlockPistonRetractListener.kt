package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPistonRetractEvent

class BlockPistonRetractListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onBlockPistonRetract(event: BlockPistonRetractEvent) {
        val gateService = plugin.services.gateService
        val blockPositions = event.blocks.map(MfBlockPosition::fromBukkitBlock)
        val gates = (blockPositions.flatMap { gateService.getGatesAt(it) } + blockPositions.flatMap { gateService.getGatesByTrigger(it) }).toSet()
        if (gates.isNotEmpty()) {
            event.isCancelled = true
        }
    }
}
