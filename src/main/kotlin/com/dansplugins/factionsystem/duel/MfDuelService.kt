package com.dansplugins.factionsystem.duel

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class MfDuelService(
    private val plugin: MedievalFactions,
    private val duelRepo: MfDuelRepository,
    private val duelInviteRepo: MfDuelInviteRepository
) {

    private val duelsById = ConcurrentHashMap<MfDuelId, MfDuel>()
    val duels: List<MfDuel>
        get() = duelsById.values.toList()

    private val duelInvites = CopyOnWriteArrayList<MfDuelInvite>()

    init {
        plugin.logger.info("Loading duels...")
        var startTime = System.currentTimeMillis()
        duelsById.putAll(duelRepo.getDuels().associateBy(MfDuel::id))
        plugin.logger.info("${duels.size} duels loaded (${System.currentTimeMillis() - startTime}ms)")

        plugin.logger.info("Loading duel invites...")
        startTime = System.currentTimeMillis()
        duelInvites.addAll(duelInviteRepo.getInvites())
        plugin.logger.info("${duelInvites.size} duel invites loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    @JvmName("getDuelByPlayerId")
    fun getDuel(playerId: MfPlayerId): MfDuel? {
        return duelsById.values.singleOrNull { it.challengerId == playerId || it.challengedId == playerId }
    }

    @JvmName("getDuelByDuelId")
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

    @JvmName("deleteDuelByDuelId")
    fun delete(duelId: MfDuelId) = resultFrom {
        val result = duelRepo.delete(duelId)
        duelsById.remove(duelId)
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("getInviteByInviterIdAndInviteeId")
    fun getInvite(inviter: MfPlayerId, invitee: MfPlayerId) = duelInvites.singleOrNull { it.inviterId == inviter && it.inviteeId == invitee }

    @JvmName("getInvitesByInviteeId")
    fun getInvitesByInvitee(invitee: MfPlayerId) = duelInvites.filter { it.inviteeId == invitee }

    @JvmName("getInvitesByInviterId")
    fun getInvitesByInviter(inviter: MfPlayerId) = duelInvites.filter { it.inviterId == inviter }

    fun save(invite: MfDuelInvite) = resultFrom {
        val result = duelInviteRepo.upsert(invite)
        duelInvites.add(result)
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("deleteInviteByInviterIdAndInviteeId")
    fun deleteInvite(inviter: MfPlayerId, invitee: MfPlayerId) = resultFrom {
        val result = duelInviteRepo.deleteInvite(inviter, invitee)
        val duelInvitesToRemove = duelInvites.filter { it.inviterId == inviter && it.inviteeId == invitee }
        duelInvites.removeAll(duelInvitesToRemove)
        return@resultFrom result
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
