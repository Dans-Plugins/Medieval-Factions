package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.player.MfPlayerId
import org.bukkit.ChatColor.RED
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent

class BlockPlaceListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val gateService = plugin.services.gateService
        val blockPosition = MfBlockPosition.fromBukkitBlock(event.block)
        val gates = gateService.getGatesAt(blockPosition)
        if (gates.isNotEmpty()) {
            event.isCancelled = true
            event.player.sendMessage("$RED${plugin.language["CannotPlaceBlockInGate"]}")
            return
        }

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(event.block.chunk) ?: return
        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return
        val playerId = MfPlayerId(event.player.uniqueId.toString())
        if (!claimService.isInteractionAllowed(playerId, claim)) {
            event.isCancelled = true
            event.player.sendMessage("$RED${plugin.language["CannotPlaceBlockInFactionTerritory", claimFaction.name]}")
        }
    }

}