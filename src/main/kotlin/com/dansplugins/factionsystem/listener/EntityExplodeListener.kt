package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.utils.MfServerVersion
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent

class EntityExplodeListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        val gateService = plugin.services.gateService
        val gateBlocks = event.blockList().filter { block ->
            gateService.getGatesAt(MfBlockPosition.fromBukkitBlock(block)).isNotEmpty() ||
                gateService.getGatesByTrigger(MfBlockPosition.fromBukkitBlock(block)).isNotEmpty()
        }
        event.blockList().removeAll(gateBlocks)

        if (MfServerVersion.isAtLeast(1, 21)) {
            applyWindChargeProtection(event)
        }
    }

    private fun applyWindChargeProtection(event: EntityExplodeEvent) {
        val entity = event.entity
        if (entity.type.name != "WIND_CHARGE") return
        val projectile = entity as? Projectile ?: return
        val shooter = projectile.shooter as? Player ?: return
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(shooter) ?: return
        if (mfPlayer.isBypassEnabled && shooter.hasPermission("mf.bypass")) return
        val claimService = plugin.services.claimService
        val protectedBlocks = event.blockList().filter { block ->
            val claim = claimService.getClaim(block.chunk) ?: return@filter false
            !claimService.isInteractionAllowed(mfPlayer.id, claim)
        }
        event.blockList().removeAll(protectedBlocks)
    }
}
