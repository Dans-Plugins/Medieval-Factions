package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.area.MfBlockPosition
import java.util.*

interface MfLockRepository {

    fun getLockedBlock(id: MfLockedBlockId): MfLockedBlock?
    fun getLockedBlock(worldId: UUID, x: Int, y: Int, z: Int): MfLockedBlock?
    fun getLockedBlocks(): List<MfLockedBlock>
    fun upsert(lockedBlock: MfLockedBlock): MfLockedBlock
    fun delete(block: MfBlockPosition)
}
