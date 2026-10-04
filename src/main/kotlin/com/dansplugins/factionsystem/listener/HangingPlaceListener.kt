package com.dansplugins.factionsystem.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.hanging.HangingPlaceEvent

/**
 * Protects claims from item frames and paintings hung by a player (#2002). The claim checked is the
 * one holding the block the entity hangs on.
 */
class HangingPlaceListener(private val protection: WorldActionProtection) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) {
        val player = event.player ?: return
        protection.protect(event, player, event.block, event.itemStack?.type)
    }
}
