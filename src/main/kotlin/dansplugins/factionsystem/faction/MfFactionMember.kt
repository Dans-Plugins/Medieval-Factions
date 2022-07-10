package dansplugins.factionsystem.faction

import dansplugins.factionsystem.faction.role.MfFactionRole
import dansplugins.factionsystem.player.MfPlayer

data class MfFactionMember(
    val player: MfPlayer,
    val role: MfFactionRole
)

fun MfPlayer.withRole(role: MfFactionRole) = MfFactionMember(this, role)