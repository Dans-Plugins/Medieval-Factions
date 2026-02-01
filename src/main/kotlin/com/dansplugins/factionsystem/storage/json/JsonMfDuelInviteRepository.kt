package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.duel.MfDuelInvite
import com.dansplugins.factionsystem.duel.MfDuelInviteRepository
import com.dansplugins.factionsystem.player.MfPlayerId
import com.google.gson.Gson

class JsonMfDuelInviteRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfDuelInviteRepository {

    private val fileName = "duel_invites.json"
    private val gson: Gson = Gson()

    data class DuelInviteData(
        val invites: MutableList<MfDuelInvite> = mutableListOf()
    )

    private fun loadData(): DuelInviteData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, DuelInviteData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse duel invites JSON: ${e.message}")
                DuelInviteData()
            }
        } else {
            DuelInviteData()
        }
    }

    private fun saveData(data: DuelInviteData) {
        storageManager.writeJsonFile(fileName, data, null)
    }

    override fun getInvite(inviter: MfPlayerId, invitee: MfPlayerId): MfDuelInvite? {
        val data = loadData()
        return data.invites.find { it.inviterId == inviter && it.inviteeId == invitee }
    }

    override fun getInvites(): List<MfDuelInvite> {
        val data = loadData()
        return data.invites.toList()
    }

    override fun upsert(invite: MfDuelInvite): MfDuelInvite {
        val data = loadData()
        data.invites.removeIf { it.inviterId == invite.inviterId && it.inviteeId == invite.inviteeId }
        data.invites.add(invite)
        saveData(data)
        return invite
    }

    override fun deleteInvite(inviter: MfPlayerId, invitee: MfPlayerId) {
        val data = loadData()
        data.invites.removeIf { it.inviterId == inviter && it.inviteeId == invitee }
        saveData(data)
    }
}
