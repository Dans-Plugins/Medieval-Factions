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
import dev.forkhandles.result4k.resultFrom

class MfFactionService(private val plugin: MedievalFactions, private val repository: MfFactionRepository) {

    val factions: List<MfFaction>
        get() = repository.getFactions()

    fun getFaction(name: String): MfFaction? = repository.getFaction(name)
    fun getFaction(playerId: MfPlayerId): MfFaction? = repository.getFaction(playerId)
    fun getFaction(factionId: MfFactionId): MfFaction? = repository.getFaction(factionId)
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
            val newMembers = faction.members.map { it.player.id } - previousState.members.map { it.player.id }
            newMembers.forEach { newMember ->
                val event = FactionJoinEvent(faction.id, newMember, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
            val oldMembers = previousState.members.map { it.player.id } - faction.members.map { it.player.id }
            oldMembers.forEach { oldMember ->
                val event = FactionLeaveEvent(faction.id, oldMember, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
        }
        repository.upsert(faction)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }
    fun delete(factionId: MfFactionId): Result4k<Unit, ServiceFailure> = resultFrom {
        val event = FactionDisbandEvent(factionId, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            throw EventCancelledException("Event cancelled")
        }
        repository.delete(factionId)
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