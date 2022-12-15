package com.dansplugins.factionsystem.area

import org.bukkit.Bukkit
import org.bukkit.Chunk
import java.util.*

data class MfChunkPosition(
    val worldId: UUID,
    val x: Int,
    val z: Int
) {
    companion object {
        fun fromBukkit(chunk: Chunk) = MfChunkPosition(
            chunk.world.uid,
            chunk.x,
            chunk.z
        )
    }

    fun toBukkit() = Bukkit.getWorld(worldId)?.getChunkAt(x, z)
}
