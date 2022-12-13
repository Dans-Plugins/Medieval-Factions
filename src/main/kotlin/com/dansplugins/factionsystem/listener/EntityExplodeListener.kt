package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent

class EntityExplodeListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        val gateService = plugin.services.gateService
        val blocks = event.blockList().filter { block ->
            gateService.getGatesAt(MfBlockPosition.fromBukkitBlock(block)).isNotEmpty() ||
                gateService.getGatesByTrigger(MfBlockPosition.fromBukkitBlock(block)).isNotEmpty()
        }
        event.blockList().removeAll(blocks)
    }
}
