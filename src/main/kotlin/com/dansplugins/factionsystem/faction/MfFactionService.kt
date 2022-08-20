package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import com.dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom

class MfFactionService(private val repository: MfFactionRepository) {

    val factions: List<MfFaction>
        get() = repository.getFactions()

    fun getFaction(name: String): MfFaction? = repository.getFaction(name)
    fun getFaction(playerId: MfPlayerId): MfFaction? = repository.getFaction(playerId)
    fun getFaction(factionId: MfFactionId): MfFaction? = repository.getFaction(factionId)
    fun save(faction: MfFaction): Result4k<MfFaction, ServiceFailure> = resultFrom {
        repository.upsert(faction)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }
    fun delete(factionId: MfFactionId): Result4k<Unit, ServiceFailure> = resultFrom {
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