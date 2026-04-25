package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionRepository
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.player.MfPlayerId
import com.google.gson.Gson
import org.everit.json.schema.Schema

/**
 * JSON-based implementation of MfFactionRepository
 */
class JsonMfFactionRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager,
    private val gson: Gson
) : MfFactionRepository {

    private val fileName = "factions.json"
    private val schema: Schema? = try {
        storageManager.loadSchemaFromResource("schemas/factions.json")
    } catch (e: Exception) {
        plugin.logger.severe("Could not load faction schema, validation will be skipped: ${e.message}")
        null
    }

    data class FactionData(
        val factions: MutableList<MfFaction> = mutableListOf()
    )

    private fun loadData(): FactionData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, FactionData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse factions JSON: ${e.message}")
                FactionData()
            }
        } else {
            FactionData()
        }
    }

    private fun saveData(data: FactionData) {
        storageManager.writeJsonFile(fileName, data, schema)
    }

    override fun getFaction(id: MfFactionId): MfFaction? {
        val data = loadData()
        return data.factions.find { it.id == id }
    }

    override fun getFaction(name: String): MfFaction? {
        val data = loadData()
        return data.factions.find { it.name == name }
    }

    override fun getFaction(playerId: MfPlayerId): MfFaction? {
        val data = loadData()
        return data.factions.find { faction ->
            faction.members.any { it.playerId == playerId }
        }
    }

    override fun getFactions(): List<MfFaction> {
        val data = loadData()
        return data.factions.toList()
    }

    override fun upsert(faction: MfFaction): MfFaction {
        val data = loadData()
        val existingIndex = data.factions.indexOfFirst { it.id == faction.id }

        if (existingIndex >= 0) {
            val existing = data.factions[existingIndex]
            if (existing.version != faction.version) {
                throw OptimisticLockingFailureException("Invalid version: ${faction.version}")
            }
            val updated = faction.copy(version = faction.version + 1)
            data.factions[existingIndex] = updated
            saveData(data)
            return updated
        } else {
            val newFaction = faction.copy(version = 1)
            data.factions.add(newFaction)
            saveData(data)
            return newFaction
        }
    }

    override fun delete(factionId: MfFactionId) {
        val data = loadData()
        data.factions.removeIf { it.id == factionId }
        saveData(data)
    }
}
