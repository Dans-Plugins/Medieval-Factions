package com.dansplugins.factionsystem.interaction

import com.dansplugins.factionsystem.player.MfPlayerId

data class MfPlayerInteractionStatus(
    @get:JvmName("getPlayerId")
    val playerId: MfPlayerId,
    val interactionStatus: MfInteractionStatus
)
