package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.locks.MfLockedEntity
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority.MONITOR
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import java.util.UUID
import java.util.logging.Level.SEVERE

/**
 * Keeps locked armour stands and item frames (#1679) from being used, emptied or broken by players
 * without access, mirroring block locks: players with access may use the entity (take an item out of
 * a frame, swap armour), but only the owner may break it, unless the breaker has `mf.force.unlock`.
 * Hits by a player's projectiles count as the player's. Damage from anything that is not a player
 * (mobs, explosions) is not refused here, as it is not for locked blocks.
 *
 * Right-clicks are handled by [EntityInteractionProtection]. When a locked entity is gone, its lock
 * is removed.
 */
class EntityLockListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        // Normally refused earlier, with the interact-at event; this covers anything that skips it.
        if (!mayUse(event.player, event.rightClicked)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        if (!MfLockedEntity.isLockable(entity)) return
        val player = playerBehind(event.damager) ?: return
        // Hitting an item frame takes its item out; hitting an armour stand breaks it.
        val allowed = if (entity is ArmorStand) mayBreak(player, entity) else mayUse(player, entity)
        if (!allowed) refuse(event, player, entity)
    }

    @EventHandler(ignoreCancelled = true)
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        val entity = event.entity
        if (!MfLockedEntity.isLockable(entity)) return
        val player = playerBehind(event.remover ?: return) ?: return
        if (!mayBreak(player, entity)) refuse(event, player, entity)
    }

    @EventHandler(priority = MONITOR, ignoreCancelled = true)
    fun onHangingBreak(event: HangingBreakEvent) = removeLock(event.entity)

    @EventHandler(priority = MONITOR)
    fun onEntityDeath(event: EntityDeathEvent) = removeLock(event.entity)

    private fun playerBehind(damager: Entity): Player? = when (damager) {
        is Player -> damager
        is Projectile -> damager.shooter as? Player
        else -> null
    }

    private fun mayUse(player: Player, entity: Entity): Boolean {
        val lockedEntity = plugin.services.lockService.getLockedEntity(entity.uniqueId) ?: return true
        val playerId = MfPlayerId(player.uniqueId.toString())
        if (lockedEntity.hasAccess(playerId)) return true
        val mfPlayer = plugin.services.playerService.getPlayer(player)
        return (mfPlayer != null && mfPlayer.isBypassEnabled && player.hasPermission("mf.bypass")) ||
            EntityLockInteraction.hasLockBypassPermission(plugin, playerId)
    }

    private fun mayBreak(player: Player, entity: Entity): Boolean {
        val lockedEntity = plugin.services.lockService.getLockedEntity(entity.uniqueId) ?: return true
        return lockedEntity.playerId.value == player.uniqueId.toString() || player.hasPermission("mf.force.unlock")
    }

    private fun refuse(event: Cancellable, player: Player, entity: Entity) {
        event.isCancelled = true
        val ownerId = plugin.services.lockService.getLockedEntity(entity.uniqueId)?.playerId ?: return
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val ownerName = plugin.services.playerService.getPlayer(ownerId)?.toBukkit()?.name ?: plugin.language["UnknownPlayer"]
                player.sendMessage("$RED${plugin.language["EntityLocked", ownerName]}")
            }
        )
    }

    private fun removeLock(entity: Entity) {
        if (!MfLockedEntity.isLockable(entity)) return
        val entityId: UUID = entity.uniqueId
        val lockService = plugin.services.lockService
        if (lockService.getLockedEntity(entityId) == null) return
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                lockService.deleteLockedEntity(entityId).onFailure {
                    plugin.logger.log(SEVERE, "Failed to delete lock on removed entity $entityId: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            }
        )
    }
}
