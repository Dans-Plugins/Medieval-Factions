package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.*
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.player.MfPlayerId

data class MfFaction(
    private val plugin: com.dansplugins.factionsystem.MedievalFactions,
    val id: MfFactionId = MfFactionId.generate(),
    val version: Int = 0,
    val name: String,
    val description: String = "",
    val members: List<MfFactionMember> = emptyList(),
    val invites: List<MfFactionInvite> = emptyList(),
    val flags: MfFlagValues = plugin.flags.defaults(),
    val prefix: String? = name,
    val home: MfPosition? = null,
    val bonusPower: Int = 0,
    val autoclaim: Boolean = false,
    val roles: MfFactionRoles = MfFactionRoles.defaults(),
    val defaultPermissions: Map<MfFactionPermission, Boolean> = mapOf(
        ADD_LAW to false,
        REMOVE_LAW to false,
        LIST_LAWS to true,
        REQUEST_ALLIANCE to false,
        BREAK_ALLIANCE to false,
        TOGGLE_AUTOCLAIM to false,
        CHAT to true,
        CLAIM to false,
        UNCLAIM to false,
        DECLARE_INDEPENDENCE to false,
        SWEAR_FEALTY to false,
        GRANT_INDEPENDENCE to false,
        VASSALIZE to false,
        DECLARE_WAR to false,
        MAKE_PEACE to false,
        PROMOTE to false,
        DEMOTE to false,
        CHANGE_NAME to false,
        CHANGE_DESCRIPTION to false,
        CHANGE_PREFIX to false,
        DISBAND to false,
        SET_FLAG to false,
        VIEW_FLAGS to true,
        CREATE_GATE to false,
        REMOVE_GATE to false,
        RENAME_GATE to false,
        LIST_GATES to false,
        SET_HOME to false,
        GO_HOME to true,
        VIEW_INFO to true,
        VIEW_STATS to true,
        INVITE to true,
        INVOKE to false,
        KICK to false
    )
) {

    constructor(plugin: com.dansplugins.factionsystem.MedievalFactions, name: String) : this(
        plugin,
        MfFactionId.generate(),
        0,
        name,
        "",
        emptyList(),
        emptyList(),
        plugin.flags.defaults(),
        prefix = name
    )

    fun getRole(playerId: MfPlayerId): MfFactionRole? = members.singleOrNull { it.player.id == playerId }?.role
    fun getRole(roleId: MfFactionRoleId): MfFactionRole? = roles.getRole(roleId)
}