package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.gate.MfGateStatus.CLOSED
import org.bukkit.Material

data class MfGate(
    val id: MfGateId = MfGateId.generate(),
    val version: Int = 0,
    val factionId: MfFactionId,
    val area: MfCuboidArea,
    val trigger: MfBlockPosition,
    val material: Material,
    val status: MfGateStatus = CLOSED
)