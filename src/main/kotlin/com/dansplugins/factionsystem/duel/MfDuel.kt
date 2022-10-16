package com.dansplugins.factionsystem.duel

import com.dansplugins.factionsystem.player.MfPlayerId
import java.time.Instant

data class MfDuel(
    val id: MfDuelId = MfDuelId.generate(),
    val version: Int = 0,
    val challengerId: MfPlayerId,
    val challengedId: MfPlayerId,
    val challengerHealth: Double,
    val challengedHealth: Double,
    val endTime: Instant
)