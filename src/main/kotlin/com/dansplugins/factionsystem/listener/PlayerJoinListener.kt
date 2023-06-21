package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType.ACTION_BAR
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val claimService = plugin.services.claimService
                val newChunkClaim = claimService.getClaim(event.player.location.chunk)
                val factionService = plugin.services.factionService
                val newChunkFaction = newChunkClaim?.let { factionService.getFaction(it.factionId) }
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        val title = if (newChunkFaction != null) {
                            "${ChatColor.of(newChunkFaction.flags[plugin.flags.color])}${newChunkFaction.name}"
                        } else {
                            "${ChatColor.of(plugin.config.getString("wilderness.color"))}${plugin.language["Wilderness"]}"
                        }

                        val subtitle = if (newChunkFaction != null) {
                            "${ChatColor.of(newChunkFaction.flags[plugin.flags.color])}${newChunkFaction.description}"
                        } else {
                            "${ChatColor.of(plugin.config.getString("wilderness.color"))}${plugin.language["Wilderness"]}"
                        }

                        if (plugin.config.getBoolean("factions.titleTerritoryIndicator")) {
                            event.player.resetTitle()
                            event.player.sendTitle(title, subtitle, plugin.config.getInt("factions.titleTerritoryFadeInLength"),
                                plugin.config.getInt("factions.titleTerritoryDuration"),
                                plugin.config.getInt("factions.titleTerritoryFadeOutLength"))
                        }
                        if (plugin.config.getBoolean("factions.actionBarTerritoryIndicator")) {
                            event.player.spigot().sendMessage(ACTION_BAR, *TextComponent.fromLegacyText(title))
                        }
                    }
                )
            }
        )
    }
}
