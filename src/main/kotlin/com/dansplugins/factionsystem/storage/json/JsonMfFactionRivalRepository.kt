package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.relationship.rival.MfFactionRival
import com.dansplugins.factionsystem.relationship.rival.MfFactionRivalRepository
import com.google.gson.Gson
import java.time.Instant

/**
 * Rivals in their own file, `rivals.json`, which no version without rivals reads or writes. A storage folder from
 * before rivals existed has no such file and loads with no rivals.
 */
class JsonMfFactionRivalRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfFactionRivalRepository {

    private val fileName = "rivals.json"
    private val gson: Gson = Gson()

    data class RivalDto(val factionId: String = "", val targetId: String = "", val createdAt: Long = 0)
    data class RivalData(val rivals: MutableList<RivalDto> = mutableListOf())

    private fun loadData(): RivalData =
        storageManager.loadJsonData(fileName, "rivals", gson, RivalData::class.java) { RivalData() }

    override fun getRivals(): List<MfFactionRival> = loadData().rivals.map { it.toDomain() }

    override fun insert(rival: MfFactionRival): MfFactionRival = storageManager.withFileLock(fileName) {
        val data = loadData()
        val existing = data.rivals.firstOrNull { it.factionId == rival.factionId.value && it.targetId == rival.targetId.value }
        if (existing != null) return@withFileLock existing.toDomain()
        data.rivals.add(RivalDto(rival.factionId.value, rival.targetId.value, rival.createdAt.toEpochMilli()))
        storageManager.writeJsonFile(fileName, data, null)
        rival
    }

    override fun delete(factionId: MfFactionId, targetId: MfFactionId) = storageManager.withFileLock(fileName) {
        val data = loadData()
        if (data.rivals.removeIf { it.factionId == factionId.value && it.targetId == targetId.value }) {
            storageManager.writeJsonFile(fileName, data, null)
        }
    }

    private fun RivalDto.toDomain() = MfFactionRival(MfFactionId(factionId), MfFactionId(targetId), Instant.ofEpochMilli(createdAt))
}
