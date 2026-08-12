package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateId
import com.dansplugins.factionsystem.gate.MfGateRepository
import com.google.gson.Gson

/**
 * JSON-based implementation of MfGateRepository.
 *
 * Gates are persisted through [JsonGateDto] rather than as domain objects, because [MfGate] holds a
 * plugin reference that Gson cannot serialize.
 */
class JsonMfGateRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfGateRepository {

    private val fileName = "gates.json"
    private val gson: Gson = Gson()

    internal data class GateData(
        val gates: MutableList<JsonGateDto> = mutableListOf()
    )

    private fun loadData(): GateData =
        storageManager.loadJsonData(fileName, "gates", gson, GateData::class.java) { GateData() }

    private fun saveData(data: GateData) {
        storageManager.writeJsonFile(fileName, data, null)
    }

    override fun getGate(id: MfGateId): MfGate? =
        loadData().gates.find { it.id == id.value }?.toDomain(plugin)

    override fun getGates(): List<MfGate> = loadData().gates.mapNotNull { it.toDomain(plugin) }

    override fun upsert(gate: MfGate): MfGate = storageManager.withFileLock(fileName) {
        val data = loadData()
        val existingIndex = data.gates.indexOfFirst { it.id == gate.id.value }

        val updated = if (existingIndex >= 0) {
            if (data.gates[existingIndex].version != gate.version) {
                throw OptimisticLockingFailureException("Invalid version: ${gate.version}")
            }
            gate.copy(version = gate.version + 1).also { data.gates[existingIndex] = it.toDto() }
        } else {
            gate.copy(version = 1).also { data.gates.add(it.toDto()) }
        }
        saveData(data)
        updated
    }

    override fun delete(gateId: MfGateId) = storageManager.withFileLock(fileName) {
        val data = loadData()
        data.gates.removeIf { it.id == gateId.value }
        saveData(data)
    }

    override fun deleteAll(factionId: MfFactionId) = storageManager.withFileLock(fileName) {
        val data = loadData()
        data.gates.removeIf { it.factionId == factionId.value }
        saveData(data)
    }
}
