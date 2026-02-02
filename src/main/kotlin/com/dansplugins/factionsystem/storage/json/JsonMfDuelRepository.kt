package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.duel.MfDuel
import com.dansplugins.factionsystem.duel.MfDuelId
import com.dansplugins.factionsystem.duel.MfDuelRepository
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.google.gson.Gson

class JsonMfDuelRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfDuelRepository {

    private val fileName = "duels.json"
    private val gson: Gson = Gson()

    data class DuelData(
        val duels: MutableList<MfDuel> = mutableListOf()
    )

    private fun loadData(): DuelData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, DuelData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse duels JSON: ${e.message}")
                DuelData()
            }
        } else {
            DuelData()
        }
    }

    private fun saveData(data: DuelData) {
        storageManager.writeJsonFile(fileName, data, null)
    }

    override fun getDuels(): List<MfDuel> {
        val data = loadData()
        return data.duels.toList()
    }

    override fun upsert(duel: MfDuel): MfDuel {
        val data = loadData()
        val existingIndex = data.duels.indexOfFirst { it.id == duel.id }

        if (existingIndex >= 0) {
            val existing = data.duels[existingIndex]
            if (existing.version != duel.version) {
                throw OptimisticLockingFailureException("Invalid version: ${duel.version}")
            }
            val updated = duel.copy(version = duel.version + 1)
            data.duels[existingIndex] = updated
            saveData(data)
            return updated
        } else {
            val newDuel = duel.copy(version = 1)
            data.duels.add(newDuel)
            saveData(data)
            return newDuel
        }
    }

    override fun delete(id: MfDuelId) {
        val data = loadData()
        data.duels.removeIf { it.id == id }
        saveData(data)
    }
}
