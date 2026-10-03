package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.bukkit.Material
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level.SEVERE

class MfGateService(
    private val plugin: MedievalFactions,
    private val gateRepo: MfGateRepository,
    private val gateCreationContextRepo: MfGateCreationContextRepository,
    val blockSafety: MfGateBlockSafety = MfGateBlockSafety()
) {

    private val gatesById: MutableMap<MfGateId, MfGate> = ConcurrentHashMap()
    val gates: List<MfGate>
        get() = gatesById.values.toList()

    // Restricted block materials now comes from the config file
    val restrictedBlockMaterials: Set<Material>

    init {
        plugin.logger.info("Loading gates...")
        val startTime = System.currentTimeMillis()
        gatesById.putAll(gateRepo.getGates().associateBy { it.id })
        plugin.logger.info("${gatesById.size} gates loaded (${System.currentTimeMillis() - startTime}ms)")

        restrictedBlockMaterials = loadRestrictedBlocksFromConfig()
        plugin.logger.info("Loaded ${restrictedBlockMaterials.size} restricted block materials.")

        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                try {
                    warnAboutGatesWithRestrictedMaterials()
                } catch (e: Exception) {
                    plugin.logger.log(SEVERE, "Error during gate restricted material review:", e)
                }
            }
        )

        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                try {
                    warnAboutGatesWithDataHoldingMaterials()
                } catch (e: Exception) {
                    plugin.logger.log(SEVERE, "Error during gate data-holding material review:", e)
                }
            }
        )
    }

    fun getGatesByTrigger(trigger: MfBlockPosition) = gatesById.values.filter { it.trigger == trigger }
    fun getGatesAt(block: MfBlockPosition) = gatesById.values.filter { it.area.contains(block) }

    @JvmName("getGatesByFactionId")
    fun getGatesByFaction(factionId: MfFactionId) = gatesById.values.filter { it.factionId == factionId }
    fun getGatesByStatus(status: MfGateStatus) = gatesById.values.filter { it.status == status }

    /**
     * Save a gate to the database with automatic retry on optimistic locking failures.
     * * This method implements a retry mechanism to handle concurrent updates to the same gate,
     * which commonly occurs during gate opening/closing animations where multiple async tasks
     * may attempt to save status changes simultaneously.
     * * On retry, the method re-fetches the current gate state from the database and re-applies
     * the intended status change. This is designed for the common case where only the status
     * field is being modified (e.g., gate.copy(status = OPENING)).
     * * @param gate The gate to save (typically with a status change)
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @return Result containing the saved gate or a ServiceFailure
     */
    fun save(gate: MfGate, maxRetries: Int = 3) = resultFrom {
        var lastException: Exception? = null
        var currentGate = gate
        val targetStatus = gate.status // Preserve the intended status change

        repeat(maxRetries) { attempt ->
            try {
                val result = gateRepo.upsert(currentGate)
                gatesById[result.id] = result
                return@resultFrom result
            } catch (e: OptimisticLockingFailureException) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    // Re-fetch the current state from the database for next retry
                    val freshGate = gateRepo.getGate(currentGate.id) ?: throw e
                    // Apply the intended status change to the fresh gate state
                    currentGate = freshGate.copy(status = targetStatus)
                    // Small delay before retry to reduce contention (runs in async context)
                    Thread.sleep(50L * (attempt + 1))
                }
            }
        }

        throw lastException ?: IllegalStateException("Retry failed without exception")
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

    /**
     * `gates.restrictedBlocks` is enforced only when a gate is created (#2071). Gates that already exist and are made
     * of a restricted material (for example, gates created while the list loaded empty) are loaded and kept, and are
     * named here once at startup so that admins can rebuild them. No gate data is changed or deleted.
     */
    internal fun warnAboutGatesWithRestrictedMaterials(): List<MfGate> {
        val affected = gates.filter { it.material in restrictedBlockMaterials }
        affected.forEach { gate ->
            plugin.logger.warning(
                "Gate ${gate.id.value} (faction ${gate.factionId.value}) is made of ${gate.material}, which is listed " +
                    "in gates.restrictedBlocks. New gates of this material are refused. Consider removing and " +
                    "rebuilding it from a different block. The gate has not been changed."
            )
        }
        return affected
    }

    /**
     * Gates made of block-entity materials (chests, barrels, hoppers, ...) lose the blocks' contents whenever
     * they open, because a gate only stores a material (#1255). New gates of such materials are refused at
     * creation; gates created before that check are still loaded and work as before, but are named here once at
     * startup so that admins can rebuild them. No gate data is changed.
     */
    internal fun warnAboutGatesWithDataHoldingMaterials(): List<MfGate> {
        val affected = gates.filter { blockSafety.isDataHoldingMaterial(it.material) == true }
        affected.forEach { gate ->
            plugin.logger.warning(
                "Gate ${gate.id.value} (faction ${gate.factionId.value}) is made of ${gate.material}, which holds " +
                    "contents or data that is lost every time the gate opens. Consider removing and rebuilding it " +
                    "from a different block. The gate has not been changed."
            )
        }
        return affected
    }

    private fun loadRestrictedBlocksFromConfig(): Set<Material> {
        val blockNames = flattenMaterialNames(plugin.config.getList("gates.restrictedBlocks"))
        return blockNames.mapNotNull { blockName ->
            try {
                Material.valueOf(blockName)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid block material in config: $blockName")
                null
            }
        }.toSet()
    }

    companion object {
        /**
         * Reads a material-name list that may be nested. The bundled config.yml groups `gates.restrictedBlocks` with
         * YAML anchors and aliases, so the value is a list of lists; Bukkit's `getStringList` skips the nested
         * entries, which made the list load empty (#2071). Nested lists are flattened in order, other entries are
         * converted to strings, and nulls are dropped. Duplicates are kept here; callers collect into a set.
         */
        internal fun flattenMaterialNames(raw: Any?): List<String> = when (raw) {
            null -> emptyList()
            is Iterable<*> -> raw.flatMap { flattenMaterialNames(it) }
            is Array<*> -> raw.flatMap { flattenMaterialNames(it) }
            else -> listOf(raw.toString())
        }
    }
}
