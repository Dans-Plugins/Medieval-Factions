package com.dansplugins.factionsystem.command.faction.unclaim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.UNCLAIM
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionUnclaimCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.unclaim")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer.fromBukkit(sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            if (faction == null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimMustBeInAFaction"]}")
                return@Runnable
            }
            val role = faction.getRole(mfPlayer.id)
            if (role == null || !role.hasPermission(faction, UNCLAIM)) {
                sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNoFactionPermission"]}")
                return@Runnable
            }
            val radius = if (args.isNotEmpty()) {
                args[0].toIntOrNull()
            } else {
                null
            }
            val maxClaimRadius = plugin.config.getInt("factions.maxClaimRadius")
            if (radius != null && (radius < 0 || radius > maxClaimRadius)) {
                sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimMaxClaimRadius", maxClaimRadius.toString()]}")
                return@Runnable
            }
            val claimService = plugin.services.claimService
            val senderChunkX = sender.location.chunk.x
            val senderChunkZ = sender.location.chunk.z
            plugin.server.scheduler.runTask(plugin, Runnable {
                val chunks = if (radius == null) {
                    listOf(sender.location.chunk)
                } else {
                    (senderChunkX - radius..senderChunkX + radius).flatMap { x ->
                        (senderChunkZ - radius..senderChunkZ + radius).filter { z ->
                            val a = x - senderChunkX
                            val b = z - senderChunkZ
                            (a * a) + (b * b) <= radius * radius
                        }.map { z -> sender.world.getChunkAt(x, z) }
                    }
                }.filter { chunk ->
                    val claim = claimService.getClaim(chunk)
                    return@filter claim != null && claim.factionId.value == faction.id.value
                }
                plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable saveChunks@{
                    if (chunks.isEmpty()) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNoUnclaimableChunks"]}")
                        return@saveChunks
                    }
                    chunks.forEach { chunk ->
                        claimService.delete(chunk.world, chunk.x, chunk.z)
                            .onFailure {
                                sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimFailedToDeleteClaim"]}")
                                plugin.logger.log(SEVERE, "Failed to delete claimed chunk: ${it.reason.message}", it.reason.cause)
                                return@saveChunks
                            }
                    }
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionUnclaimSuccess", chunks.size.toString()]}")
                })
            })
        })
        return true
    }
}