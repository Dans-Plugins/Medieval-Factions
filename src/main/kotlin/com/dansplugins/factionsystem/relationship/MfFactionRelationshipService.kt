package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom

class MfFactionRelationshipService(private val repository: MfFactionRelationshipRepository) {

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
        repository.upsert(relationship)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun delete(id: MfFactionRelationshipId): Result4k<Unit, ServiceFailure> = resultFrom {
        repository.delete(id)
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