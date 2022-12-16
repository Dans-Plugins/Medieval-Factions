package com.dansplugins.factionsystem.area

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import java.util.*

data class MfBlockPosition(
    val worldId: UUID,
    val x: Int,
    val y: Int,
    val z: Int
) {
    fun toBukkitBlock() = Bukkit.getWorld(worldId)?.getBlockAt(x, y, z)
    fun toBukkitLocation() = toBukkitBlock()?.location

    companion object {
        fun fromBukkitBlock(block: Block) = MfBlockPosition(
            block.world.uid,
            block.x,
            block.y,
            block.z
        )

        fun fromBukkitLocation(location: Location) = location.world?.getBlockAt(location)?.let { fromBukkitBlock(it) }
    }
}
