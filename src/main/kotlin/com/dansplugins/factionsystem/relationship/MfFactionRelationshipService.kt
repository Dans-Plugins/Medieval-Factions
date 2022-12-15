package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.event.relationship.RelationshipCreateEvent
import com.dansplugins.factionsystem.event.relationship.RelationshipDeleteEvent
import com.dansplugins.factionsystem.exception.EventCancelledException
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import java.util.concurrent.ConcurrentHashMap

class MfFactionRelationshipService(private val plugin: MedievalFactions, private val repository: MfFactionRelationshipRepository) {

    private val relationshipsById: MutableMap<MfFactionRelationshipId, MfFactionRelationship> = ConcurrentHashMap()
    private val relationships: List<MfFactionRelationship>
        get() = relationshipsById.values.toList()

    init {
        plugin.logger.info("Loading faction relationships...")
        val startTime = System.currentTimeMillis()
        relationshipsById.putAll(repository.getFactionRelationships().associateBy(MfFactionRelationship::id))
        plugin.logger.info("${relationshipsById.size} faction relationships loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    @JvmName("getRelationshipByRelationshipId")
    fun getRelationship(relationshipId: MfFactionRelationshipId): MfFactionRelationship? {
        return relationshipsById[relationshipId]
    }

    @JvmName("getRelationshipsByFactionIdAndTargetId")
    fun getRelationships(factionId: MfFactionId, targetId: MfFactionId): List<MfFactionRelationship> {
        return relationships.filter { it.factionId == factionId && it.targetId == targetId }
    }

    @JvmName("getRelationshipsByFactionId")
    fun getRelationships(factionId: MfFactionId): List<MfFactionRelationship> {
        return relationships.filter { it.factionId == factionId }
    }

    @JvmName("getRelationshipsByFactionIdAndType")
    fun getRelationships(factionId: MfFactionId, type: MfFactionRelationshipType): List<MfFactionRelationship> {
        return relationships.filter { it.factionId == factionId && it.type == type }
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
        val result = repository.upsert(relationship)
        relationshipsById[result.id] = result
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("deleteRelationshipByRelationshipId")
    fun delete(id: MfFactionRelationshipId): Result4k<Unit, ServiceFailure> = resultFrom {
        val event = RelationshipDeleteEvent(id, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            throw EventCancelledException("Event cancelled")
        }
        val result = repository.delete(id)
        relationshipsById.remove(id)
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("getVassalTreeByFactionId")
    fun getVassalTree(factionId: MfFactionId): MfVassalNode {
        return MfVassalNode(
            factionId,
            getVassals(factionId).map(::getVassalTree)
        )
    }

    @JvmName("getLiegeChainByFactionId")
    fun getLiegeChain(factionId: MfFactionId): MfLiegeNode {
        val liege = getLiege(factionId)
        return if (liege != null) {
            MfLiegeNode(factionId, getLiegeChain(liege))
        } else {
            MfLiegeNode(factionId, null)
        }
    }

    @JvmName("getLiegeByFactionId")
    fun getLiege(factionId: MfFactionId): MfFactionId? {
        val liege = getRelationships(factionId, LIEGE).firstOrNull()?.targetId
        if (liege != null) {
            val reverseRelationships = getRelationships(liege, factionId)
            if (reverseRelationships.any { it.type == VASSAL }) {
                return liege
            }
        }
        return null
    }

    @JvmName("getVassalsByFactionId")
    fun getVassals(factionId: MfFactionId): List<MfFactionId> {
        return getRelationships(factionId, VASSAL)
            .filter { relationship ->
                getRelationships(relationship.targetId, factionId).any {
                    it.type == LIEGE
                }
            }.map(MfFactionRelationship::targetId)
    }

    @JvmName("getAlliesByFactionId")
    fun getAllies(factionId: MfFactionId): List<MfFactionId> {
        return getRelationships(factionId, ALLY)
            .filter { relationship ->
                getRelationships(relationship.targetId, relationship.factionId).any {
                    it.type == ALLY
                }
            }.map(MfFactionRelationship::targetId)
    }

    @JvmName("getFactionsAtWarWithByFactionId")
    fun getFactionsAtWarWith(factionId: MfFactionId): List<MfFactionId> {
        return relationships
            .filter { it.type == AT_WAR && (it.factionId == factionId || it.targetId == factionId) }
            .map {
                if (it.factionId == factionId) it.targetId else it.factionId
            }.toSet().toList()
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> ServiceFailureType.CONFLICT
            else -> ServiceFailureType.GENERAL
        }
    }
}
