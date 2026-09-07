package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipId
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipRepository
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import com.google.gson.Gson

class JsonMfFactionRelationshipRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfFactionRelationshipRepository {

    private val fileName = "relationships.json"
    private val gson: Gson = Gson()

    data class RelationshipData(
        val relationships: MutableList<MfFactionRelationship> = mutableListOf()
    )

    private fun loadData(): RelationshipData =
        storageManager.loadJsonData(fileName, "relationships", gson, RelationshipData::class.java) { RelationshipData() }

    private fun saveData(data: RelationshipData) {
        storageManager.writeJsonFile(fileName, data, null)
    }

    override fun getFactionRelationship(relationshipId: MfFactionRelationshipId): MfFactionRelationship? {
        val data = loadData()
        return data.relationships.find { it.id == relationshipId }
    }

    override fun getFactionRelationships(factionId: MfFactionId, targetId: MfFactionId): List<MfFactionRelationship> {
        val data = loadData()
        return data.relationships.filter { it.factionId == factionId && it.targetId == targetId }
    }

    override fun getFactionRelationships(factionId: MfFactionId, type: MfFactionRelationshipType): List<MfFactionRelationship> {
        val data = loadData()
        return data.relationships.filter { it.factionId == factionId && it.type == type }
    }

    override fun getFactionRelationships(factionId: MfFactionId): List<MfFactionRelationship> {
        val data = loadData()
        return data.relationships.filter { it.factionId == factionId }
    }

    override fun getFactionRelationships(): List<MfFactionRelationship> {
        val data = loadData()
        return data.relationships.toList()
    }

    override fun upsert(relationship: MfFactionRelationship): MfFactionRelationship = storageManager.withFileLock(fileName) {
        val data = loadData()
        data.relationships.removeIf { it.id == relationship.id }
        data.relationships.add(relationship)
        saveData(data)
        relationship
    }

    override fun delete(relationshipId: MfFactionRelationshipId) = storageManager.withFileLock(fileName) {
        val data = loadData()
        data.relationships.removeIf { it.id == relationshipId }
        saveData(data)
    }
}
