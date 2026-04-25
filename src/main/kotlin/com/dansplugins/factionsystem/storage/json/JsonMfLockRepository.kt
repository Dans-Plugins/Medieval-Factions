package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.locks.MfLockRepository
import com.dansplugins.factionsystem.locks.MfLockedBlock
import com.dansplugins.factionsystem.locks.MfLockedBlockId
import com.google.gson.Gson
import java.util.*

class JsonMfLockRepository(
    private val plugin: MedievalFactions,
    private val storageManager: JsonStorageManager
) : MfLockRepository {

    private val fileName = "locks.json"
    private val gson: Gson = Gson()

    data class LockData(
        val locks: MutableList<MfLockedBlock> = mutableListOf()
    )

    private fun loadData(): LockData {
        val json = storageManager.readJsonFileAsString(fileName)
        return if (json != null) {
            try {
                gson.fromJson(json, LockData::class.java)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse locks JSON: ${e.message}")
                LockData()
            }
        } else {
            LockData()
        }
    }

    private fun saveData(data: LockData) {
        storageManager.writeJsonFile(fileName, data, null)
    }

    override fun getLockedBlock(id: MfLockedBlockId): MfLockedBlock? {
        val data = loadData()
        return data.locks.find { it.id == id }
    }

    override fun getLockedBlock(worldId: UUID, x: Int, y: Int, z: Int): MfLockedBlock? {
        val data = loadData()
        return data.locks.find { it.block.worldId == worldId && it.block.x == x && it.block.y == y && it.block.z == z }
    }

    override fun getLockedBlocks(): List<MfLockedBlock> {
        val data = loadData()
        return data.locks.toList()
    }

    override fun upsert(lockedBlock: MfLockedBlock): MfLockedBlock {
        val data = loadData()
        val existingIndex = data.locks.indexOfFirst { it.id == lockedBlock.id }

        if (existingIndex >= 0) {
            val existing = data.locks[existingIndex]
            if (existing.version != lockedBlock.version) {
                throw OptimisticLockingFailureException("Invalid version: ${lockedBlock.version}")
            }
            val updated = lockedBlock.copy(version = lockedBlock.version + 1)
            data.locks[existingIndex] = updated
            saveData(data)
            return updated
        } else {
            val newLock = lockedBlock.copy(version = 1)
            data.locks.add(newLock)
            saveData(data)
            return newLock
        }
    }

    override fun delete(block: MfBlockPosition) {
        val data = loadData()
        data.locks.removeIf { it.block.worldId == block.worldId && it.block.x == block.x && it.block.y == block.y && it.block.z == block.z }
        saveData(data)
    }
}
