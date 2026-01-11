package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.claim.MfClaimedChunkRepository
import com.dansplugins.factionsystem.faction.MfFactionId
import com.google.gson.Gson
import java.util.*

class JsonMfClaimedChunkRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfClaimedChunkRepository {
    
    private val fileName = "claimed_chunks.json"
    private val gson: Gson = Gson()
    
    data class ClaimData(
        val claims: MutableList<MfClaimedChunk> = mutableListOf()
    )
    
    private fun loadData(): ClaimData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, ClaimData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse claims JSON: ${e.message}")
                ClaimData()
            }
        } else {
            ClaimData()
        }
    }
    
    private fun saveData(data: ClaimData) {
        storageManager.writeJsonFile(fileName, data, null)
    }
    
    override fun getClaim(worldId: UUID, x: Int, z: Int): MfClaimedChunk? {
        val data = loadData()
        return data.claims.find { it.worldId == worldId && it.x == x && it.z == z }
    }
    
    override fun getClaims(factionId: MfFactionId): List<MfClaimedChunk> {
        val data = loadData()
        return data.claims.filter { it.factionId == factionId }
    }
    
    override fun getClaims(): List<MfClaimedChunk> {
        val data = loadData()
        return data.claims.toList()
    }
    
    override fun upsert(claim: MfClaimedChunk): MfClaimedChunk {
        val data = loadData()
        data.claims.removeIf { it.worldId == claim.worldId && it.x == claim.x && it.z == claim.z }
        data.claims.add(claim)
        saveData(data)
        return claim
    }
    
    override fun delete(worldId: UUID, x: Int, z: Int) {
        val data = loadData()
        data.claims.removeIf { it.worldId == worldId && it.x == x && it.z == z }
        saveData(data)
    }
    
    override fun deleteAll(factionId: MfFactionId) {
        val data = loadData()
        data.claims.removeIf { it.factionId == factionId }
        saveData(data)
    }
}
