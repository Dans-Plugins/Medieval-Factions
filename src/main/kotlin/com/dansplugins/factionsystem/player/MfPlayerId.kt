package com.dansplugins.factionsystem.player

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.*

@JvmInline
value class MfPlayerId(val value: String) {
    companion object {
        fun fromBukkitPlayer(player: OfflinePlayer) = MfPlayerId(player.uniqueId.toString())
    }

    fun toBukkitPlayer() = Bukkit.getOfflinePlayer(UUID.fromString(value))
}
