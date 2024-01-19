package com.dansplugins.factionsystem.command.faction.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
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
import java.util.*
import java.util.logging.Level.SEVERE
import kotlin.math.floor

class MfFactionClaimFillCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols.getInstance(plugin.language.locale))
    private val claimFillMaxChunks = plugin.config.getInt("factions.claimFillMaxChunks", -1)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.claim.fill") && !sender.hasPermission("mf.claimfill")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.claim)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillNoFactionPermission"]}")
                    return@Runnable
                }
                val senderWorldId = sender.location.world?.uid
                val senderChunkX = sender.location.chunk.x
                val senderChunkZ = sender.location.chunk.z
                if (senderWorldId == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillMustBeInWorld"]}")
                    return@Runnable
                }
                plugin.server.scheduler.runTaskAsynchronously(
                    plugin,
                    Runnable saveChunks@{
                        val chunks: Set<MfChunkPosition>?
                        try {
                            chunks = fill(senderWorldId, senderChunkX, senderChunkZ, faction)
                        } catch (e: ClaimFillLimitReachedException) {
                            sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillTooManyChunks", claimFillMaxChunks.toString()]}")
                            return@saveChunks
                        }
                        if (chunks == null) {
                            sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillNotEnoughPower"]}")
                            return@saveChunks
                        }
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
                                return@filter (relationships + reverseRelationships).any { it.type == AT_WAR } &&
                                    claimFaction.power < claimService.getClaims(claimFactionId).size - claims.size
                            }
                            .flatMap { it.value.map { (chunk, _) -> chunk } }
                        val claimableChunks = unclaimedChunks + contestedChunks
                        if (claimableChunks.isEmpty()) {
                            sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillNoClaimableChunks"]}")
                            return@saveChunks
                        }
                        if (plugin.config.getBoolean("factions.limitLand") && claimableChunks.size + claimService.getClaims(faction.id).size > faction.power) {
                            sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillReachedDemesneLimit", decimalFormat.format(floor(faction.power))]}")
                            return@saveChunks
                        }
                        claimableChunks.forEach { chunk ->
                            claimService.save(MfClaimedChunk(chunk, faction.id))
                                .onFailure {
                                    sender.sendMessage("$RED${plugin.language["CommandFactionClaimFillFailedToSaveClaim"]}")
                                    plugin.logger.log(SEVERE, "Failed to save claimed chunk: ${it.reason.message}", it.reason.cause)
                                    return@saveChunks
                                }
                        }
                        sender.sendMessage("$GREEN${plugin.language["CommandFactionClaimFillSuccess", chunks.size.toString()]}")
                    }
                )
            }
        )
        return true
    }

    private fun fill(worldId: UUID, startChunkX: Int, startChunkZ: Int, faction: MfFaction, chunksToFill: Set<MfChunkPosition> = emptySet()): Set<MfChunkPosition>? {
        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(worldId, startChunkX, startChunkZ)
        if (claim?.factionId == faction.id) return chunksToFill
        if (chunksToFill.contains(MfChunkPosition(worldId, startChunkX, startChunkZ))) return chunksToFill
        val newChunks = mutableSetOf(*chunksToFill.toTypedArray(), MfChunkPosition(worldId, startChunkX, startChunkZ))
        if (newChunks.size + claimService.getClaims(faction.id).size > faction.power) return null
        if (claimFillMaxChunks > 0 && newChunks.size > claimFillMaxChunks) {
            throw ClaimFillLimitReachedException()
        }
        val westChunks = fill(worldId, startChunkX - 1, startChunkZ, faction, newChunks) ?: return null
        newChunks += westChunks
        val eastChunks = fill(worldId, startChunkX + 1, startChunkZ, faction, newChunks) ?: return null
        newChunks += eastChunks
        val northChunks = fill(worldId, startChunkX, startChunkZ - 1, faction, newChunks) ?: return null
        newChunks += northChunks
        val southChunks = fill(worldId, startChunkX, startChunkZ + 1, faction, newChunks) ?: return null
        newChunks += southChunks
        return newChunks
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()

    class ClaimFillLimitReachedException : Exception()
}
