package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.*
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import java.util.logging.Level.SEVERE

class PlayerInteractListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val clickedBlock = event.clickedBlock ?: return
        val interactionService = plugin.services.interactionService
        // Looking up the player currently involves a blocking call to the database, but since UUIDs are used as
        // player IDs, and they're in memory anyway, we can just pass that to getInteractionStatus, which doesn't
        // involve any slow database calls.
        val interactionStatus = interactionService.getInteractionStatus(MfPlayerId(event.player.uniqueId.toString()))
        when (interactionStatus) {
            LOCKING -> {
                lock(event.player, clickedBlock)
                event.isCancelled = true
            }
            UNLOCKING -> {
                unlock(event.player, clickedBlock)
                event.isCancelled = true
            }
            CHECKING_ACCESS -> {
                checkAccess(event.player, clickedBlock)
                event.isCancelled = true
            }
            ADDING_ACCESSOR -> {
                addAccessor(event.player, clickedBlock)
                event.isCancelled = true
            }
            REMOVING_ACCESSOR -> {
                removeAccessor(event.player, clickedBlock)
                event.isCancelled = true
            }
            null -> {
                val lockService = plugin.services.lockService
                val lockedBlock = lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(clickedBlock))
                if (lockedBlock != null) {
                    if (event.player.uniqueId.toString() !in (lockedBlock.accessors + lockedBlock.playerId).map(MfPlayerId::value)) {
                        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                            val playerService = plugin.services.playerService
                            val owner = playerService.getPlayer(lockedBlock.playerId)
                            event.player.sendMessage("$RED${plugin.language["BlockLocked", owner?.toBukkit()?.name ?: "unknown player"]}")
                        })
                        event.isCancelled = true
                    }
                }
            }
        }

    }

    private fun lock(player: Player, block: Block) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer.fromBukkit(player)).onFailure {
                player.sendMessage("$RED${plugin.language["BlockLockFailedToSavePlayer"]}")
                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            val factionService = plugin.services.factionService
            val playerFaction = factionService.getFaction(mfPlayer.id)
            if (playerFaction == null) {
                player.sendMessage("$RED${plugin.language["BlockLockNoFaction"]}")
                return@Runnable
            }
            val claimService = plugin.services.claimService
            val claim = claimService.getClaim(block.chunk)
            if (claim == null || claim.factionId != playerFaction.id) {
                player.sendMessage("$RED${plugin.language["BlockLockNotInFactionTerritory"]}")
                return@Runnable
            }
            val lockService = plugin.services.lockService
            val blockPosition = MfBlockPosition.fromBukkitBlock(block)
            val existingLock = lockService.getLockedBlock(blockPosition)
            if (existingLock != null) {
                val existingLockOwner = playerService.getPlayer(existingLock.playerId)
                player.sendMessage("$RED${plugin.language["BlockLockAlreadyLocked", existingLockOwner?.toBukkit()?.name ?: "unknown player"]}")
                return@Runnable
            }
            lockService.lock(blockPosition, claim, mfPlayer).onFailure {
                player.sendMessage("$RED${plugin.language["BlockLockFailedToSaveLockedBlock"]}")
                plugin.logger.log(SEVERE, "Failed to save locked block: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            val interactionService = plugin.services.interactionService
            interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                player.sendMessage("$RED${plugin.language["BlockLockFailedToSaveInteractionStatus"]}")
                plugin.logger.log(SEVERE, "Failed to save interaction status: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            player.sendMessage("$GREEN${plugin.language["BlockLockSuccessful"]}")
        })
    }

    private fun unlock(player: Player, block: Block) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer.fromBukkit(player)).onFailure {
                player.sendMessage("$RED${plugin.language["BlockUnlockFailedToSavePlayer"]}")
                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            val lockService = plugin.services.lockService
            val lockedBlock = lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(block))
            if (lockedBlock == null) {
                player.sendMessage("$RED${plugin.language["BlockUnlockNotLocked"]}")
                return@Runnable
            }
            if (lockedBlock.playerId.value != mfPlayer.id.value) {
                val lockOwner = playerService.getPlayer(lockedBlock.playerId)
                val ownerName = if (lockOwner == null) {
                    "unknown player"
                } else {
                    lockOwner.toBukkit().name ?: "unknown player"
                }
                player.sendMessage("$RED${plugin.language["BlockUnlockOwnedByOtherPlayer", ownerName]}")
                return@Runnable
            }
            lockService.delete(lockedBlock.block).onFailure {
                player.sendMessage("$RED${plugin.language["BlockUnlockFailedToDeleteBlock"]}")
                plugin.logger.log(SEVERE, "Failed to delete block: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            val interactionService = plugin.services.interactionService
            interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                player.sendMessage("$RED${plugin.language["BlockUnlockFailedToSaveInteractionStatus"]}")
                plugin.logger.log(SEVERE, "Failed to save interaction status: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            player.sendMessage("$RED${plugin.language["BlockUnlockSuccessful"]}")
        })
    }

    private fun checkAccess(player: Player, block: Block) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer.fromBukkit(player)).onFailure {
                player.sendMessage("$RED${plugin.language["BlockCheckAccessFailedToSavePlayer"]}")
                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            val lockService = plugin.services.lockService
            val lockedBlock = lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(block))
            if (lockedBlock == null) {
                player.sendMessage("$RED${plugin.language["BlockCheckAccessNotLocked"]}")
                return@Runnable
            }
            plugin.server.scheduler.runTask(plugin, Runnable {
                player.performCommand("accessors list ${block.x} ${block.y} ${block.z}")
            })
            val interactionService = plugin.services.interactionService
            interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                player.sendMessage("$RED${plugin.language["BlockCheckAccessFailedToSetInteractionStatus"]}")
                plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
        })
    }

    private fun addAccessor(player: Player, block: Block) {
        player.performCommand("accessors add ${block.x} ${block.y} ${block.z}")
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer.fromBukkit(player)).onFailure {
                player.sendMessage("$RED${plugin.language["BlockAddAccessorFailedToSavePlayer"]}")
                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            val interactionService = plugin.services.interactionService
            interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                player.sendMessage("$RED${plugin.language["BlockAddAccessorFailedToSetInteractionStatus"]}")
                plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
        })
    }

    private fun removeAccessor(player: Player, block: Block) {
        player.performCommand("accessors remove ${block.x} ${block.y} ${block.z}")
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer.fromBukkit(player)).onFailure {
                player.sendMessage("$RED${plugin.language["BlockRemoveAccessorFailedToSavePlayer"]}")
                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            val interactionService = plugin.services.interactionService
            interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                player.sendMessage("$RED${plugin.language["BlockRemoveAccessorFailedToSetInteractionStatus"]}")
                plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
        })
    }

}