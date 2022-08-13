package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import com.dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import dev.forkhandles.result4k.*
import org.bukkit.OfflinePlayer

class MfPlayerService(private val playerRepository: MfPlayerRepository) {

    fun getPlayer(id: MfPlayerId): MfPlayer? {
        return playerRepository.getPlayer(id)
    }

    fun getPlayer(player: OfflinePlayer): MfPlayer? = getPlayer(MfPlayerId(player.uniqueId.toString()))

    fun save(player: MfPlayer): Result4k<MfPlayer, ServiceFailure> = resultFrom {
        playerRepository.upsert(player)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun updatePlayerPower(onlinePlayerIds: List<MfPlayerId>): Result4k<Unit, ServiceFailure> {
        resultFrom {
            playerRepository.increaseOnlinePlayerPower(onlinePlayerIds)
        }.mapFailure { exception ->
            ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
        }.onFailure { return it }
        resultFrom {
            playerRepository.decreaseOfflinePlayerPower(onlinePlayerIds)
        }.mapFailure { exception ->
            ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
        }.onFailure { return it }
        return Success(Unit)
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> CONFLICT
            else -> GENERAL
        }
    }

}