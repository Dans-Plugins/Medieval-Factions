package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.OfflinePlayer

data class MfPlayer(
    val id: MfPlayerId,
    val version: Int = 0,
    val power: Int = 0
) {
    fun toBukkit() = id.toBukkitPlayer()

    constructor(plugin: MedievalFactions, id: MfPlayerId): this(id, power = plugin.config.getInt("players.initialPower"))
    constructor(player: OfflinePlayer, version: Int = 0, power: Int = 0): this(MfPlayerId.fromBukkitPlayer(player), version, power)
    constructor(plugin: MedievalFactions, player: OfflinePlayer): this(MfPlayerId.fromBukkitPlayer(player), power = plugin.config.getInt("players.initialPower"))

}