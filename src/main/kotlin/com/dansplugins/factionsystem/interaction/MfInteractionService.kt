package com.dansplugins.factionsystem.interaction

import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import java.util.concurrent.ConcurrentHashMap

class MfInteractionService(private val repository: MfInteractionStatusRepository) {

    private val interactionStatus: MutableMap<String, MfInteractionStatus> = ConcurrentHashMap()

    @JvmName("getInteractionStatus")
    fun getInteractionStatus(playerId: MfPlayerId) = interactionStatus[playerId.value]

    @JvmName("setInteractionStatus")
    fun setInteractionStatus(playerId: MfPlayerId, status: MfInteractionStatus?): Result<Unit, ServiceFailure> {
        val result = resultFrom {
            repository.setInteractionStatus(playerId, status)
        }.mapFailure { exception ->
            ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
        }
        if (result is Success) {
            if (status != null) {
                interactionStatus[playerId.value] = status
            } else {
                interactionStatus.remove(playerId.value)
            }
        }
        return result
    }

    @JvmName("loadInteractionStatusByPlayerId")
    fun loadInteractionStatus(playerId: MfPlayerId) {
        val status = repository.getInteractionStatus(playerId)
        if (status != null) {
            interactionStatus[playerId.value] = status
        } else {
            interactionStatus.remove(playerId.value)
        }
    }

    @JvmName("unloadInteractionStatusByPlayerId")
    fun unloadInteractionStatus(playerId: MfPlayerId) {
        interactionStatus.remove(playerId.value)
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> ServiceFailureType.CONFLICT
            else -> ServiceFailureType.GENERAL
        }
    }
}
