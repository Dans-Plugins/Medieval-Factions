package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.event.faction.FactionClaimEvent
import com.dansplugins.factionsystem.event.faction.FactionUnclaimEvent
import com.dansplugins.factionsystem.exception.EventCancelledException
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import net.md_5.bungee.api.ChatColor
import org.bukkit.Chunk
import org.bukkit.World
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MfClaimService(private val plugin: MedievalFactions, private val repository: MfClaimedChunkRepository) {

    private data class ClaimKey(val worldId: UUID, val x: Int, val z: Int) {
        constructor(claimedChunk: MfClaimedChunk): this(claimedChunk.worldId, claimedChunk.x, claimedChunk.z)
    }

    private val claimsByKey: MutableMap<ClaimKey, MfClaimedChunk> = ConcurrentHashMap()
    private val claims: List<MfClaimedChunk>
        get() = claimsByKey.values.toList()

    init {
        plugin.logger.info("Loading claims...")
        val startTime = System.currentTimeMillis()
        claimsByKey.putAll(repository.getClaims().associateBy { ClaimKey(it.worldId, it.x, it.z) })
        plugin.logger.info("${claimsByKey.size} claims loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    fun getClaim(worldId: UUID, x: Int, z: Int): MfClaimedChunk? = claimsByKey[ClaimKey(worldId, x, z)]
    fun getClaim(world: World, x: Int, z: Int): MfClaimedChunk? = getClaim(world.uid, x, z)
    fun getClaim(chunk: Chunk): MfClaimedChunk? = getClaim(chunk.world, chunk.x, chunk.z)
    fun getClaim(chunkPosition: MfChunkPosition): MfClaimedChunk? = getClaim(chunkPosition.worldId, chunkPosition.x, chunkPosition.z)
    fun getClaims(factionId: MfFactionId): List<MfClaimedChunk> = claims.filter { it.factionId == factionId }
    fun isInteractionAllowed(playerId: MfPlayerId, claim: MfClaimedChunk): Boolean {
        val factionService = plugin.services.factionService
        val playerFaction = factionService.getFaction(playerId) ?: return false
        if (claim.factionId == playerFaction.id) return true
        val claimFaction = factionService.getFaction(claim.factionId) ?: return true
        val relationshipService = plugin.services.factionRelationshipService
        val vassals = relationshipService.getVassalTree(claim.factionId)
        if (claimFaction.flags[plugin.flags.vassalageTreeCanInteractWithLand] && vassals.contains(playerFaction.id)) return true
        val allies = relationshipService.getRelationships(claim.factionId, MfFactionRelationshipType.ALLY).map { it.targetId }
        if (claimFaction.flags[plugin.flags.alliesCanInteractWithLand] && allies.contains(playerFaction.id)) return true
        return false
    }

    fun save(claim: MfClaimedChunk) = resultFrom {
        val factionService = plugin.services.factionService
        val faction = factionService.getFaction(claim.factionId).let(::requireNotNull)
        val event = FactionClaimEvent(claim.factionId, claim, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) throw EventCancelledException("Event cancelled")
        val result = repository.upsert(event.claim)
        claimsByKey[ClaimKey(result)] = result
        plugin.server.scheduler.runTask(plugin, Runnable {
            val world = plugin.server.getWorld(event.claim.worldId)
            if (world != null) {
                val players = world.players.filter { it.location.chunk.x == claim.x && it.location.chunk.z == claim.z }
                if (players.isNotEmpty()) {
                    plugin.server.scheduler.runTask(plugin, Runnable {
                        players.forEach { player ->
                            player.resetTitle()
                            val title = "${ChatColor.of(faction.flags[plugin.flags.color])}${faction.name}"
                            player.sendTitle(title, null, 10, 70, 20)
                        }
                    })
                }
            }
        })
        val dynmapService = plugin.services.dynmapService
        if (dynmapService != null) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                dynmapService.updateClaims(faction)
            })
        }
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun delete(claim: MfClaimedChunk) = resultFrom {
        val event = FactionUnclaimEvent(claim.factionId, claim, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) throw EventCancelledException("Event cancelled")
        val result = repository.delete(event.claim.worldId, event.claim.x, event.claim.z)
        claimsByKey.remove(ClaimKey(event.claim))
        plugin.server.scheduler.runTask(plugin, Runnable {
            val world = plugin.server.getWorld(event.claim.worldId)
            if (world != null) {
                val players = world.players.filter { it.location.chunk.x == claim.x && it.location.chunk.z == claim.z }
                if (players.isNotEmpty()) {
                    players.forEach { player ->
                        player.resetTitle()
                        val title =
                            "${ChatColor.of(plugin.config.getString("wilderness.color"))}${plugin.language["Wilderness"]}"
                        player.sendTitle(title, null, 10, 70, 20)
                    }
                }
            }
        })
        val dynmapService = plugin.services.dynmapService
        if (dynmapService != null) {
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(claim.factionId)
            if (faction != null) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    dynmapService.updateClaims(faction)
                })
            }
        }
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun deleteAllClaims(factionId: MfFactionId) = resultFrom {
        val result = repository.deleteAll(factionId)
        val claimsToDelete = claimsByKey.filterValues { it.factionId == factionId }
        claimsToDelete.forEach { (key, value) ->
            claimsByKey.remove(key, value)
        }
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> ServiceFailureType.CONFLICT
            else -> ServiceFailureType.GENERAL
        }
    }

}