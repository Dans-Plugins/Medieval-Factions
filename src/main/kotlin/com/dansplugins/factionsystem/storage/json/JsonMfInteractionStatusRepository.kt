package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.interaction.MfInteractionStatus
import com.dansplugins.factionsystem.interaction.MfInteractionStatusRepository
import com.dansplugins.factionsystem.player.MfPlayerId
import com.google.gson.Gson

class JsonMfInteractionStatusRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfInteractionStatusRepository {

    private val fileName = "interaction_statuses.json"
    private val gson: Gson = Gson()

    data class InteractionStatusData(
        val statuses: MutableMap<String, MfInteractionStatus?> = mutableMapOf()
    )

    private fun loadData(): InteractionStatusData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, InteractionStatusData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse interaction statuses JSON: ${e.message}")
                InteractionStatusData()
            }
        } else {
            InteractionStatusData()
        }
    }

    private fun saveData(data: InteractionStatusData) {
        storageManager.writeJsonFile(fileName, data, null)
    }

    override fun getInteractionStatus(playerId: MfPlayerId): MfInteractionStatus? {
        val data = loadData()
        return data.statuses[playerId.value]
    }

    override fun setInteractionStatus(playerId: MfPlayerId, status: MfInteractionStatus?) {
        val data = loadData()
        if (status == null) {
            data.statuses.remove(playerId.value)
        } else {
            data.statuses[playerId.value] = status
        }
        saveData(data)
    }
}
