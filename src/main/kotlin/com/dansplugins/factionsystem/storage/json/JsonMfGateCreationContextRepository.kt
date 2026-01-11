package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.gate.MfGateCreationContext
import com.dansplugins.factionsystem.gate.MfGateCreationContextRepository
import com.dansplugins.factionsystem.player.MfPlayerId
import com.google.gson.Gson

class JsonMfGateCreationContextRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfGateCreationContextRepository {
    
    private val fileName = "gate_creation_contexts.json"
    private val gson: Gson = Gson()
    
    data class ContextData(
        val contexts: MutableList<MfGateCreationContext> = mutableListOf()
    )
    
    private fun loadData(): ContextData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, ContextData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse gate creation contexts JSON: ${e.message}")
                ContextData()
            }
        } else {
            ContextData()
        }
    }
    
    private fun saveData(data: ContextData) {
        storageManager.writeJsonFile(fileName, data, null)
    }
    
    override fun getContext(playerId: MfPlayerId): MfGateCreationContext? {
        val data = loadData()
        return data.contexts.find { it.playerId == playerId }
    }
    
    override fun upsert(context: MfGateCreationContext): MfGateCreationContext {
        val data = loadData()
        val existingIndex = data.contexts.indexOfFirst { it.playerId == context.playerId }
        
        if (existingIndex >= 0) {
            val existing = data.contexts[existingIndex]
            if (existing.version != context.version) {
                throw OptimisticLockingFailureException("Invalid version: ${context.version}")
            }
            val updated = context.copy(version = context.version + 1)
            data.contexts[existingIndex] = updated
            saveData(data)
            return updated
        } else {
            val newContext = context.copy(version = 1)
            data.contexts.add(newContext)
            saveData(data)
            return newContext
        }
    }
    
    override fun delete(playerId: MfPlayerId) {
        val data = loadData()
        data.contexts.removeIf { it.playerId == playerId }
        saveData(data)
    }
}
