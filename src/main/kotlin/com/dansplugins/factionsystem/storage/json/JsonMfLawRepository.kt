package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.law.MfLaw
import com.dansplugins.factionsystem.law.MfLawId
import com.dansplugins.factionsystem.law.MfLawRepository
import com.google.gson.Gson

class JsonMfLawRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfLawRepository {

    private val fileName = "laws.json"
    private val gson: Gson = Gson()

    data class LawData(
        val laws: MutableList<MfLaw> = mutableListOf()
    )

    private fun loadData(): LawData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, LawData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse laws JSON: ${e.message}")
                LawData()
            }
        } else {
            LawData()
        }
    }

    private fun saveData(data: LawData) {
        storageManager.writeJsonFile(fileName, data, null)
    }

    override fun getLaw(id: MfLawId): MfLaw? {
        val data = loadData()
        return data.laws.find { it.id == id }
    }

    override fun getLaw(factionId: MfFactionId, index: Int?): MfLaw? {
        val data = loadData()
        val factionLaws = data.laws.filter { it.factionId == factionId }.sortedBy { it.index }
        return if (index != null) {
            factionLaws.getOrNull(index)
        } else {
            factionLaws.firstOrNull()
        }
    }

    override fun getLaws(factionId: MfFactionId): List<MfLaw> {
        val data = loadData()
        return data.laws.filter { it.factionId == factionId }.sortedBy { it.index }
    }

    override fun upsert(law: MfLaw): MfLaw {
        val data = loadData()
        val existingIndex = data.laws.indexOfFirst { it.id == law.id }

        if (existingIndex >= 0) {
            val existing = data.laws[existingIndex]
            if (existing.version != law.version) {
                throw OptimisticLockingFailureException("Invalid version: ${law.version}")
            }
            val updated = law.copy(version = law.version + 1)
            data.laws[existingIndex] = updated
            saveData(data)
            return updated
        } else {
            val newLaw = law.copy(version = 1)
            data.laws.add(newLaw)
            saveData(data)
            return newLaw
        }
    }

    override fun delete(id: MfLawId) {
        val data = loadData()
        data.laws.removeIf { it.id == id }
        saveData(data)
    }

    override fun delete(law: MfLaw) {
        delete(law.id)
    }

    override fun move(law: MfLaw, number: Int) {
        val data = loadData()
        val existingIndex = data.laws.indexOfFirst { it.id == law.id }
        if (existingIndex >= 0) {
            val updated = law.copy(index = number, version = law.version + 1)
            data.laws[existingIndex] = updated
            saveData(data)
        }
    }
}
