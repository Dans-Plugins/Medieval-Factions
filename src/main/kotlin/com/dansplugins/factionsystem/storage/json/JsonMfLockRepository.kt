package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.locks.MfLockRepository
import com.dansplugins.factionsystem.locks.MfLockedBlock
import com.dansplugins.factionsystem.locks.MfLockedBlockId
import com.dansplugins.factionsystem.locks.MfLockedEntity
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

    private fun loadData(): LockData =
        storageManager.loadJsonData(fileName, "locks", gson, LockData::class.java) { LockData() }

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

    override fun upsert(lockedBlock: MfLockedBlock): MfLockedBlock = storageManager.withFileLock(fileName) {
        val data = loadData()
        val existingIndex = data.locks.indexOfFirst { it.id == lockedBlock.id }

        val updated = if (existingIndex >= 0) {
            val existing = data.locks[existingIndex]
            if (existing.version != lockedBlock.version) {
                throw OptimisticLockingFailureException("Invalid version: ${lockedBlock.version}")
            }
            lockedBlock.copy(version = lockedBlock.version + 1).also { data.locks[existingIndex] = it }
        } else {
            lockedBlock.copy(version = 1).also { data.locks.add(it) }
        }
        saveData(data)
        updated
    }

    override fun delete(block: MfBlockPosition) = storageManager.withFileLock(fileName) {
        val data = loadData()
        data.locks.removeIf { it.block.worldId == block.worldId && it.block.x == block.x && it.block.y == block.y && it.block.z == block.z }
        saveData(data)
    }

    // --- locked entities (#1679), kept in their own file so locks.json keeps its existing shape ---

    private val entityFileName = "locked_entities.json"

    data class LockedEntityData(
        val lockedEntities: MutableList<MfLockedEntity> = mutableListOf()
    )

    private fun loadEntityData(): LockedEntityData =
        storageManager.loadJsonData(entityFileName, "locked entities", gson, LockedEntityData::class.java) { LockedEntityData() }

    override fun getLockedEntities(): List<MfLockedEntity> = loadEntityData().lockedEntities.toList()

    override fun upsertLockedEntity(lockedEntity: MfLockedEntity): MfLockedEntity = storageManager.withFileLock(entityFileName) {
        val data = loadEntityData()
        val existingIndex = data.lockedEntities.indexOfFirst { it.id == lockedEntity.id }
        val updated = if (existingIndex >= 0) {
            if (data.lockedEntities[existingIndex].version != lockedEntity.version) {
                throw OptimisticLockingFailureException("Invalid version: ${lockedEntity.version}")
            }
            lockedEntity.copy(version = lockedEntity.version + 1).also { data.lockedEntities[existingIndex] = it }
        } else {
            if (data.lockedEntities.any { it.entityId == lockedEntity.entityId }) {
                throw OptimisticLockingFailureException("Entity ${lockedEntity.entityId} is already locked")
            }
            lockedEntity.copy(version = 1).also { data.lockedEntities.add(it) }
        }
        storageManager.writeJsonFile(entityFileName, data, null)
        updated
    }

    override fun deleteLockedEntity(entityId: UUID) = storageManager.withFileLock(entityFileName) {
        val data = loadEntityData()
        if (data.lockedEntities.removeIf { it.entityId == entityId }) {
            storageManager.writeJsonFile(entityFileName, data, null)
        }
    }
}
