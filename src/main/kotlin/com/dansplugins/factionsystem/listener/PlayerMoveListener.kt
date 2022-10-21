package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType.ACTION_BAR
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor.RED
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.util.logging.Level.SEVERE

class PlayerMoveListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to ?: return
        if (from.chunk == to.chunk) return
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val claimService = plugin.services.claimService
            val newChunkClaim = claimService.getClaim(to.chunk)
            val oldChunkClaim = claimService.getClaim(from.chunk)
            if (newChunkClaim?.factionId?.value == oldChunkClaim?.factionId?.value) return@Runnable
            val factionService = plugin.services.factionService
            val newChunkFaction = newChunkClaim?.let { factionService.getFaction(it.factionId) }
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(event.player)
                ?: playerService.save(MfPlayer(plugin, event.player)).onFailure {
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val playerFaction = factionService.getFaction(mfPlayer.id)
            if (playerFaction != null) {
                plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable claimLand@{
                    if (newChunkFaction == null && playerFaction.autoclaim) {
                        if (plugin.config.getBoolean("factions.limitLand") && claimService.getClaims(playerFaction.id).size + 1 > playerFaction.power) {
                            event.player.sendMessage("$RED${plugin.language["AutoclaimPowerLimitReached"]}")
                            val updatedFaction = factionService.save(playerFaction.copy(autoclaim = false)).onFailure {
                                plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                                return@claimLand
                            }
                            updatedFaction.sendMessage(
                                plugin.language["AutoclaimDisabledNotificationTitle"],
                                plugin.language["AutoclaimDisabledNotificationBody"]
                            )
                            return@claimLand
                        }
                        claimService.save(MfClaimedChunk(to.chunk, playerFaction.id)).onFailure {
                            plugin.logger.log(SEVERE, "Failed to save chunk claim: ${it.reason.message}", it.reason.cause)
                            return@claimLand
                        }
                    }
                })
            }
            plugin.server.scheduler.runTask(plugin, Runnable {
                event.player.resetTitle()
                val title = if (newChunkFaction != null) {
                    "${ChatColor.of(newChunkFaction.flags[plugin.flags.color])}${newChunkFaction.name}"
                } else {
                    "${ChatColor.of(plugin.config.getString("wilderness.color"))}${plugin.language["Wilderness"]}"
                }
                event.player.sendTitle(title, null, 10, 70, 20)
                if (plugin.config.getBoolean("factions.actionBarTerritoryIndicator")) {
                    event.player.spigot().sendMessage(ACTION_BAR, *TextComponent.fromLegacyText(title))
                }
            })
        })
    }

}