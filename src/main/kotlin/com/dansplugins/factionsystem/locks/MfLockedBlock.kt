package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.player.MfPlayerId

data class MfLockedBlock(
    val id: MfLockedBlockId = MfLockedBlockId.generate(),
    val version: Int = 0,
    val block: MfBlockPosition,
    val chunkX: Int,
    val chunkZ: Int,
    val playerId: MfPlayerId,
    val accessors: List<MfPlayerId>
)