package dansplugins.factionsystem.area

import org.bukkit.Bukkit

data class MfBlockPosition(
    val worldName: String,
    val x: Int,
    val y: Int,
    val z: Int
) {
    fun toBukkitBlock() = Bukkit.getWorld(worldName)?.getBlockAt(x, y, z)
    fun toBukkitLocation() = toBukkitBlock()?.location
}