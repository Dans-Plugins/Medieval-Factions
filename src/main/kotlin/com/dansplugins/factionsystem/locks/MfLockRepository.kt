package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.area.MfBlockPosition
import java.util.*

interface MfLockRepository {

    fun getLockedBlock(id: MfLockedBlockId): MfLockedBlock?
    fun getLockedBlock(worldId: UUID, x: Int, y: Int, z: Int): MfLockedBlock?
    fun getLockedBlocks(): List<MfLockedBlock>
    fun upsert(lockedBlock: MfLockedBlock): MfLockedBlock
    fun delete(block: MfBlockPosition)

    /** Every locked armour stand and item frame (#1679). */
    fun getLockedEntities(): List<MfLockedEntity>

    /** Stores [lockedEntity], rejecting a write at a stale version as [upsert] does for blocks. */
    fun upsertLockedEntity(lockedEntity: MfLockedEntity): MfLockedEntity

    /** Removes the lock, if any, on the entity with the UUID [entityId]. */
    fun deleteLockedEntity(entityId: UUID)
}
