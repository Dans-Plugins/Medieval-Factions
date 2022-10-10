package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import java.util.concurrent.ConcurrentHashMap

class MfLockService(private val plugin: MedievalFactions, private val repository: MfLockRepository) {

    private val lockedBlocks: MutableMap<MfBlockPosition, MfLockedBlock> = ConcurrentHashMap()

    init {
        plugin.logger.info("Loading locked blocks...")
        val startTime = System.currentTimeMillis()
        lockedBlocks.putAll(repository.getLockedBlocks().map { it.block to it })
        plugin.logger.info("${lockedBlocks.size} locked blocks loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    fun lock(block: MfBlockPosition, claim: MfClaimedChunk, player: MfPlayer): Result4k<MfLockedBlock, ServiceFailure> = resultFrom {
        val lockedBlock = repository.upsert(MfLockedBlock(
            block = MfBlockPosition(
                worldId = block.worldId,
                x = block.x,
                y = block.y,
                z = block.z
            ),
            chunkX = claim.x,
            chunkZ = claim.z,
            playerId = player.id,
            accessors = emptyList()
        ))
        lockedBlocks[block] = lockedBlock
        return@resultFrom lockedBlock
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun save(lockedBlock: MfLockedBlock): Result4k<MfLockedBlock, ServiceFailure> = resultFrom {
        val upsertedLockedBlock = repository.upsert(lockedBlock)
        lockedBlocks[lockedBlock.block] = lockedBlock
        return@resultFrom upsertedLockedBlock
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun delete(block: MfBlockPosition): Result4k<Unit, ServiceFailure> = resultFrom {
        repository.delete(block)
        lockedBlocks.remove(block)
        return@resultFrom
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun getLockedBlock(block: MfBlockPosition): MfLockedBlock? {
        return lockedBlocks[block]
    }

    fun getLockedBlock(id: MfLockedBlockId): Result4k<MfLockedBlock?, ServiceFailure> = resultFrom {
        repository.getLockedBlock(id)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun getLockedBlocks(playerId: MfPlayerId): List<MfLockedBlock> {
        return lockedBlocks.values.filter { it.playerId == playerId }
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> ServiceFailureType.CONFLICT
            else -> ServiceFailureType.GENERAL
        }
    }

}