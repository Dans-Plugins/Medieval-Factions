package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.player.MfPlayerId

data class MfGateCreationContext(
    val playerId: MfPlayerId,
    val version: Int = 0,
    val position1: MfBlockPosition? = null,
    val position2: MfBlockPosition? = null,
    val trigger: MfBlockPosition? = null
)
