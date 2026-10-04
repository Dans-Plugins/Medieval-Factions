package com.dansplugins.factionsystem.listener

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FIREBALL
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL

/**
 * Protects claims from fire set by a player with flint and steel or a fire charge (#2002). Fire that
 * spreads, or is started by lava or lightning, has no player and is left to the game rules.
 */
class BlockIgniteListener(private val protection: WorldActionProtection) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        val player = event.player ?: return
        val item = when (event.cause) {
            FLINT_AND_STEEL -> Material.FLINT_AND_STEEL
            FIREBALL -> Material.FIRE_CHARGE
            else -> return
        }
        protection.protect(event, player, event.block, item)
    }
}
