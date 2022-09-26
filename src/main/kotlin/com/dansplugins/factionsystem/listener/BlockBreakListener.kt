package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class BlockBreakListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val gateService = plugin.services.gateService
        val blockPosition = MfBlockPosition.fromBukkitBlock(event.block)
        val gates = gateService.getGatesAt(blockPosition) + gateService.getGatesByTrigger(blockPosition)
        if (gates.isNotEmpty()) {
            event.isCancelled = true
            event.player.sendMessage("${ChatColor.RED}${plugin.language["CannotBreakBlockInGate"]}")
        }
    }

}