package com.dansplugins.factionsystem.command.faction.unclaim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.command.unquote
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionUnclaimCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.unclaim")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNotAPlayer"]}")
            return true
        }
        val senderChunkPosition = MfChunkPosition.fromBukkit(sender.location.chunk)
        // With mf.force.unclaim, a faction name or ID may be given before the radius to unclaim that faction's land
        // (/f unclaim <faction> [radius]). A lone integer is always read as a radius, so existing usage is unchanged.
        val hasForcePermission = sender.hasPermission("mf.force.unclaim")
        val unquotedArgs = args.unquote()
        val targetFactionName = if (hasForcePermission && unquotedArgs.isNotEmpty() && unquotedArgs[0].toIntOrNull() == null) {
            unquotedArgs[0]
        } else {
            null
        }
        val radiusArgs = if (targetFactionName != null) unquotedArgs.drop(1) else unquotedArgs.toList()
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                var faction: MfFaction? = null
                if (targetFactionName != null) {
                    // An explicitly named faction is only reachable with mf.force.unclaim, which replaces the
                    // faction's own role check, matching how mf.force.flag is honoured. Only that faction's claims
                    // are removed, even if bypass mode is enabled.
                    faction = factionService.getFaction(MfFactionId(targetFactionName)) ?: factionService.getFaction(targetFactionName)
                    if (faction == null) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimInvalidFaction", targetFactionName]}")
                        return@Runnable
                    }
                } else if (!mfPlayer.isBypassEnabled) { // skip faction check if in bypass mode
                    faction = factionService.getFaction(mfPlayer.id)
                    if (faction == null) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimMustBeInAFaction"]}")
                        return@Runnable
                    }
                    val role = faction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(faction, plugin.factionPermissions.unclaim)) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNoFactionPermission"]}")
                        return@Runnable
                    }
                }
                val radius = radiusArgs.firstOrNull()?.toIntOrNull()
                val maxClaimRadius = plugin.config.getInt("factions.maxClaimRadius")
                if (radius != null && (radius < 0 || radius > maxClaimRadius)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimMaxClaimRadius", maxClaimRadius.toString()]}")
                    return@Runnable
                }
                val claimService = plugin.services.claimService
                val senderChunkX = senderChunkPosition.x
                val senderChunkZ = senderChunkPosition.z
                val chunks = if (radius == null) {
                    listOf(senderChunkPosition)
                } else {
                    (senderChunkX - radius..senderChunkX + radius).flatMap { x ->
                        (senderChunkZ - radius..senderChunkZ + radius).filter { z ->
                            val a = x - senderChunkX
                            val b = z - senderChunkZ
                            (a * a) + (b * b) <= radius * radius
                        }.map { z -> MfChunkPosition(senderChunkPosition.worldId, x, z) }
                    }
                }
                val claims: List<MfClaimedChunk> = if (targetFactionName != null || !mfPlayer.isBypassEnabled) {
                    chunks.mapNotNull { chunk ->
                        claimService.getClaim(chunk)
                    }.filter { claim ->
                        if (faction == null) {
                            return@filter false
                        }
                        return@filter claim.factionId.value == faction.id.value
                    }
                } else {
                    chunks.mapNotNull { chunk ->
                        claimService.getClaim(chunk)
                    }
                }
                if (claims.isEmpty()) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNoUnclaimableChunks"]}")
                    return@Runnable
                }
                // Count the claims actually removed: the radius covers every chunk in range, most of which may be
                // unclaimed or held by another faction.
                var removedClaimCount = 0
                claims.forEach { claim ->
                    claimService.delete(claim)
                        .onFailure {
                            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimFailedToDeleteClaim"]}")
                            plugin.logger.log(SEVERE, "Failed to delete claimed chunk: ${it.reason.message}", it.reason.cause)
                            return@Runnable
                        }
                    removedClaimCount++
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionUnclaimSuccess", removedClaimCount.toString()]}")
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}
