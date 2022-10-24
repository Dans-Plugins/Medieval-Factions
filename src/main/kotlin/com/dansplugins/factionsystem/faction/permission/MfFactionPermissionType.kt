package com.dansplugins.factionsystem.faction.permission

import com.dansplugins.factionsystem.faction.role.MfFactionRoleId

abstract class MfFactionPermissionType {
    abstract fun parse(name: String): MfFactionPermission?
    abstract fun permissionsFor(roleIds: List<MfFactionRoleId>): List<MfFactionPermission>
}