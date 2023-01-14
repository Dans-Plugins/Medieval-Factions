package com.dansplugins.factionsystem.law

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import com.dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom

class MfLawService(private val repository: MfLawRepository) {

    @JvmName("getLawByLawId")
    fun getLaw(id: MfLawId): MfLaw? = repository.getLaw(id)

    @JvmName("getLawByIndex")
    fun getLaw(factionId: MfFactionId, index: Int?): MfLaw? = repository.getLaw(factionId, index)

    @JvmName("getLawsByFactionId")
    fun getLaws(factionId: MfFactionId): List<MfLaw> = repository.getLaws(factionId)
    fun save(law: MfLaw): Result4k<MfLaw, ServiceFailure> = resultFrom {
        repository.upsert(law)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("deleteLaw")
    fun delete(law: MfLaw): Result4k<Unit, ServiceFailure> = resultFrom {
        repository.delete(law)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("deleteLawByLawId")
    fun delete(id: MfLawId): Result4k<Unit, ServiceFailure> = resultFrom {
        repository.delete(id)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun move(law: MfLaw, number: Int): Result4k<Unit, ServiceFailure> = resultFrom {
        repository.move(law, number)
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
