package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.player.MfPlayer

data class MfFactionMember(
    val player: MfPlayer,
    val role: MfFactionRole
)

fun MfPlayer.withRole(role: MfFactionRole) = MfFactionMember(this, role)