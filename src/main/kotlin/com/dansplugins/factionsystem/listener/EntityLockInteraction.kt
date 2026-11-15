package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.interaction.MfInteractionStatus
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.ADDING_ACCESSOR
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.CHECKING_ACCESS
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.LOCKING
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.REMOVING_ACCESSOR
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.UNLOCKING
import com.dansplugins.factionsystem.locks.MfLockedEntity
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

/**
 * What right-clicking an armour stand or item frame does while the player is in one of the lock
 * modes set by `/lock`, `/unlock` and `/accessors` (#1679): the entity counterpart of the block
 * handling in [PlayerInteractListener].
 */
class EntityLockInteraction(private val plugin: MedievalFactions) {

    fun handle(status: MfInteractionStatus, player: Player, entity: Entity) {
        when (status) {
            LOCKING -> lock(player, entity)
            UNLOCKING -> unlock(player, entity)
            CHECKING_ACCESS -> runThenLeaveMode(player, "accessors list entity ${entity.uniqueId}")
            ADDING_ACCESSOR -> runThenLeaveMode(player, "accessors add entity ${entity.uniqueId}")
            REMOVING_ACCESSOR -> runThenLeaveMode(player, "accessors remove entity ${entity.uniqueId}")
            else -> {}
        }
    }

    private fun lock(player: Player, entity: Entity) {
        // Entity and world state is read here, on the main thread, before going async.
        val entityId = entity.uniqueId
        val claim = plugin.services.claimService.getClaim(entity.location.chunk)
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val mfPlayer = getOrSavePlayer(player) ?: return@Runnable
                val playerFaction = plugin.services.factionService.getFaction(mfPlayer.id)
                if (playerFaction == null) {
                    player.sendMessage("$RED${plugin.language["EntityLockNoFaction"]}")
                    return@Runnable
                }
                if (claim == null || claim.factionId != playerFaction.id) {
                    player.sendMessage("$RED${plugin.language["EntityLockNotInFactionTerritory"]}")
                    return@Runnable
                }
                val lockService = plugin.services.lockService
                val existingLock = lockService.getLockedEntity(entityId)
                if (existingLock != null) {
                    player.sendMessage("$RED${plugin.language["EntityLockAlreadyLocked", ownerName(existingLock)]}")
                    return@Runnable
                }
                lockService.lockEntity(entityId, claim, mfPlayer).onFailure {
                    player.sendMessage("$RED${plugin.language["EntityLockFailedToSaveLockedEntity"]}")
                    plugin.logger.log(SEVERE, "Failed to save locked entity: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                player.sendMessage("$GREEN${plugin.language["EntityLockSuccessful"]}")
            }
        )
    }

    private fun unlock(player: Player, entity: Entity) {
        val entityId = entity.uniqueId
        val canForceUnlock = player.hasPermission("mf.force.unlock")
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val mfPlayer = getOrSavePlayer(player) ?: return@Runnable
                val lockService = plugin.services.lockService
                val lockedEntity = lockService.getLockedEntity(entityId)
                if (lockedEntity == null) {
                    player.sendMessage("$RED${plugin.language["EntityUnlockNotLocked"]}")
                    return@Runnable
                }
                if (lockedEntity.playerId.value != mfPlayer.id.value) {
                    if (!canForceUnlock && !hasLockBypassPermission(plugin, mfPlayer.id)) {
                        player.sendMessage("$RED${plugin.language["EntityUnlockOwnedByOtherPlayer"]}")
                        return@Runnable
                    }
                    player.sendMessage("$RED${plugin.language["BlockUnlockProtectionBypassed", ownerName(lockedEntity)]}")
                }
                lockService.deleteLockedEntity(entityId).onFailure {
                    player.sendMessage("$RED${plugin.language["EntityUnlockFailedToDeleteLockedEntity"]}")
                    plugin.logger.log(SEVERE, "Failed to delete locked entity: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                player.sendMessage("$GREEN${plugin.language["EntityUnlockSuccessful"]}")
            }
        )
    }

    /** Runs an `/accessors` form for the entity and leaves the one-shot mode, as a block click does. */
    private fun runThenLeaveMode(player: Player, command: String) {
        player.performCommand(command)
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val mfPlayer = getOrSavePlayer(player) ?: return@Runnable
                plugin.services.interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                    plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            }
        )
    }

    private fun getOrSavePlayer(player: Player): MfPlayer? {
        val playerService = plugin.services.playerService
        return playerService.getPlayer(player) ?: playerService.save(MfPlayer(plugin, player)).onFailure {
            player.sendMessage("$RED${plugin.language["BlockLockFailedToSavePlayer"]}")
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return null
        }
    }

    private fun ownerName(lockedEntity: MfLockedEntity): String =
        plugin.services.playerService.getPlayer(lockedEntity.playerId)?.toBukkit()?.name ?: plugin.language["UnknownPlayer"]

    companion object {
        /** The statuses in which a click on a lockable entity is a lock-mode action, not a use. */
        val LOCK_MODES = setOf(LOCKING, UNLOCKING, CHECKING_ACCESS, ADDING_ACCESSOR, REMOVING_ACCESSOR)

        /** Whether the player's faction role has the BYPASS_LOCKS permission, as for block locks. */
        internal fun hasLockBypassPermission(plugin: MedievalFactions, playerId: MfPlayerId): Boolean {
            val faction = plugin.services.factionService.getFaction(playerId) ?: return false
            val role = faction.getRole(playerId) ?: return false
            return role.hasPermission(faction, plugin.factionPermissions.bypassLocks)
        }
    }
}
