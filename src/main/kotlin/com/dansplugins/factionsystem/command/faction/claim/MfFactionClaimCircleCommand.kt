package com.dansplugins.factionsystem.command.faction.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.command.unquote
import com.dansplugins.factionsystem.exception.WorldClaimBlockedException
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.logging.Level

/**
 * Claims the sender's chunk, or an area around it: a circle of chunks within a radius (`/f claim [radius]`,
 * `/f claim circle [radius]`), a square (`/f claim square <size>`) or a rectangle (`/f claim rectangle <sizeX> <sizeZ>`),
 * centred on the sender's chunk. Square and rectangle sizes are side lengths in chunks (#1653).
 */
class MfFactionClaimCircleCommand(
    private val plugin: MedievalFactions,
    private val shape: MfClaimShape = MfClaimShape.CIRCLE
) : CommandExecutor, TabCompleter {

    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols.getInstance(plugin.language.locale))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission(shape.permission) && !sender.hasPermission("mf.claim")) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimNotAPlayer"]}")
            return true
        }
        val senderWorld = sender.world
        val senderChunk = sender.location.chunk
        val senderChunkX = senderChunk.x
        val senderChunkZ = senderChunk.z
        // With mf.force.claim, a faction name or ID may be given before the radius to claim on that faction's behalf
        // (/f claim <faction> [radius]). A lone integer is always read as a radius, so existing usage is unchanged.
        val hasForcePermission = sender.hasPermission("mf.force.claim")
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
                        sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = if (targetFactionName != null) {
                    // An explicitly named faction is only reachable with mf.force.claim, which replaces the faction's
                    // own role check, matching how mf.force.flag is honoured.
                    factionService.getFaction(MfFactionId(targetFactionName)) ?: factionService.getFaction(targetFactionName) ?: run {
                        sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimInvalidFaction", targetFactionName]}")
                        return@Runnable
                    }
                } else {
                    val ownFaction = factionService.getFaction(mfPlayer.id)
                    if (ownFaction == null) {
                        sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimMustBeInAFaction"]}")
                        return@Runnable
                    }
                    val role = ownFaction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(ownFaction, plugin.factionPermissions.claim)) {
                        sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimNoFactionPermission"]}")
                        return@Runnable
                    }
                    ownFaction
                }
                val claimService = plugin.services.claimService
                if (claimService.isClaimingBlockedInWorld(senderWorld)) {
                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimWorldBlocked"]}")
                    return@Runnable
                }
                val radius = radiusArgs.firstOrNull()?.toIntOrNull()
                val maxClaimRadius = plugin.config.getInt("factions.maxClaimRadius")
                // A square or rectangle may span as many chunks along each side as the widest circle
                val maxClaimSize = maxClaimRadius * 2 + 1
                val sizeX: Int
                val sizeZ: Int
                when (shape) {
                    MfClaimShape.CIRCLE -> {
                        if (radius != null && (radius < 0 || radius > maxClaimRadius)) {
                            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimMaxClaimRadius", maxClaimRadius.toString()]}")
                            return@Runnable
                        }
                        sizeX = 0
                        sizeZ = 0
                    }
                    MfClaimShape.SQUARE, MfClaimShape.RECTANGLE -> {
                        val parsedX = radius
                        val parsedZ = if (shape == MfClaimShape.SQUARE) radius else radiusArgs.getOrNull(1)?.toIntOrNull()
                        if (parsedX == null || parsedZ == null) {
                            sender.sendMessage("${ChatColor.RED}${plugin.language[shape.usageKey ?: "CommandFactionClaimSquareUsage"]}")
                            return@Runnable
                        }
                        if (parsedX !in 1..maxClaimSize || parsedZ !in 1..maxClaimSize) {
                            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimMaxClaimSize", maxClaimSize.toString()]}")
                            return@Runnable
                        }
                        sizeX = parsedX
                        sizeZ = parsedZ
                    }
                }
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        val chunks = if (shape != MfClaimShape.CIRCLE) {
                            // The sender's chunk is the centre, or just past it on a side of even length
                            val minX = senderChunkX - (sizeX - 1) / 2
                            val minZ = senderChunkZ - (sizeZ - 1) / 2
                            (minX until minX + sizeX).flatMap { x ->
                                (minZ until minZ + sizeZ).map { z -> MfChunkPosition(senderWorld.uid, x, z) }
                            }
                        } else if (radius == null) {
                            listOf(MfChunkPosition(senderWorld.uid, senderChunkX, senderChunkZ))
                        } else {
                            (senderChunkX - radius..senderChunkX + radius).flatMap { x ->
                                (senderChunkZ - radius..senderChunkZ + radius).filter { z ->
                                    val a = x - senderChunkX
                                    val b = z - senderChunkZ
                                    (a * a) + (b * b) <= radius * radius
                                }.map { z -> MfChunkPosition.fromBukkit(senderWorld.getChunkAt(x, z)) }
                            }
                        }
                        plugin.server.scheduler.runTaskAsynchronously(
                            plugin,
                            Runnable saveChunks@{
                                val claimService = plugin.services.claimService
                                // Chunks listed in factions.blockedClaimChunks are never claimable, whoever holds them now
                                val blockedChunks = chunks.filter(claimService::isChunkClaimBlocked)
                                val claims = (chunks - blockedChunks.toSet()).associateWith(claimService::getClaim)
                                val relationshipService = plugin.services.factionRelationshipService
                                val unclaimedChunks = claims.filter { (_, claim) -> claim == null }.keys
                                // With factions.preventConqueringSurroundedChunks, an enemy chunk enclosed by its owner's
                                // other claims cannot be taken until a neighbouring border chunk has been conquered
                                val surroundedChunks = claims
                                    .filter { (chunk, claim) -> claim != null && claim.factionId != faction.id && claimService.isConquestBlockedBySurroundingClaims(chunk, claim.factionId) }
                                    .keys
                                val contestedChunks = claims
                                    .filterKeys { it !in surroundedChunks }
                                    .mapNotNull { (chunk, claim) -> claim?.let { chunk to it } }
                                    .groupBy { (_, claim) -> claim.factionId }
                                    .filter { (claimFactionId, claims) ->
                                        val claimFaction = factionService.getFaction(claimFactionId) ?: return@filter true
                                        val relationships = relationshipService.getRelationships(faction.id, claimFactionId)
                                        val reverseRelationships = relationshipService.getRelationships(claimFactionId, faction.id)
                                        return@filter (relationships + reverseRelationships).any { it.type == MfFactionRelationshipType.AT_WAR } &&
                                            claimFaction.power <= claimService.getClaimPowerUsed(claimFactionId) - claimService.getClaimPowerCost(claims.map { (chunk, _) -> chunk })
                                    }
                                    .flatMap { it.value.map { (chunk, _) -> chunk } }
                                val claimableChunks = unclaimedChunks + contestedChunks
                                if (claimableChunks.isEmpty()) {
                                    val reason = when {
                                        surroundedChunks.isNotEmpty() -> "CommandFactionClaimSurroundedChunk"
                                        blockedChunks.isNotEmpty() -> "CommandFactionClaimChunkBlocked"
                                        else -> "CommandFactionClaimNoClaimableChunks"
                                    }
                                    sender.sendMessage("${ChatColor.RED}${plugin.language[reason]}")
                                    return@saveChunks
                                }
                                if (plugin.config.getBoolean("factions.limitLand") && claimService.getClaimPowerCost(claimableChunks) + claimService.getClaimPowerUsed(faction.id) > faction.power) {
                                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimReachedDemesneLimit", decimalFormat.format(claimService.getClaimLimit(faction.id, faction.power, senderWorld.uid))]}")
                                    return@saveChunks
                                }
                                // Checks if the attempted claim is connected to an already existing claim. Will make an exception if the faction has no claims.
                                if (plugin.config.getBoolean("factions.contiguousClaims") &&
                                    !claimService.isClaimAdjacent(faction.id, *claimableChunks.toTypedArray()) &&
                                    claimService.getClaims(faction.id).isNotEmpty()
                                ) {
                                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimNotContiguous"]}")
                                    return@saveChunks
                                }
                                if (claimService.exceedsMaxClaimDistance(faction.id, *claimableChunks.toTypedArray())) {
                                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimTooFarFromClaims", plugin.config.getInt("factions.maxClaimDistance").toString()]}")
                                    return@saveChunks
                                }
                                claimableChunks.forEach { chunk ->
                                    claimService.save(MfClaimedChunk(chunk, faction.id))
                                        .onFailure {
                                            when (it.reason.cause) {
                                                is WorldClaimBlockedException -> {
                                                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimWorldBlocked"]}")
                                                }
                                                else -> {
                                                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionClaimFailedToSaveClaim"]}")
                                                    plugin.logger.log(Level.SEVERE, "Failed to save claimed chunk: ${it.reason.message}", it.reason.cause)
                                                }
                                            }
                                            return@saveChunks
                                        }
                                }
                                sender.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionClaimSuccess", chunks.size.toString()]}")
                            }
                        )
                    }
                )
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
