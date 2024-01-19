package com.dansplugins.factionsystem.listener
import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import java.util.logging.Level

class PlayerBucketListener(private val plugin: MedievalFactions) : Listener {
    fun handleProtection(event: PlayerBucketEvent) {
        val gateService = plugin.services.gateService
        val blockPosition = MfBlockPosition.fromBukkitBlock(event.block)
        val gates = gateService.getGatesAt(blockPosition)
        if (gates.isNotEmpty()) {
            event.isCancelled = true
            event.player.sendMessage("${ChatColor.RED}${plugin.language["CannotPlaceBlockInGate"]}")
            return
        }

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(event.block.chunk) ?: return
        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(event.player)
        if (mfPlayer == null) {
            event.isCancelled = true
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    playerService.save(MfPlayer(plugin, event.player)).onFailure {
                        event.player.sendMessage("${ChatColor.RED}${plugin.language["BlockPlaceFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
            return
        }

        if (!claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            if (mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) {
                event.player.sendMessage("${ChatColor.RED}${plugin.language["FactionTerritoryProtectionBypassed"]}")
            } else {
                event.isCancelled = true
                event.player.sendMessage("${ChatColor.RED}${plugin.language["CannotBreakBlockInFactionTerritory", claimFaction.name]}")
            }
        }
    }

    @EventHandler
    fun playerBuckeEmptytEvent(event: PlayerBucketEmptyEvent) {
        handleProtection(event)
    }

    @EventHandler
    fun playerBucketFillEvent(event: PlayerBucketFillEvent) {
        handleProtection(event)
    }
}
