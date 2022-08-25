package com.dansplugins.factionsystem.area

import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

data class MfPosition(
    val worldId: UUID,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
) {
    fun toBukkitLocation() = Bukkit.getWorld(worldId)?.let { world -> Location(world, x, y, z, yaw, pitch) }
}