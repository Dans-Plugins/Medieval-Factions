package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.Chunk
import java.util.*

data class MfClaimedChunk(
    val worldId: UUID,
    val x: Int,
    val z: Int,
    val factionId: MfFactionId
) {
    constructor(chunk: Chunk, factionId: MfFactionId) : this(chunk.world.uid, chunk.x, chunk.z, factionId)
}