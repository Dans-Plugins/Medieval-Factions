package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import net.md_5.bungee.api.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val claimService = plugin.services.claimService
            val newChunkClaim = claimService.getClaim(event.player.location.chunk)
            val factionService = plugin.services.factionService
            val newChunkFaction = newChunkClaim?.let { factionService.getFaction(it.factionId) }
            plugin.server.scheduler.runTask(plugin, Runnable {
                event.player.resetTitle()
                val title = if (newChunkFaction != null) {
                    "${ChatColor.of(newChunkFaction.flags[plugin.flags.territoryAlertColor])}${newChunkFaction.name}"
                } else {
                    "${ChatColor.of(plugin.config.getString("wilderness.territoryAlertColor"))}${plugin.language["Wilderness"]}"
                }
                event.player.sendTitle(title, null, 10, 70, 20)
            })
        })
    }

}