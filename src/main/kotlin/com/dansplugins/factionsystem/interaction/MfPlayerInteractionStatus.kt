package com.dansplugins.factionsystem.interaction

import com.dansplugins.factionsystem.player.MfPlayerId

data class MfPlayerInteractionStatus(
    val playerId: MfPlayerId,
    val interactionStatus: MfInteractionStatus
)