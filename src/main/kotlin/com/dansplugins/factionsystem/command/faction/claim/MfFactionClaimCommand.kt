package com.dansplugins.factionsystem.command.faction.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.logging.Level.SEVERE
import kotlin.math.floor

class MfFactionClaimCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val factionClaimFillCommand = MfFactionClaimFillCommand(plugin)

    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols.getInstance(plugin.language.locale))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.claim")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionClaimNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionClaimNotAPlayer"]}")
            return true
        }
        if (args.isNotEmpty() && args[0].equals("fill", ignoreCase = true)) {
            return factionClaimFillCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionClaimFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            if (faction == null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionClaimMustBeInAFaction"]}")
                return@Runnable
            }
            val role = faction.getRole(mfPlayer.id)
            if (role == null || !role.hasPermission(faction, plugin.factionPermissions.claim)) {
                sender.sendMessage("$RED${plugin.language["CommandFactionClaimNoFactionPermission"]}")
                return@Runnable
            }
            val radius = if (args.isNotEmpty()) {
                args[0].toIntOrNull()
            } else {
                null
            }
            val maxClaimRadius = plugin.config.getInt("factions.maxClaimRadius")
            if (radius != null && (radius < 0 || radius > maxClaimRadius)) {
                sender.sendMessage("$RED${plugin.language["CommandFactionClaimMaxClaimRadius", maxClaimRadius.toString()]}")
                return@Runnable
            }
            val senderChunkX = sender.location.chunk.x
            val senderChunkZ = sender.location.chunk.z
            plugin.server.scheduler.runTask(plugin, Runnable {
                val chunks = if (radius == null) {
                    listOf(MfChunkPosition(sender.world.uid, senderChunkX, senderChunkZ))
                } else {
                    (senderChunkX - radius..senderChunkX + radius).flatMap { x ->
                        (senderChunkZ - radius..senderChunkZ + radius).filter { z ->
                            val a = x - senderChunkX
                            val b = z - senderChunkZ
                            (a * a) + (b * b) <= radius * radius
                        }.map { z -> MfChunkPosition.fromBukkit(sender.world.getChunkAt(x, z)) }
                    }
                }
                plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable saveChunks@{
                    val claimService = plugin.services.claimService
                    val claims = chunks.associateWith(claimService::getClaim)
                    val relationshipService = plugin.services.factionRelationshipService
                    val unclaimedChunks = claims.filter { (_, claim) -> claim == null }.keys
                    val contestedChunks = claims
                        .mapNotNull { (chunk, claim) -> claim?.let { chunk to it } }
                        .groupBy { (_, claim) -> claim.factionId }
                        .filter { (claimFactionId, claims) ->
                            val claimFaction = factionService.getFaction(claimFactionId) ?: return@filter true
                            val relationships = relationshipService.getRelationships(faction.id, claimFactionId)
                            val reverseRelationships = relationshipService.getRelationships(claimFactionId, faction.id)
                            return@filter (relationships + reverseRelationships).any { it.type == AT_WAR }
                                    && claimFaction.power < claimService.getClaims(claimFactionId).size - claims.size
                        }
                        .flatMap { it.value.map { (chunk, _) -> chunk } }
                    val claimableChunks = unclaimedChunks + contestedChunks
                    if (claimableChunks.isEmpty()) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionClaimNoClaimableChunks"]}")
                        return@saveChunks
                    }
                    if (plugin.config.getBoolean("factions.limitLand") && claimableChunks.size + claimService.getClaims(faction.id).size > faction.power) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionClaimReachedDemesneLimit", decimalFormat.format(floor(faction.power))]}")
                        return@saveChunks
                    }
                    // Checks if the attempted claim is connected to an already existing claim. Will make an exception if the faction has no claims.
                    if (plugin.config.getBoolean("factions.contiguousClaims") &&
                            !claimService.isClaimAdjacent(faction.id, *claimableChunks.toTypedArray()) &&
                            claimService.getClaims(faction.id).isNotEmpty()) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionClaimNotContiguous"]}")
                        return@saveChunks
                    }
                    claimableChunks.forEach { chunk ->
                        claimService.save(MfClaimedChunk(chunk, faction.id))
                            .onFailure {
                                sender.sendMessage("$RED${plugin.language["CommandFactionClaimFailedToSaveClaim"]}")
                                plugin.logger.log(SEVERE, "Failed to save claimed chunk: ${it.reason.message}", it.reason.cause)
                                return@saveChunks
                            }
                    }
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionClaimSuccess", chunks.size.toString()]}")
                })
            })
        })
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = when {
        args.isEmpty() -> listOf("fill")
        args.size == 1 && "fill".startsWith(args[0].lowercase()) -> listOf("fill")
        else -> emptyList()
    }
}