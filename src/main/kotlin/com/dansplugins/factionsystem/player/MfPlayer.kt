package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfFactionChatChannel
import org.bukkit.OfflinePlayer

data class MfPlayer(
    val id: MfPlayerId,
    val version: Int = 0,
    val power: Int = 0,
    val isBypassEnabled: Boolean = false,
    val chatChannel: MfFactionChatChannel? = null
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
        isBypassEnabled: Boolean = false,
        chatChannel: MfFactionChatChannel? = null
    ): this(
        MfPlayerId.fromBukkitPlayer(player),
        version,
        power,
        isBypassEnabled,
        chatChannel
    )

    constructor(
        plugin: MedievalFactions,
        player: OfflinePlayer
    ): this(
        MfPlayerId.fromBukkitPlayer(player),
        power = plugin.config.getInt("players.initialPower")
    )

}