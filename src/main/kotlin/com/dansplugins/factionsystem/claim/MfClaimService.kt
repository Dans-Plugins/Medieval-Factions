package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.event.faction.FactionClaimEvent
import com.dansplugins.factionsystem.event.faction.FactionUnclaimEvent
import com.dansplugins.factionsystem.exception.EventCancelledException
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import net.md_5.bungee.api.ChatColor
import org.bukkit.Chunk
import org.bukkit.World
import java.util.concurrent.CopyOnWriteArrayList

class MfClaimService(private val plugin: MedievalFactions, private val repository: MfClaimedChunkRepository) {

    private val claims: MutableList<MfClaimedChunk>
    fun getClaim(world: World, x: Int, z: Int): MfClaimedChunk? = repository.getClaim(world, x, z)
    fun getClaim(chunk: Chunk): MfClaimedChunk? = getClaim(chunk.world, chunk.x, chunk.z)
    fun getClaims(factionId: MfFactionId): List<MfClaimedChunk> = repository.getClaims(factionId)

    init {
        plugin.logger.info("Loading claims...")
        val startTime = System.currentTimeMillis()
        claims = CopyOnWriteArrayList(repository.getClaims().toMutableList())
        plugin.logger.info("Claims loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    fun save(claim: MfClaimedChunk) = resultFrom {
        val factionService = plugin.services.factionService
        val faction = factionService.getFaction(claim.factionId).let(::requireNotNull)
        val event = FactionClaimEvent(claim.factionId, claim, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) throw EventCancelledException("Event cancelled")
        val result = repository.upsert(event.claim)
        claims.add(event.claim)
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
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun delete(claim: MfClaimedChunk) = resultFrom {
        val event = FactionUnclaimEvent(claim.factionId, claim, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) throw EventCancelledException("Event cancelled")
        val result = repository.delete(event.claim.worldId, event.claim.x, event.claim.z)
        claims.removeAll { it.worldId == event.claim.worldId && it.x == event.claim.x && it.z == event.claim.z }
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
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun deleteAllClaims(factionId: MfFactionId) = resultFrom {
        val result = repository.deleteAll(factionId)
        claims.removeAll { it.factionId.value == factionId.value }
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