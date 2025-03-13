package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.resultFrom
import org.bukkit.Material
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level.SEVERE

class MfGateService(
    private val plugin: MedievalFactions,
    private val gateRepo: MfGateRepository,
    private val gateCreationContextRepo: MfGateCreationContextRepository
) {

    private val gatesById: MutableMap<MfGateId, MfGate> = ConcurrentHashMap()
    val gates: List<MfGate>
        get() = gatesById.values.toList()

    init {
        plugin.logger.info("Loading gates...")
        val startTime = System.currentTimeMillis()
        gatesById.putAll(gateRepo.getGates().associateBy { it.id })
        plugin.logger.info("${gatesById.size} gates loaded (${System.currentTimeMillis() - startTime}ms)")

        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                try {
                    updateFallingBlockGates()
                } catch (e: Exception) {
                    plugin.logger.log(SEVERE, "Error during gate material review:", e)
                }
            }
        )
    }

    fun getGatesByTrigger(trigger: MfBlockPosition) = gatesById.values.filter { it.trigger == trigger }
    fun getGatesAt(block: MfBlockPosition) = gatesById.values.filter { it.area.contains(block) }

    @JvmName("getGatesByFactionId")
    fun getGatesByFaction(factionId: MfFactionId) = gatesById.values.filter { it.factionId == factionId }
    fun getGatesByStatus(status: MfGateStatus) = gatesById.values.filter { it.status == status }

    fun save(gate: MfGate) = resultFrom {
        val result = gateRepo.upsert(gate)
        gatesById[result.id] = result
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("deleteGateByGateId")
    fun delete(gateId: MfGateId) = resultFrom {
        val result = gateRepo.delete(gateId)
        gatesById.remove(gateId)
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("deleteAllGatesByFactionId")
    fun deleteAllGates(factionId: MfFactionId) = resultFrom {
        val result = gateRepo.deleteAll(factionId)
        val gatesToDelete = gatesById.filterValues { it.factionId == factionId }
        gatesToDelete.forEach { (key, value) ->
            gatesById.remove(key, value)
        }
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("getGateCreationContextByPlayerId")
    fun getGateCreationContext(playerId: MfPlayerId) = resultFrom {
        gateCreationContextRepo.getContext(playerId)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun save(ctx: MfGateCreationContext) = resultFrom {
        gateCreationContextRepo.upsert(ctx)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("deleteGateCreationContextByPlayerId")
    fun deleteGateCreationContext(playerId: MfPlayerId) = resultFrom {
        gateCreationContextRepo.delete(playerId)
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> ServiceFailureType.CONFLICT
            else -> ServiceFailureType.GENERAL
        }
    }

    private fun updateFallingBlockGates() {
        val gateService = plugin.services.gateService

        gates.forEach { gate ->
            if (gate.material in fallingBlockMaterials) {
                plugin.logger.info("Deleting gate with ID: ${gate.id} as it uses a falling block material: ${gate.material}")

                gateService.delete(gate.id).onFailure {
                    plugin.logger.log(SEVERE, "Failed to delete gate with ID: ${gate.id}.") as Nothing
                }
            }
        }

        plugin.logger.info("Gate material review and deletion completed.")
    }

    val fallingBlockMaterials = setOf(
        Material.SAND,
        Material.GRAVEL,
        Material.ANVIL,
        Material.WHITE_CONCRETE_POWDER,
        Material.ORANGE_CONCRETE_POWDER,
        Material.MAGENTA_CONCRETE_POWDER,
        Material.LIGHT_BLUE_CONCRETE_POWDER,
        Material.YELLOW_CONCRETE_POWDER,
        Material.LIME_CONCRETE_POWDER,
        Material.PINK_CONCRETE_POWDER,
        Material.GRAY_CONCRETE_POWDER,
        Material.LIGHT_GRAY_CONCRETE_POWDER,
        Material.CYAN_CONCRETE_POWDER,
        Material.PURPLE_CONCRETE_POWDER,
        Material.BLUE_CONCRETE_POWDER,
        Material.BROWN_CONCRETE_POWDER,
        Material.GREEN_CONCRETE_POWDER,
        Material.RED_CONCRETE_POWDER,
        Material.BLACK_CONCRETE_POWDER
    )
}
