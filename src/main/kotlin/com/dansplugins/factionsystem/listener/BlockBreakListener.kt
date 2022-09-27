package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.player.MfPlayerId
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

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(event.block.chunk) ?: return
        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return
        val playerId = MfPlayerId(event.player.uniqueId.toString())
        if (!claimService.isInteractionAllowed(playerId, claim)) {
            event.isCancelled = true
            event.player.sendMessage("${ChatColor.RED}${plugin.language["CannotBreakBlockInFactionTerritory", claimFaction.name]}")
        }
    }

}