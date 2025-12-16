package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfFactionChatChannel
import org.bukkit.OfflinePlayer

data class MfPlayer(
    @get:JvmName("getId")
    val id: MfPlayerId,
    val version: Int = 0,
    val name: String? = null,
    val power: Double = 0.0,
    val powerAtLogout: Double = 0.0,
    val isBypassEnabled: Boolean = false,
    val chatChannel: MfFactionChatChannel? = null
) {
    fun toBukkit() = id.toBukkitPlayer()

    constructor(
        plugin: MedievalFactions,
        id: MfPlayerId,
        name: String?
    ) : this(
        id,
        name = name,
        power = plugin.config.getDouble("players.initialPower"),
        powerAtLogout = plugin.config.getDouble("players.initialPower")
    )

    constructor(
        player: OfflinePlayer,
        version: Int = 0,
        power: Double = 0.0,
        powerOnLogout: Double = power,
        isBypassEnabled: Boolean = false,
        chatChannel: MfFactionChatChannel? = null
    ) : this(
        MfPlayerId.fromBukkitPlayer(player),
        version,
        player.name,
        power,
        powerOnLogout,
        isBypassEnabled,
        chatChannel
    )

    constructor(
        plugin: MedievalFactions,
        player: OfflinePlayer
    ) : this(
        MfPlayerId.fromBukkitPlayer(player),
        name = player.name,
        power = plugin.config.getDouble("players.initialPower"),
        powerAtLogout = plugin.config.getDouble("players.initialPower")
    )
}
