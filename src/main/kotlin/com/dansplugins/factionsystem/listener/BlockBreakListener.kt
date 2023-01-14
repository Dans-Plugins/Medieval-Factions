package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.GREEN
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
        }

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(event.block.chunk) ?: return
        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(event.player)
        if (mfPlayer == null) {
            event.isCancelled = true
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                playerService.save(MfPlayer(plugin, event.player)).onFailure {
                    event.player.sendMessage("$RED${plugin.language["BlockBreakFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            })
            return
        }
        if (!claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            if (mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) {
                event.player.sendMessage("$RED${plugin.language["FactionTerritoryProtectionBypassed"]}")
            } else {
                event.isCancelled = true
                event.player.sendMessage("$RED${plugin.language["CannotBreakBlockInFactionTerritory", claimFaction.name]}")
            }
        }

        // if block locked and breaker is owner, unlock
        val lockService = plugin.services.lockService
        val lockedBlock = lockService.getLockedBlock(blockPosition) ?: return
        if (lockedBlock.playerId == mfPlayer.id) {
            if (lockedBlock.playerId.value != mfPlayer.id.value) {
                val lockOwner = playerService.getPlayer(lockedBlock.playerId)
                val ownerName = if (lockOwner == null) {
                    plugin.language["UnknownPlayer"]
                } else {
                    lockOwner.toBukkit().name ?: plugin.language["UnknownPlayer"]
                }
                if (!event.player.hasPermission("mf.force.unlock")) {
                    event.player.sendMessage("$RED${plugin.language["BlockUnlockOwnedByOtherPlayer", ownerName]}")
                    return
                } else {
                    event.player.sendMessage("$RED${plugin.language["BlockUnlockProtectionBypassed", ownerName]}")
                }
            }
            lockService.unlock(event.block)
            val result = lockService.unlock(event.block)
            when (result) {
                0 -> {
                    event.player.sendMessage("$GREEN${plugin.language["BlockUnlockSuccessful"]}")
                }

                1 -> {
                    event.player.sendMessage("$RED${plugin.language["BlockNotLocked"]}")
                }

                2 -> {
                    event.player.sendMessage("$RED${plugin.language["BlockUnlockFailedToSaveLockedBlock"]}")
                }
            }
        } else {
            event.isCancelled = true
            val owner = playerService.getPlayer(lockedBlock.playerId)
            event.player.sendMessage("$RED${plugin.language["BlockLocked", owner?.toBukkit()?.name ?: plugin.language["UnknownPlayer"]]}")
        }
    }

}