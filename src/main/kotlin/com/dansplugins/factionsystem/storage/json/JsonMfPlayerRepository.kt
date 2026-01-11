package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfFactionChatChannel
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.everit.json.schema.Schema
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * JSON-based implementation of MfPlayerRepository
 */
class JsonMfPlayerRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfPlayerRepository {
    
    private val fileName = "players.json"
    private val schema: Schema? = try {
        storageManager.loadSchemaFromResource("schemas/players.json")
    } catch (e: Exception) {
        plugin.logger.warning("Could not load player schema, validation will be skipped: ${e.message}")
        null
    }
    
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    data class PlayerData(
        val players: MutableList<MfPlayer> = mutableListOf()
    )
    
    private fun loadData(): PlayerData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, PlayerData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse players JSON: ${e.message}")
                PlayerData()
            }
        } else {
            PlayerData()
        }
    }
    
    private fun saveData(data: PlayerData) {
        storageManager.writeJsonFile(fileName, data, schema)
    }
    
    override fun getPlayer(id: MfPlayerId): MfPlayer? {
        val data = loadData()
        return data.players.find { it.id == id }
    }
    
    override fun getPlayers(): List<MfPlayer> {
        val data = loadData()
        return data.players.toList()
    }
    
    override fun upsert(player: MfPlayer): MfPlayer {
        val data = loadData()
        val existingIndex = data.players.indexOfFirst { it.id == player.id }
        
        if (existingIndex >= 0) {
            val existing = data.players[existingIndex]
            if (existing.version != player.version) {
                throw OptimisticLockingFailureException("Invalid version: ${player.version}")
            }
            val updated = player.copy(version = player.version + 1)
            data.players[existingIndex] = updated
            saveData(data)
            return updated
        } else {
            val newPlayer = player.copy(version = 1)
            data.players.add(newPlayer)
            saveData(data)
            return newPlayer
        }
    }
    
    override fun increaseOnlinePlayerPower(onlinePlayerIds: List<MfPlayerId>) {
        val data = loadData()
        val minPower = plugin.config.getDouble("players.minPower")
        val maxPower = plugin.config.getDouble("players.maxPower")
        val hoursToReachMax = plugin.config.getDouble("players.hoursToReachMaxPower")
        val timeIncrementHours = 0.25
        
        data.players.forEachIndexed { index, player ->
            if (player.id in onlinePlayerIds && player.power < maxPower) {
                val currentPower = player.power
                // Replicate the power increase formula from JooqMfPlayerRepository
                val normalizedPower = currentPower / maxPower
                val invNormalizedPower = -(normalizedPower - 1)
                val term1 = invNormalizedPower.pow(0.25) + 1
                val term2 = term1 * hoursToReachMax
                val term3 = (hoursToReachMax * 2) + timeIncrementHours - term2
                val term4 = term3 / hoursToReachMax - 1
                val term5 = term4.pow(4)
                val term6 = -term5 + 1
                val newPower = term6 * maxPower
                
                val clampedPower = max(minPower, min(maxPower, newPower))
                data.players[index] = player.copy(
                    power = clampedPower,
                    version = player.version + 1
                )
            }
        }
        
        saveData(data)
    }
    
    override fun decreaseOfflinePlayerPower(offlinePlayerIds: List<MfPlayerId>) {
        val data = loadData()
        val minPower = plugin.config.getDouble("players.minPower")
        val maxPower = plugin.config.getDouble("players.maxPower")
        val hoursToReachMin = plugin.config.getDouble("players.hoursToReachMinPower")
        val timeIncrementHours = 0.25
        
        data.players.forEachIndexed { index, player ->
            if (player.id in offlinePlayerIds && player.power > minPower) {
                val currentPower = player.power
                // Replicate the power decrease formula from JooqMfPlayerRepository
                val normalizedPower = currentPower / maxPower
                val term1 = normalizedPower.pow(0.25) + 1
                val term2 = term1 * hoursToReachMin
                val term3 = (hoursToReachMin * 2) - timeIncrementHours - term2
                val term4 = term3 / hoursToReachMin - 1
                val term5 = term4.pow(4)
                val term6 = -term5 + 1
                val newPower = term6 * maxPower
                
                val clampedPower = max(minPower, min(maxPower, newPower))
                data.players[index] = player.copy(
                    power = clampedPower,
                    version = player.version + 1
                )
            }
        }
        
        saveData(data)
    }
}
