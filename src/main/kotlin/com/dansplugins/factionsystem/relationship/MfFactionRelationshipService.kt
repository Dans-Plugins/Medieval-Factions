package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.event.relationship.RelationshipCreateEvent
import com.dansplugins.factionsystem.event.relationship.RelationshipDeleteEvent
import com.dansplugins.factionsystem.exception.EventCancelledException
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom

class MfFactionRelationshipService(private val plugin: MedievalFactions, private val repository: MfFactionRelationshipRepository) {

    fun getRelationship(relationshipId: MfFactionRelationshipId): MfFactionRelationship? {
        return repository.getFactionRelationship(relationshipId)
    }

    fun getRelationships(factionId: MfFactionId, targetId: MfFactionId): List<MfFactionRelationship> {
        return repository.getFactionRelationships(factionId, targetId)
    }

    fun getRelationships(factionId: MfFactionId): List<MfFactionRelationship> {
        return repository.getFactionRelationships(factionId)
    }

    fun getRelationships(factionId: MfFactionId, type: MfFactionRelationshipType): List<MfFactionRelationship> {
        return repository.getFactionRelationships(factionId, type)
    }

    fun save(relationship: MfFactionRelationship): Result4k<MfFactionRelationship, ServiceFailure> = resultFrom {
        val previousState = getRelationship(relationship.id)
        if (previousState == null) {
            val event = RelationshipCreateEvent(relationship.id, relationship, !plugin.server.isPrimaryThread)
            plugin.server.pluginManager.callEvent(event)
            if (event.isCancelled) {
                throw EventCancelledException("Event cancelled")
            }
        }
        repository.upsert(relationship)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun delete(id: MfFactionRelationshipId): Result4k<Unit, ServiceFailure> = resultFrom {
        val event = RelationshipDeleteEvent(id, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            throw EventCancelledException("Event cancelled")
        }
        repository.delete(id)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun getVassalTree(factionId: MfFactionId): MfVassalNode {
        return MfVassalNode(
            factionId,
            getRelationships(factionId, VASSAL)
                .filter { relationship ->
                    getRelationships(relationship.targetId, factionId).any {
                        it.type == LIEGE
                    }
                }.map { getVassalTree(it.targetId) }
        )
    }

    fun getLiegeChain(factionId: MfFactionId): MfLiegeNode {
        val liege = getRelationships(factionId, LIEGE).singleOrNull()?.targetId
        return if (liege != null) {
            MfLiegeNode(factionId, getLiegeChain(liege))
        } else {
            MfLiegeNode(factionId, null)
        }
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> ServiceFailureType.CONFLICT
            else -> ServiceFailureType.GENERAL
        }
    }

}