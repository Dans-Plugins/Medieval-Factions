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

    private fun loadData(): LawData =
        storageManager.loadJsonData(fileName, "laws", gson, LawData::class.java) { LawData() }

    private fun saveData(data: LawData) {
        storageManager.writeJsonFile(fileName, data, null)
    }

    override fun getLaw(id: MfLawId): MfLaw? {
        val data = loadData()
        return data.laws.find { it.id == id }
    }

    override fun getLaw(factionId: MfFactionId, index: Int?): MfLaw? {
        val data = loadData()
        val factionLaws = data.laws.filter { it.factionId == factionId }.sortedBy { it.number }
        return if (index != null) {
            factionLaws.getOrNull(index)
        } else {
            factionLaws.firstOrNull()
        }
    }

    override fun getLaws(factionId: MfFactionId): List<MfLaw> {
        val data = loadData()
        return data.laws.filter { it.factionId == factionId }.sortedBy { it.number }
    }

    override fun upsert(law: MfLaw): MfLaw = storageManager.withFileLock(fileName) {
        val data = loadData()
        val existingIndex = data.laws.indexOfFirst { it.id == law.id }

        val updated = if (existingIndex >= 0) {
            val existing = data.laws[existingIndex]
            if (existing.version != law.version) {
                throw OptimisticLockingFailureException("Invalid version: ${law.version}")
            }
            law.copy(version = law.version + 1).also { data.laws[existingIndex] = it }
        } else {
            law.copy(version = 1).also { data.laws.add(it) }
        }
        saveData(data)
        updated
    }

    override fun delete(id: MfLawId) = storageManager.withFileLock(fileName) {
        val data = loadData()
        data.laws.removeIf { it.id == id }
        saveData(data)
    }

    override fun delete(law: MfLaw) {
        delete(law.id)
    }

    override fun move(law: MfLaw, number: Int) = storageManager.withFileLock(fileName) {
        val data = loadData()
        val existingIndex = data.laws.indexOfFirst { it.id == law.id }
        if (existingIndex >= 0) {
            val oldNumber = data.laws[existingIndex].number
            if (oldNumber != null) {
                // Shift the laws in between by one, as JooqMfLawRepository.move does, so that no two laws share a number.
                data.laws.replaceAll { other ->
                    val otherNumber = other.number
                    when {
                        other.id == law.id || other.factionId != law.factionId || otherNumber == null -> other
                        oldNumber > number && otherNumber in number until oldNumber -> other.copy(number = otherNumber + 1)
                        oldNumber < number && otherNumber in (oldNumber + 1)..number -> other.copy(number = otherNumber - 1)
                        else -> other
                    }
                }
            }
            data.laws[existingIndex] = law.copy(number = number, version = law.version + 1)
            saveData(data)
        }
    }
}
