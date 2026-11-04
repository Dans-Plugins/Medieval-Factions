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

    private fun loadData(): FactionData =
        storageManager.loadJsonData(fileName, "factions", gson, FactionData::class.java) { FactionData() }

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
        updated.withMembersInSavedRoles()
    }

    /**
     * Each member holds its own copy of its role, and only the role's id is written, so a save that changes a role
     * (a permission, a name) leaves the members' copies out of date. [com.dansplugins.factionsystem.faction.MfFactionService]
     * caches the returned faction, and permission checks read the member's copy, so the change would not apply until
     * the faction was next read from the file at startup. A member whose role is not among the faction's roles is kept
     * as it is rather than dropped or moved to another role.
     */
    private fun MfFaction.withMembersInSavedRoles() = copy(
        members = members.map { member -> roles.getRole(member.role.id)?.let { member.copy(role = it) } ?: member }
    )

    override fun delete(factionId: MfFactionId) = storageManager.withFileLock(fileName) {
        val data = loadData()
        data.factions.removeIf { it.id == factionId.value }
        saveData(data)
    }
}
