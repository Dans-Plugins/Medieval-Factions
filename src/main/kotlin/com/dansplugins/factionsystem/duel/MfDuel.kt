package com.dansplugins.factionsystem.duel

import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.player.MfPlayerId
import java.time.Instant

data class MfDuel(
    @get:JvmName("getId")
    val id: MfDuelId = MfDuelId.generate(),
    val version: Int = 0,
    @get:JvmName("getChallengerId")
    val challengerId: MfPlayerId,
    @get:JvmName("getChallengedId")
    val challengedId: MfPlayerId,
    val challengerHealth: Double,
    val challengedHealth: Double,
    val endTime: Instant,
    val challengerLocation: MfPosition?,
    val challengedLocation: MfPosition?
)
