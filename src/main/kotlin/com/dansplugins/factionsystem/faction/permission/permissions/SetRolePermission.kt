package com.dansplugins.factionsystem.faction.permission.permissions

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissionType
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId

class SetRolePermission(private val plugin: MedievalFactions) : MfFactionPermissionType() {
    override fun parse(name: String): MfFactionPermission? =
        if (name.matches(Regex("SET_ROLE_PERMISSION\\((.+)\\)"))) {
            Regex("SET_ROLE_PERMISSION\\((.+)\\)").find(name)
                ?.groupValues?.get(1)
                ?.let(plugin.factionPermissions::parse)
                ?.let(::permissionFor)
        } else {
            null
        }

    override fun permissionsFor(factionId: MfFactionId, roleIds: List<MfFactionRoleId>): List<MfFactionPermission> =
        plugin.factionPermissions.permissionTypes
            .filter { it !is SetRolePermission }
            .flatMap { it.permissionsFor(factionId, roleIds) }
            .map(::permissionFor)

    private fun permissionFor(permission: MfFactionPermission) = MfFactionPermission(
        "SET_ROLE_PERMISSION(${permission.name})",
        { faction -> plugin.language["FactionPermissionSetRolePermission", permission.translate(faction)] },
        false
    )
}
