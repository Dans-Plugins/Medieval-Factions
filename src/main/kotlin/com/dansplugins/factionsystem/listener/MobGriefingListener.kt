package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Mob
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityInteractEvent

/**
 * Applies the `allowMobGriefing` faction flag (#1614). When a faction sets it to false, mobs
 * cannot change blocks in its territory: endermen taking blocks, zombies breaking doors, ravagers
 * trampling crops, withers breaking blocks, sheep eating grass and mobs trampling farmland.
 * Players, falling blocks and other non-mob entities are not affected, and neither are explosions.
 */
class MobGriefingListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (isProtectedFromMob(event.entity, event.block)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityInteract(event: EntityInteractEvent) {
        if (event.block.type != Material.FARMLAND) return
        if (isProtectedFromMob(event.entity, event.block)) {
            event.isCancelled = true
        }
    }

    private fun isProtectedFromMob(entity: Entity, block: Block): Boolean {
        if (entity !is Mob) return false
        val claim = plugin.services.claimService.getClaim(block.chunk) ?: return false
        val faction = plugin.services.factionService.getFaction(claim.factionId) ?: return false
        return !faction.flags[plugin.flags.allowMobGriefing]
    }
}
