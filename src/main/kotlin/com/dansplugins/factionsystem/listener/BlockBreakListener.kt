package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.locks.MfUnlockResult.FAILURE
import com.dansplugins.factionsystem.locks.MfUnlockResult.SUCCESS
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import java.util.logging.Level.SEVERE

class BlockBreakListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val gateService = plugin.services.gateService
        val blockPosition = MfBlockPosition.fromBukkitBlock(event.block)
        val gates = gateService.getGatesAt(blockPosition) + gateService.getGatesByTrigger(blockPosition)
        if (gates.isNotEmpty()) {
            event.isCancelled = true
            event.player.sendMessage("$RED${plugin.language["CannotBreakBlockInGate"]}")
            return
        }

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(event.block.chunk)
        if (claim == null) {
            if (plugin.config.getBoolean("wilderness.break.prevent", false)) {
                event.isCancelled = true
                if (plugin.config.getBoolean("wilderness.break.alert", true)) {
                    event.player.sendMessage("$RED${plugin.language["CannotBreakBlockInWilderness"]}")
                }
            }
            return
        }
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
                        event.player.sendMessage("$RED${plugin.language["BlockBreakFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
            return
        }
        if (!claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            if (mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) {
                event.player.sendMessage("$RED${plugin.language["FactionTerritoryProtectionBypassed"]}")
            } else if (!claimService.isWartimeBreakableBlock(mfPlayer.id, claim, event.block.type)) {
                event.isCancelled = true
                event.player.sendMessage("$RED${plugin.language["CannotBreakBlockInFactionTerritory", claimFaction.name]}")
                return
            }
        }

        // A locked area (#1393) narrows who may break blocks inside it, on top of the claim.
        if (claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            val lockedArea = plugin.services.lockedAreaService.getRefusingArea(mfPlayer, event.player, claim, blockPosition)
            if (lockedArea != null) {
                event.isCancelled = true
                event.player.sendMessage("$RED${plugin.language["CannotBreakBlockInLockedArea", lockedArea.name]}")
                return
            }
        }

        // if block locked and breaker is owner, unlock
        val lockService = plugin.services.lockService
        val lockedBlock = lockService.getLockedBlock(blockPosition) ?: return
        if (lockedBlock.playerId != mfPlayer.id) {
            val lockOwner = playerService.getPlayer(lockedBlock.playerId)
            val ownerName = if (lockOwner == null) {
                plugin.language["UnknownPlayer"]
            } else {
                lockOwner.toBukkit().name ?: plugin.language["UnknownPlayer"]
            }
            if (!event.player.hasPermission("mf.force.unlock")) {
                event.player.sendMessage("$RED${plugin.language["BlockUnlockOwnedByOtherPlayer", ownerName]}")
                event.isCancelled = true
                return
            }
        }
        lockService.unlock(event.block) { result ->
            when (result) {
                SUCCESS -> event.player.sendMessage("$GREEN${plugin.language["BlockUnlockSuccessful"]}")
                FAILURE -> event.player.sendMessage("$RED${plugin.language["BlockUnlockFailedToSaveLockedBlock"]}")
                else -> {}
            }
        }
    }
}
