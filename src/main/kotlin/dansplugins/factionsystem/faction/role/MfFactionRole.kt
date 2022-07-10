package dansplugins.factionsystem.faction.role

import dansplugins.factionsystem.faction.MfFaction
import dansplugins.factionsystem.faction.permission.MfFactionPermission

data class MfFactionRole(
    val id: MfFactionRoleId = MfFactionRoleId.generate(),
    val name: String,
    val permissions: Map<MfFactionPermission, Boolean?> = emptyMap()
) {
    fun hasPermission(faction: MfFaction, permission: MfFactionPermission) =
        permissions[permission]
            ?: faction.defaultPermissions[permission]
            ?: false
}