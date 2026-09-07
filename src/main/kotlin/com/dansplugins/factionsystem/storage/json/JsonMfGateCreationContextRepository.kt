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

    private fun loadData(): ContextData =
        storageManager.loadJsonData(fileName, "gate creation contexts", gson, ContextData::class.java) { ContextData() }

    private fun saveData(data: ContextData) {
        storageManager.writeJsonFile(fileName, data, null)
    }

    override fun getContext(playerId: MfPlayerId): MfGateCreationContext? {
        val data = loadData()
        return data.contexts.find { it.playerId == playerId }
    }

    override fun upsert(context: MfGateCreationContext): MfGateCreationContext = storageManager.withFileLock(fileName) {
        val data = loadData()
        val existingIndex = data.contexts.indexOfFirst { it.playerId == context.playerId }

        val updated = if (existingIndex >= 0) {
            val existing = data.contexts[existingIndex]
            if (existing.version != context.version) {
                throw OptimisticLockingFailureException("Invalid version: ${context.version}")
            }
            context.copy(version = context.version + 1).also { data.contexts[existingIndex] = it }
        } else {
            context.copy(version = 1).also { data.contexts.add(it) }
        }
        saveData(data)
        updated
    }

    override fun delete(playerId: MfPlayerId) = storageManager.withFileLock(fileName) {
        val data = loadData()
        data.contexts.removeIf { it.playerId == playerId }
        saveData(data)
    }
}
