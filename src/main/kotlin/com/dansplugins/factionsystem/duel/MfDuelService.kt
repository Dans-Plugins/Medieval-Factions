package com.dansplugins.factionsystem.duel

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import java.util.concurrent.ConcurrentHashMap

class MfDuelService(
    private val plugin: MedievalFactions,
    private val duelRepo: MfDuelRepository,
    private val duelInviteRepo: MfDuelInviteRepository
) {

    private val duelsById = ConcurrentHashMap<MfDuelId, MfDuel>()
    val duels: List<MfDuel>
        get() = duelsById.values.toList()

    init {
        plugin.logger.info("Loading duels...")
        val startTime = System.currentTimeMillis()
        duelsById.putAll(duelRepo.getDuels().associateBy(MfDuel::id))
        plugin.logger.info("${duels.size} duels loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    fun getDuel(playerId: MfPlayerId): MfDuel? {
        return duelsById.values.singleOrNull { it.challengerId == playerId || it.challengedId == playerId }
    }

    fun getDuel(duelId: MfDuelId): MfDuel? {
        return duelsById[duelId]
    }

    fun save(duel: MfDuel) = resultFrom {
        val result = duelRepo.upsert(duel)
        duelsById[result.id] = result
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun delete(duelId: MfDuelId) = resultFrom {
        val result = duelRepo.delete(duelId)
        duelsById.remove(duelId)
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun getInvite(inviter: MfPlayerId, invitee: MfPlayerId) = resultFrom {
        duelInviteRepo.getInvite(inviter, invitee)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun save(invite: MfDuelInvite) = resultFrom {
        duelInviteRepo.upsert(invite)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun deleteInvite(inviter: MfPlayerId, invitee: MfPlayerId) = resultFrom {
        duelInviteRepo.deleteInvite(inviter, invitee)
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