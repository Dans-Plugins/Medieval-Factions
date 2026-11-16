package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.RED
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.PortalCreateEvent

/**
 * With `factions.preventPortalGenerationInClaims` enabled, stops the game from generating the exit portal of a nether
 * portal trip ([PortalCreateEvent.CreateReason.NETHER_PAIR]) inside a faction's territory (#1585), unless the
 * travelling player could build there anyway, or is in bypass mode. Portals that players light themselves are not
 * covered by this option.
 */
class PortalCreateListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPortalCreate(event: PortalCreateEvent) {
        if (event.reason != PortalCreateEvent.CreateReason.NETHER_PAIR) return
        if (!plugin.config.getBoolean("factions.preventPortalGenerationInClaims")) return
        val worldId = event.world.uid
        val claimService = plugin.services.claimService
        val claims = event.blocks
            .map { it.x shr 4 to (it.z shr 4) }
            .distinct()
            .mapNotNull { (x, z) -> claimService.getClaim(worldId, x, z) }
        if (claims.isEmpty()) return
        val player = event.entity as? Player
        val mfPlayer = player?.let { plugin.services.playerService.getPlayer(it) }
        if (mfPlayer != null) {
            if (claims.all { claimService.isInteractionAllowed(mfPlayer.id, it) }) return
            if (mfPlayer.isBypassEnabled && player?.hasPermission("mf.bypass") == true) return
        }
        event.isCancelled = true
        if (player != null) {
            val factionName = plugin.services.factionService.getFaction(claims.first().factionId)?.name
                ?: plugin.language["UnknownFaction"]
            player.sendMessage("$RED${plugin.language["CannotGeneratePortalInFactionTerritory", factionName]}")
        }
    }
}
