package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.event.faction.*
import com.dansplugins.factionsystem.exception.EventCancelledException
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import com.dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.resultFrom
import java.util.concurrent.ConcurrentHashMap

class MfFactionService(private val plugin: MedievalFactions, private val repository: MfFactionRepository) {

    private val factionsById: MutableMap<MfFactionId, MfFaction> = ConcurrentHashMap()
    val factions: List<MfFaction>
        get() = factionsById.values.toList()

    init {
        plugin.logger.info("Loading factions...")
        val startTime = System.currentTimeMillis()
        factionsById.putAll(repository.getFactions().associateBy(MfFaction::id))
        plugin.logger.info("${factionsById.size} factions loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    fun getFaction(name: String): MfFaction? = factions.singleOrNull { it.name == name }
    fun getFaction(playerId: MfPlayerId): MfFaction? = factions.singleOrNull { faction ->
        faction.members.any { member -> member.playerId == playerId }
    }
    fun getFaction(factionId: MfFactionId): MfFaction? = factionsById[factionId]
    fun save(faction: MfFaction): Result4k<MfFaction, ServiceFailure> = resultFrom {
        val previousState = getFaction(faction.id)
        if (previousState == null) {
            val event = FactionCreateEvent(faction.id, faction, !plugin.server.isPrimaryThread)
            plugin.server.pluginManager.callEvent(event)
            if (event.isCancelled) {
                throw EventCancelledException("Event cancelled")
            }
        } else {
            if (previousState.name != faction.name) {
                val event = FactionRenameEvent(faction.id, faction.name, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
            if (previousState.description != faction.description) {
                val event = FactionDescriptionChangeEvent(faction.id, faction.description, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
            if (previousState.prefix != faction.prefix) {
                val event = FactionPrefixChangeEvent(faction.id, faction.prefix, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
            val newMembers = faction.members.map(MfFactionMember::playerId) - previousState.members.map(MfFactionMember::playerId)
            newMembers.forEach { newMember ->
                val event = FactionJoinEvent(faction.id, newMember, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
            val oldMembers = previousState.members.map(MfFactionMember::playerId) - faction.members.map(MfFactionMember::playerId)
            oldMembers.forEach { oldMember ->
                val event = FactionLeaveEvent(faction.id, oldMember, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
                val lockService = plugin.services.lockService
                lockService.getLockedBlocks(oldMember).forEach { lockedBlock ->
                    lockService.delete(lockedBlock.block)
                        .onFailure { failure -> throw failure.reason.cause }
                }
            }
        }
        val result = repository.upsert(faction)
        factionsById[result.id] = result
        val dynmapService = plugin.services.dynmapService
        if (dynmapService != null) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                dynmapService.updateClaims(result)
            })
        }
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }
    fun delete(factionId: MfFactionId): Result4k<Unit, ServiceFailure> = resultFrom {
        val event = FactionDisbandEvent(factionId, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            throw EventCancelledException("Event cancelled")
        }
        val result = repository.delete(factionId)
        factionsById.remove(factionId)
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> CONFLICT
            else -> GENERAL
        }
    }

}