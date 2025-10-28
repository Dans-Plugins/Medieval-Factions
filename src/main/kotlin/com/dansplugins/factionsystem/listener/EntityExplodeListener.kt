package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import org.bukkit.entity.Player
import org.bukkit.entity.WindCharge
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent

class EntityExplodeListener(
    private val plugin: MedievalFactions,
) : Listener {
    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        val gateService = plugin.services.gateService
        val blocks =
            event.blockList().filter { block ->
                gateService.getGatesAt(MfBlockPosition.fromBukkitBlock(block)).isNotEmpty() ||
                    gateService.getGatesByTrigger(MfBlockPosition.fromBukkitBlock(block)).isNotEmpty()
            }
        event.blockList().removeAll(blocks)

        if (event.entity is WindCharge) {
            val windCharge = event.entity as WindCharge
            val shooter = windCharge.shooter

            if (shooter is Player) {
                val claimService = plugin.services.claimService
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(shooter)

                if (mfPlayer != null) {
                    val protectedBlocks =
                        event.blockList().filter { block ->
                            val claim = claimService.getClaim(block.chunk)
                            if (claim != null) {
                                val hasAccess = claimService.isInteractionAllowed(mfPlayer.id, claim)
                                val hasBypass = mfPlayer.isBypassEnabled && shooter.hasPermission("mf.bypass")
                                !hasAccess && !hasBypass
                            } else {
                                false
                            }
                        }
                    event.blockList().removeAll(protectedBlocks)
                }
            }
        }
    }
}
