package com.dansplugins.factionsystem.duel

import com.dansplugins.factionsystem.player.MfPlayerId

data class MfDuelInvite(
    val inviterId: MfPlayerId,
    val inviteeId: MfPlayerId
)
