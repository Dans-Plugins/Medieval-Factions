package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.OfflinePlayer

data class MfPlayer(
    val id: MfPlayerId,
    val version: Int = 0,
    val power: Int = 0,
    val isBypassEnabled: Boolean = false
) {
    fun toBukkit() = id.toBukkitPlayer()

    constructor(
        plugin: MedievalFactions,
        id: MfPlayerId
    ): this(
        id,
        power = plugin.config.getInt("players.initialPower")
    )

    constructor(
        player: OfflinePlayer,
        version: Int = 0,
        power: Int = 0,
        isBypassEnabled: Boolean
    ): this(
        MfPlayerId.fromBukkitPlayer(player),
        version,
        power,
        isBypassEnabled
    )

    constructor(
        plugin: MedievalFactions,
        player: OfflinePlayer
    ): this(
        MfPlayerId.fromBukkitPlayer(player),
        power = plugin.config.getInt("players.initialPower")
    )

}