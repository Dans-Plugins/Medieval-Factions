package com.dansplugins.factionsystem.faction.role

import com.dansplugins.factionsystem.chat.MfFactionChatChannel.*
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.ADD_LAW
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.BREAK_ALLIANCE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.CHANGE_DESCRIPTION
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.CHANGE_NAME
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.CHANGE_PREFIX
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.CHAT
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.CLAIM
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.CREATE_GATE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.DECLARE_INDEPENDENCE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.DECLARE_WAR
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.DEMOTE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.DISBAND
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.GO_HOME
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.GRANT_INDEPENDENCE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.INVITE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.INVOKE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.KICK
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.LIST_LAWS
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.LIST_MEMBERS
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.MAKE_PEACE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.MODIFY_ROLE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.PROMOTE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.REMOVE_GATE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.REMOVE_LAW
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.REQUEST_ALLIANCE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.SET_FLAG
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.SET_HOME
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.SET_MEMBER_ROLE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.SET_ROLE_PERMISSION
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.SWEAR_FEALTY
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.TOGGLE_AUTOCLAIM
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.UNCLAIM
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VASSALIZE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VIEW_FLAGS
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VIEW_INFO
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VIEW_ROLE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VIEW_STATS

data class MfFactionRoles(
    val defaultRoleId: MfFactionRoleId,
    val roles: List<MfFactionRole>
) : List<MfFactionRole> by roles {

    val default: MfFactionRole
        get() = getRole(defaultRoleId)!!

    companion object {
        fun defaults(flags: MfFlags): MfFactionRoles {
            val member = MfFactionRole(name = "Member")
            val officer = MfFactionRole(
                name = "Officer",
                permissions = buildMap {
                    put(REQUEST_ALLIANCE, true)
                    put(DECLARE_WAR, true)
                    put(MAKE_PEACE, true)
                    put(PROMOTE, true)
                    put(DEMOTE, true)
                    put(SET_HOME, true)
                    put(KICK, true)
                    put(VIEW_ROLE(member.id), true)
                    put(MODIFY_ROLE(member.id), true)
                    put(SET_MEMBER_ROLE(member.id), true)
                    put(SET_ROLE_PERMISSION(LIST_LAWS), true)
                    put(SET_ROLE_PERMISSION(CHAT(FACTION)), true)
                    put(SET_ROLE_PERMISSION(CHAT(VASSALS)), true)
                    put(SET_ROLE_PERMISSION(CHAT(ALLIES)), true)
                    put(SET_ROLE_PERMISSION(VIEW_FLAGS), true)
                    put(SET_ROLE_PERMISSION(GO_HOME), true)
                    put(SET_ROLE_PERMISSION(VIEW_INFO), true)
                    put(SET_ROLE_PERMISSION(VIEW_STATS), true)
                    put(SET_ROLE_PERMISSION(INVITE), true)
                    put(SET_ROLE_PERMISSION(VIEW_ROLE(member.id)), true)
                    put(SET_ROLE_PERMISSION(REQUEST_ALLIANCE), true)
                    put(SET_ROLE_PERMISSION(DECLARE_WAR), true)
                    put(SET_ROLE_PERMISSION(MAKE_PEACE), true)
                    put(SET_ROLE_PERMISSION(PROMOTE), true)
                    put(SET_ROLE_PERMISSION(DEMOTE), true)
                    put(SET_ROLE_PERMISSION(SET_HOME), true)
                    put(SET_ROLE_PERMISSION(KICK), true)
                    put(SET_ROLE_PERMISSION(MODIFY_ROLE(member.id)), true)
                    put(SET_ROLE_PERMISSION(LIST_MEMBERS), true)
                }
            )
            val ownerId = MfFactionRoleId.generate()
            val owner = MfFactionRole(
                id = ownerId,
                name = "Owner",
                permissions = buildMap {
                    put(ADD_LAW, true)
                    put(REMOVE_LAW, true)
                    put(LIST_LAWS, true)
                    put(REQUEST_ALLIANCE, true)
                    put(BREAK_ALLIANCE, true)
                    put(TOGGLE_AUTOCLAIM, true)
                    put(SET_ROLE_PERMISSION(CHAT(FACTION)), true)
                    put(SET_ROLE_PERMISSION(CHAT(VASSALS)), true)
                    put(SET_ROLE_PERMISSION(CHAT(ALLIES)), true)
                    put(CLAIM, true)
                    put(UNCLAIM, true)
                    put(DECLARE_INDEPENDENCE, true)
                    put(SWEAR_FEALTY, true)
                    put(GRANT_INDEPENDENCE, true)
                    put(VASSALIZE, true)
                    put(DECLARE_WAR, true)
                    put(MAKE_PEACE, true)
                    put(PROMOTE, true)
                    put(DEMOTE, true)
                    put(CHANGE_NAME, true)
                    put(CHANGE_DESCRIPTION, true)
                    put(CHANGE_PREFIX, true)
                    put(DISBAND, true)
                    flags.forEach { flag ->
                        put(SET_FLAG(flag), true)
                    }
                    put(VIEW_FLAGS, true)
                    put(CREATE_GATE, true)
                    put(REMOVE_GATE, true)
                    put(SET_HOME, true)
                    put(GO_HOME, true)
                    put(VIEW_INFO, true)
                    put(VIEW_STATS, true)
                    put(INVITE, true)
                    put(INVOKE, true)
                    put(KICK, true)
                    put(VIEW_ROLE(member.id), true)
                    put(VIEW_ROLE(officer.id), true)
                    put(VIEW_ROLE(ownerId), true)
                    put(MODIFY_ROLE(member.id), true)
                    put(MODIFY_ROLE(officer.id), true)
                    put(MODIFY_ROLE(ownerId), true)
                    put(SET_MEMBER_ROLE(member.id), true)
                    put(SET_MEMBER_ROLE(officer.id), true)
                    put(SET_MEMBER_ROLE(ownerId), true)
                    put(LIST_MEMBERS, true)

                    putAll(MfFactionPermission.values(flags, listOf(member.id, officer.id, ownerId)).map { permission -> SET_ROLE_PERMISSION(permission) to true })
                }
            )
            return MfFactionRoles(member.id, listOf(owner, officer, member))
        }
    }
    fun getRole(roleId: MfFactionRoleId) = roles.singleOrNull { it.id.value == roleId.value }
    fun getRole(name: String) = getRole(MfFactionRoleId(name)) ?: roles.singleOrNull { it.name.equals(name, ignoreCase = true) }
}