package com.dansplugins.factionsystem.relationship.rival

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Rivals (#1896): factions a faction has named as its rivals with `/faction rival`. Rivalry is shown in
 * `/faction info` and has no other effect. Rivals of a faction that no longer exists are never returned, so a disband
 * needs no clean-up here (the database also removes them with the faction).
 */
class MfFactionRivalService(private val plugin: MedievalFactions, private val repository: MfFactionRivalRepository) {

    private val rivals = CopyOnWriteArrayList<MfFactionRival>()

    init {
        rivals.addAll(repository.getRivals())
        plugin.logger.info("${rivals.size} faction rivalries loaded")
    }

    @JvmName("getRivalsByFactionId")
    fun getRivals(factionId: MfFactionId): List<MfFactionRival> =
        rivals.filter { it.factionId == factionId && exists(it.targetId) }

    @JvmName("getRival")
    fun getRival(factionId: MfFactionId, targetId: MfFactionId): MfFactionRival? =
        rivals.firstOrNull { it.factionId == factionId && it.targetId == targetId }

    fun add(rival: MfFactionRival): Result4k<MfFactionRival, ServiceFailure> = resultFrom {
        val saved = repository.insert(rival)
        rivals.removeIf { it.factionId == saved.factionId && it.targetId == saved.targetId }
        rivals.add(saved)
        saved
    }.mapFailure { exception ->
        ServiceFailure(ServiceFailureType.GENERAL, "Service error: ${exception.message}", exception)
    }

    @JvmName("removeRival")
    fun remove(factionId: MfFactionId, targetId: MfFactionId): Result4k<Unit, ServiceFailure> = resultFrom {
        repository.delete(factionId, targetId)
        rivals.removeIf { it.factionId == factionId && it.targetId == targetId }
        Unit
    }.mapFailure { exception ->
        ServiceFailure(ServiceFailureType.GENERAL, "Service error: ${exception.message}", exception)
    }

    private fun exists(factionId: MfFactionId) = plugin.services.factionService.getFaction(factionId) != null
}
