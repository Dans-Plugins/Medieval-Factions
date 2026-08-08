package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.duel.MfDuel
import com.dansplugins.factionsystem.duel.MfDuelId
import com.dansplugins.factionsystem.duel.MfDuelRepository
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.Instant

class JsonMfDuelRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfDuelRepository {

    private val fileName = "duels.json"

    // MfDuel carries an Instant, which Gson cannot populate reflectively. Writes go through
    // JsonStorageManager, whose Gson registers this adapter, but reads happen here and must register it
    // too. Without it every read throws, loadData swallows the failure and reports an empty file, so
    // stored duels become invisible and the next write overwrites them.
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
        .create()

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

    override fun upsert(duel: MfDuel): MfDuel = storageManager.withFileLock(fileName) {
        val data = loadData()
        val existingIndex = data.duels.indexOfFirst { it.id == duel.id }

        val updated = if (existingIndex >= 0) {
            val existing = data.duels[existingIndex]
            if (existing.version != duel.version) {
                throw OptimisticLockingFailureException("Invalid version: ${duel.version}")
            }
            duel.copy(version = duel.version + 1).also { data.duels[existingIndex] = it }
        } else {
            duel.copy(version = 1).also { data.duels.add(it) }
        }
        saveData(data)
        updated
    }

    override fun delete(id: MfDuelId) = storageManager.withFileLock(fileName) {
        val data = loadData()
        data.duels.removeIf { it.id == id }
        saveData(data)
    }
}
