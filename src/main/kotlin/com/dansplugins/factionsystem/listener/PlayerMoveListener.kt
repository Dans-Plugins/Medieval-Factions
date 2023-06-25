package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
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
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val claimService = plugin.services.claimService
                val newChunkClaim = claimService.getClaim(to.chunk)
                val oldChunkClaim = claimService.getClaim(from.chunk)
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
                    if (newChunkFaction == null && playerFaction.autoclaim) {
                        val playerRole = playerFaction.getRole(mfPlayer.id) ?: return@Runnable
                        val claimPermissionValue = playerRole.getPermissionValue(plugin.factionPermissions.claim) ?: return@Runnable
                        if (!claimPermissionValue || !event.player.hasPermission("mf.claim")) {
                            return@Runnable
                        }
                        if (plugin.config.getBoolean("factions.limitLand") && claimService.getClaims(playerFaction.id).size + 1 > playerFaction.power) {
                            event.player.sendMessage("$RED${plugin.language["AutoclaimPowerLimitReached"]}")
                            val updatedFaction = factionService.save(playerFaction.copy(autoclaim = false)).onFailure {
                                plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                                return@Runnable
                            }
                            updatedFaction.sendMessage(
                                plugin.language["AutoclaimDisabledNotificationTitle"],
                                plugin.language["AutoclaimDisabledNotificationBody"]
                            )
                            return@Runnable
                        }
                        if (plugin.config.getBoolean("factions.contiguousClaims") &&
                            !claimService.isClaimAdjacent(playerFaction.id, *listOfNotNull(to.world?.let { MfChunkPosition(it.uid, to.chunk.x, to.chunk.z) }).toTypedArray()) &&
                            claimService.getClaims(playerFaction.id).isNotEmpty()
                        ) {
                            event.player.sendMessage("$RED${plugin.language["CommandFactionClaimNotContiguous"]}")
                            val updatedFaction = factionService.save(playerFaction.copy(autoclaim = false)).onFailure {
                                plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                                return@Runnable
                            }
                            updatedFaction.sendMessage(
                                plugin.language["AutoclaimDisabledNotificationTitle"],
                                plugin.language["AutoclaimDisabledNotificationBody"]
                            )
                            return@Runnable
                        }
                        claimService.save(MfClaimedChunk(to.chunk, playerFaction.id)).onFailure {
                            plugin.logger.log(SEVERE, "Failed to save chunk claim: ${it.reason.message}", it.reason.cause)
                            return@Runnable
                        }
                    }
                }
                if (newChunkClaim?.factionId?.value == oldChunkClaim?.factionId?.value) return@Runnable
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
                            null
                        }
                        if (plugin.config.getBoolean("factions.titleTerritoryIndicator")) {
                            event.player.resetTitle()
                            event.player.sendTitle(
                                title,
                                subtitle,
                                plugin.config.getInt("factions.titleTerritoryFadeInLength"),
                                plugin.config.getInt("factions.titleTerritoryDuration"),
                                plugin.config.getInt("factions.titleTerritoryFadeOutLength")
                            )
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
