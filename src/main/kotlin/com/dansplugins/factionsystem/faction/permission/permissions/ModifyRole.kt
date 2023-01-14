package com.dansplugins.factionsystem.faction.permission.permissions

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissionType
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId

class ModifyRole(private val plugin: MedievalFactions) : MfFactionPermissionType() {
    override fun parse(name: String): MfFactionPermission? =
        if (name.matches(Regex("MODIFY_ROLE\\((.+)\\)"))) {
            Regex("MODIFY_ROLE\\((.+)\\)").find(name)
                ?.groupValues?.get(1)
                ?.let(::MfFactionRoleId)
                ?.let(::permissionFor)
        } else {
            null
        }

    override fun permissionsFor(factionId: MfFactionId, roleIds: List<MfFactionRoleId>): List<MfFactionPermission> {
        return roleIds.map(::permissionFor)
    }

    private fun permissionFor(roleId: MfFactionRoleId) = MfFactionPermission(
        "MODIFY_ROLE(${roleId.value})",
        { faction -> plugin.language["FactionPermissionModifyRole", faction.getRole(roleId)?.name ?: ""] },
        true
    )
}
