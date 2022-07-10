package dansplugins.factionsystem.law

import dansplugins.factionsystem.faction.MfFactionId
import dansplugins.factionsystem.failure.OptimisticLockingFailureException
import dansplugins.factionsystem.failure.ServiceFailure
import dansplugins.factionsystem.failure.ServiceFailureType
import dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom

class MfLawService(private val repository: MfLawRepository) {

    fun getLaw(id: MfLawId): MfLaw? = repository.getLaw(id)
    fun getLaws(factionId: MfFactionId): List<MfLaw> = repository.getLaws(factionId)
    fun save(law: MfLaw): Result4k<MfLaw, ServiceFailure> = resultFrom {
        repository.upsert(law)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }
    fun delete(id: MfLawId) {
        repository.delete(id)
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> CONFLICT
            else -> GENERAL
        }
    }

}