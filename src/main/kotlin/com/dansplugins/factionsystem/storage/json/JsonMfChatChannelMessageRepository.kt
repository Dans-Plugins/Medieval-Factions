package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfChatChannelMessage
import com.dansplugins.factionsystem.chat.MfChatChannelMessageRepository
import com.dansplugins.factionsystem.faction.MfFactionId
import com.google.gson.Gson

class JsonMfChatChannelMessageRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfChatChannelMessageRepository {

    private val fileName = "chat_messages.json"
    private val gson: Gson = Gson()

    data class ChatMessageData(
        val messages: MutableList<MfChatChannelMessage> = mutableListOf()
    )

    private fun loadData(): ChatMessageData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, ChatMessageData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse chat messages JSON: ${e.message}")
                ChatMessageData()
            }
        } else {
            ChatMessageData()
        }
    }

    private fun saveData(data: ChatMessageData) {
        storageManager.writeJsonFile(fileName, data, null)
    }

    override fun insert(message: MfChatChannelMessage) {
        val data = loadData()
        data.messages.add(message)
        
        // Limit messages per faction to prevent unbounded growth
        val factionId = message.factionId
        val factionMessages = data.messages.filter { it.factionId == factionId }
        if (factionMessages.size > maxMessagesPerFaction) {
            // Remove oldest messages for this faction
            val toRemove = factionMessages.sortedBy { it.timestamp }.take(factionMessages.size - maxMessagesPerFaction)
            data.messages.removeAll(toRemove.toSet())
        }
        
        saveData(data)
    }

    override fun getChatChannelMessages(factionId: MfFactionId): List<MfChatChannelMessage> {
        val data = loadData()
        return data.messages.filter { it.factionId == factionId }.sortedBy { it.timestamp }
    }

    override fun getChatChannelMessages(factionId: MfFactionId, limit: Int, offset: Int): List<MfChatChannelMessage> {
        val data = loadData()
        return data.messages
            .filter { it.factionId == factionId }
            .sortedBy { it.timestamp }
            .drop(offset)
            .take(limit)
    }

    override fun getChatChannelMessageCount(factionId: MfFactionId): Int {
        val data = loadData()
        return data.messages.count { it.factionId == factionId }
    }
}
