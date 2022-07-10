package dansplugins.factionsystem.player

import dansplugins.factionsystem.failure.OptimisticLockingFailureException
import dansplugins.factionsystem.failure.ServiceFailure
import dansplugins.factionsystem.failure.ServiceFailureType
import dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
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

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> CONFLICT
            else -> GENERAL
        }
    }

}