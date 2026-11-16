package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.lockedarea.MfLockedArea
import com.dansplugins.factionsystem.lockedarea.MfLockedAreaId
import com.dansplugins.factionsystem.lockedarea.MfLockedAreaRepository
import com.google.gson.Gson

/** Locked areas (#1393) in `locked_areas.json`; a server without the file has none. */
class JsonMfLockedAreaRepository(private val storageManager: JsonStorageManager) : MfLockedAreaRepository {

    private val fileName = "locked_areas.json"
    private val gson = Gson()

    data class LockedAreaData(val lockedAreas: MutableList<MfLockedArea> = mutableListOf())

    private fun loadData(): LockedAreaData =
        storageManager.loadJsonData(fileName, "locked areas", gson, LockedAreaData::class.java) { LockedAreaData() }

    override fun getLockedAreas(): List<MfLockedArea> = loadData().lockedAreas.toList()

    override fun upsert(lockedArea: MfLockedArea): MfLockedArea = storageManager.withFileLock(fileName) {
        val data = loadData()
        val index = data.lockedAreas.indexOfFirst { it.id == lockedArea.id }
        val updated = if (index >= 0) {
            if (data.lockedAreas[index].version != lockedArea.version) {
                throw OptimisticLockingFailureException("Invalid version: ${lockedArea.version}")
            }
            lockedArea.copy(version = lockedArea.version + 1).also { data.lockedAreas[index] = it }
        } else {
            lockedArea.copy(version = 1).also { data.lockedAreas.add(it) }
        }
        storageManager.writeJsonFile(fileName, data, null)
        updated
    }

    override fun delete(id: MfLockedAreaId) = storageManager.withFileLock(fileName) {
        val data = loadData()
        if (data.lockedAreas.removeIf { it.id == id }) storageManager.writeJsonFile(fileName, data, null)
    }
}
