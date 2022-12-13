package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId

data class MfFactionMember(
    @get:JvmName("getPlayerId")
    val playerId: MfPlayerId,
    val role: MfFactionRole
)

fun MfPlayer.withRole(role: MfFactionRole) = MfFactionMember(this.id, role)
