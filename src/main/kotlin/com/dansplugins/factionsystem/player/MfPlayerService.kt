package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import com.dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.bukkit.OfflinePlayer
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

class MfPlayerService(private val plugin: MedievalFactions, private val playerRepository: MfPlayerRepository) {

    private val playersById: MutableMap<MfPlayerId, MfPlayer> = ConcurrentHashMap()

    init {
        plugin.logger.info("Loading players...")
        val startTime = System.currentTimeMillis()
        playersById.putAll(playerRepository.getPlayers().associateBy(MfPlayer::id))
        plugin.logger.info("${playersById.size} players loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    fun getPlayer(id: MfPlayerId): MfPlayer? {
        return playersById[id]
    }

    fun getPlayer(player: OfflinePlayer): MfPlayer? = getPlayer(MfPlayerId(player.uniqueId.toString()))

    fun save(player: MfPlayer): Result4k<MfPlayer, ServiceFailure> = resultFrom {
        val result = playerRepository.upsert(player)
        playersById[result.id] = result
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun updatePlayerPower(onlinePlayerIds: List<MfPlayerId>): Result4k<Unit, ServiceFailure> {
        return resultFrom {
            playerRepository.increaseOnlinePlayerPower(onlinePlayerIds)
            playerRepository.decreaseOfflinePlayerPower(onlinePlayerIds)
            playersById.putAll(playerRepository.getPlayers().associateBy(MfPlayer::id))
        }.mapFailure { exception ->
            ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
        }
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> CONFLICT
            else -> GENERAL
        }
    }

}