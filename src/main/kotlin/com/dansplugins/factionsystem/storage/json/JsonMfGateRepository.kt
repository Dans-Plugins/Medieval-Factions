package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateId
import com.dansplugins.factionsystem.gate.MfGateRepository
import com.google.gson.Gson

class JsonMfGateRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfGateRepository {
    
    private val fileName = "gates.json"
    private val gson: Gson = Gson()
    
    data class GateData(
        val gates: MutableList<MfGate> = mutableListOf()
    )
    
    private fun loadData(): GateData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, GateData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse gates JSON: ${e.message}")
                GateData()
            }
        } else {
            GateData()
        }
    }
    
    private fun saveData(data: GateData) {
        storageManager.writeJsonFile(fileName, data, null)
    }
    
    override fun getGate(id: MfGateId): MfGate? {
        val data = loadData()
        return data.gates.find { it.id == id }
    }
    
    override fun getGates(): List<MfGate> {
        val data = loadData()
        return data.gates.toList()
    }
    
    override fun upsert(gate: MfGate): MfGate {
        val data = loadData()
        val existingIndex = data.gates.indexOfFirst { it.id == gate.id }
        
        if (existingIndex >= 0) {
            val existing = data.gates[existingIndex]
            if (existing.version != gate.version) {
                throw OptimisticLockingFailureException("Invalid version: ${gate.version}")
            }
            val updated = gate.copy(version = gate.version + 1)
            data.gates[existingIndex] = updated
            saveData(data)
            return updated
        } else {
            val newGate = gate.copy(version = 1)
            data.gates.add(newGate)
            saveData(data)
            return newGate
        }
    }
    
    override fun delete(gateId: MfGateId) {
        val data = loadData()
        data.gates.removeIf { it.id == gateId }
        saveData(data)
    }
    
    override fun deleteAll(factionId: MfFactionId) {
        val data = loadData()
        data.gates.removeIf { it.factionId == factionId }
        saveData(data)
    }
}
