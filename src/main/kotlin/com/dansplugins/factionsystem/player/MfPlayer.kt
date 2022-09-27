package com.dansplugins.factionsystem.player

import org.bukkit.OfflinePlayer

data class MfPlayer(
    val id: MfPlayerId,
    val version: Int = 0,
    val power: Int = 0
) {
    fun toBukkit() = id.toBukkitPlayer()

    companion object {
        fun fromBukkit(player: OfflinePlayer) = MfPlayer(MfPlayerId(player.uniqueId.toString()))
    }
}