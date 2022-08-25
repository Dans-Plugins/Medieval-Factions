package com.dansplugins.factionsystem.player

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.*

data class MfPlayer(
    val id: MfPlayerId,
    val version: Int = 0,
    val power: Int = 0
) {
    fun toBukkit() = Bukkit.getOfflinePlayer(UUID.fromString(id.value))

    companion object {
        fun fromBukkit(player: OfflinePlayer) = MfPlayer(MfPlayerId(player.uniqueId.toString()))
    }
}