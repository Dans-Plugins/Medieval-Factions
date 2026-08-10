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
 * JSON-based implementation of MfFactionRepository.
 *
 * Factions are persisted through [JsonFactionDto] rather than as domain objects, because [MfFaction]
 * (and the roles and flag values it contains) holds a plugin reference that Gson cannot serialize.
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

    internal data class FactionData(
        val factions: MutableList<JsonFactionDto> = mutableListOf()
    )

    private fun loadData(): FactionData {
        val json = storageManager.readJsonFileAsString(fileName) ?: return FactionData()
        return try {
            gson.fromJson(json, FactionData::class.java) ?: FactionData()
        } catch (e: Exception) {
            plugin.logger.severe("CRITICAL: Failed to parse factions JSON: ${e.message}")
            plugin.logger.severe("The JSON file may be corrupted. Creating a backup and returning empty data.")
            plugin.logger.severe("Please investigate the file: $fileName")
            try {
                val backupFile = java.io.File(storageManager.getStorageDirectory(), "$fileName.corrupted.backup")
                backupFile.writeText(json)
                plugin.logger.warning("Corrupted file backed up to: ${backupFile.absolutePath}")
            } catch (backupError: Exception) {
                plugin.logger.severe("Failed to create backup: ${backupError.message}")
            }
            FactionData()
        }
    }

    private fun saveData(data: FactionData) {
        storageManager.writeJsonFile(fileName, data, schema)
    }

    override fun getFaction(id: MfFactionId): MfFaction? =
        loadData().factions.find { it.id == id.value }?.toDomain(plugin)

    override fun getFaction(name: String): MfFaction? =
        loadData().factions.find { it.name == name }?.toDomain(plugin)

    override fun getFaction(playerId: MfPlayerId): MfFaction? =
        loadData().factions.find { faction -> faction.members.any { it.playerId == playerId.value } }
            ?.toDomain(plugin)

    override fun getFactions(): List<MfFaction> = loadData().factions.map { it.toDomain(plugin) }

    override fun upsert(faction: MfFaction): MfFaction = storageManager.withFileLock(fileName) {
        val data = loadData()
        val existingIndex = data.factions.indexOfFirst { it.id == faction.id.value }

        val updated = if (existingIndex >= 0) {
            if (data.factions[existingIndex].version != faction.version) {
                throw OptimisticLockingFailureException("Invalid version: ${faction.version}")
            }
            faction.copy(version = faction.version + 1).also { data.factions[existingIndex] = it.toDto() }
        } else {
            faction.copy(version = 1).also { data.factions.add(it.toDto()) }
        }
        saveData(data)
        updated
    }

    override fun delete(factionId: MfFactionId) = storageManager.withFileLock(fileName) {
        val data = loadData()
        data.factions.removeIf { it.id == factionId.value }
        saveData(data)
    }
}
