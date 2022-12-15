package com.dansplugins.factionsystem.chat

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayerId
import java.time.Instant

data class MfChatChannelMessage(
    val timestamp: Instant,
    @get:JvmName("getPlayerId")
    val playerId: MfPlayerId,
    @get:JvmName("getFactionId")
    val factionId: MfFactionId,
    val chatChannel: MfFactionChatChannel,
    val message: String
)
