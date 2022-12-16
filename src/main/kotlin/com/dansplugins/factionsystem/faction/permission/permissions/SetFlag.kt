package com.dansplugins.factionsystem.faction.permission.permissions

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissionType
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId

class SetFlag(private val plugin: MedievalFactions) : MfFactionPermissionType() {
    override fun parse(name: String): MfFactionPermission? =
        if (name.matches(Regex("SET_FLAG\\((.+)\\)"))) {
            Regex("SET_FLAG\\((.+)\\)").find(name)
                ?.groupValues?.get(1)
                ?.let<String, MfFlag<out Any>?>(plugin.flags::get)
                ?.let(::permissionFor)
        } else {
            null
        }

    override fun permissionsFor(factionId: MfFactionId, roleIds: List<MfFactionRoleId>): List<MfFactionPermission> {
        return plugin.flags.map(::permissionFor)
    }

    private fun permissionFor(flag: MfFlag<out Any>) = MfFactionPermission(
        "SET_FLAG(${flag.name})",
        plugin.language["FactionPermissionSetFlag", flag.name],
        false
    )
}
