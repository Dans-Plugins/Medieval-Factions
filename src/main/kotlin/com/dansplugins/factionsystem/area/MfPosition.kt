package com.dansplugins.factionsystem.area

import org.bukkit.Bukkit
import org.bukkit.Location

data class MfPosition(
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
) {
    fun toBukkitLocation() = Bukkit.getWorld(worldName)?.let { world -> Location(world, x, y, z, yaw, pitch) }
}