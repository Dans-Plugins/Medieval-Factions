package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.bukkit.Chunk
import org.bukkit.World
import java.util.concurrent.CopyOnWriteArrayList

class MfClaimService(private val repository: MfClaimedChunkRepository) {

    private val claims: MutableList<MfClaimedChunk> = CopyOnWriteArrayList(repository.getClaims().toMutableList())
    fun getClaim(world: World, x: Int, z: Int): MfClaimedChunk? = repository.getClaim(world, x, z)
    fun getClaim(chunk: Chunk): MfClaimedChunk? = getClaim(chunk.world, chunk.x, chunk.z)
    fun getClaims(factionId: MfFactionId): List<MfClaimedChunk> = repository.getClaims(factionId)

    fun save(claim: MfClaimedChunk) = resultFrom {
        val result = repository.upsert(claim)
        claims.add(claim)
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun delete(world: World, x: Int, z: Int) = resultFrom {
        val result = repository.delete(world, x, z)
        claims.removeAll { it.worldId == world.uid && it.x == x && it.z == z }
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