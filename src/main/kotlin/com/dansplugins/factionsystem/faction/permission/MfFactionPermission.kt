package com.dansplugins.factionsystem.faction.permission

import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.lang.Language

class MfFactionPermission(
    val name: String,
    val translate: (language: Language, faction: MfFaction) -> String
) {

    constructor(name: String, translationKey: String): this(name, { language -> language[translationKey] })
    constructor(name: String, translate: (language: Language) -> String): this(name, { language, _ -> translate(language) })

    override fun toString() = name
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MfFactionPermission

        if (name != other.name) return false

        return true
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }


    companion object {
        val ADD_LAW = MfFactionPermission("ADD_LAW", "FactionPermissionAddLaw")
        val REMOVE_LAW = MfFactionPermission("REMOVE_LAW", "FactionPermissionRemoveLaw")
        val LIST_LAWS = MfFactionPermission("LIST_LAWS", "FactionPermissionListLaws")
        val REQUEST_ALLIANCE = MfFactionPermission("REQUEST_ALLIANCE", "FactionPermissionRequestAlliance")
        val BREAK_ALLIANCE = MfFactionPermission("BREAK_ALLIANCE", "FactionPermissionBreakAlliance")
        val TOGGLE_AUTOCLAIM = MfFactionPermission("TOGGLE_AUTOCLAIM", "FactionPermissionToggleAutoclaim")
        val CHAT = MfFactionPermission("CHAT", "FactionPermissionChat")
        val CLAIM = MfFactionPermission("CLAIM", "FactionPermissionClaim")
        val UNCLAIM = MfFactionPermission("UNCLAIM", "FactionPermissionUnclaim")
        val DECLARE_INDEPENDENCE = MfFactionPermission("DECLARE_INDEPENDENCE", "FactionPermissionDeclareIndependence")
        val SWEAR_FEALTY = MfFactionPermission("SWEAR_FEALTY", "FactionPermissionSwearFealty")
        val GRANT_INDEPENDENCE = MfFactionPermission("GRANT_INDEPENDENCE", "FactionPermissionGrantIndependence")
        val VASSALIZE = MfFactionPermission("VASSALIZE", "FactionPermissionVassalize")
        val DECLARE_WAR = MfFactionPermission("DECLARE_WAR", "FactionPermissionDeclareWar")
        val MAKE_PEACE = MfFactionPermission("MAKE_PEACE", "FactionPermissionMakePeace")
        val PROMOTE = MfFactionPermission("PROMOTE", "FactionPermissionPromote")
        val DEMOTE = MfFactionPermission("DEMOTE", "FactionPermissionDemote")
        val CHANGE_NAME = MfFactionPermission("CHANGE_NAME", "FactionPermissionChangeName")
        val CHANGE_DESCRIPTION = MfFactionPermission("CHANGE_DESCRIPTION", "FactionPermissionChangeDescription")
        val CHANGE_PREFIX = MfFactionPermission("CHANGE_PREFIX", "FactionPermissionChangePrefix")
        val DISBAND = MfFactionPermission("DISBAND", "FactionPermissionDisband")
        val SET_FLAG = { flag: MfFlag<*> -> MfFactionPermission("SET_FLAG(${flag.name})") { language -> language["FactionPermissionSetFlag", flag.name] } }
        val VIEW_FLAGS = MfFactionPermission("VIEW_FLAGS", "FactionPermissionViewFlags")
        val CREATE_GATE = MfFactionPermission("CREATE_GATE", "FactionPermissionCreateGate")
        val REMOVE_GATE = MfFactionPermission("REMOVE_GATE", "FactionPermissionRemoveGate")
        val RENAME_GATE = MfFactionPermission("RENAME_GATE", "FactionPermissionRenameGate")
        val LIST_GATES = MfFactionPermission("LIST_GATES", "FactionPermissionListGates")
        val SET_HOME = MfFactionPermission("SET_HOME", "FactionPermissionSetHome")
        val GO_HOME = MfFactionPermission("GO_HOME", "FactionPermissionGoHome")
        val VIEW_INFO = MfFactionPermission("VIEW_INFO", "FactionPermissionViewInfo")
        val VIEW_STATS = MfFactionPermission("VIEW_STATS", "FactionPermissionViewStats")
        val INVITE = MfFactionPermission("INVITE", "FactionPermissionInvite")
        val INVOKE = MfFactionPermission("INVOKE", "FactionPermissionInvoke")
        val KICK = MfFactionPermission("KICK", "FactionPermissionKick")
        val VIEW_ROLE = { roleId: MfFactionRoleId -> MfFactionPermission("VIEW_ROLE(${roleId.value})") { language, faction -> language["FactionPermissionViewRole", faction.getRole(roleId)?.name ?: ""] } }
        val SET_ROLE_PERMISSION = { permission: MfFactionPermission -> MfFactionPermission("SET_ROLE_PERMISSION(${permission.name})") { language, faction -> language["FactionPermissionSetRolePermission", permission.translate(language, faction)] } }
        val MODIFY_ROLE = { roleId: MfFactionRoleId -> MfFactionPermission("MODIFY_ROLE(${roleId.value})") { language, faction -> language["FactionPermissionModifyRole", faction.getRole(roleId)?.name ?: ""] } }
        val SET_MEMBER_ROLE = { roleId: MfFactionRoleId -> MfFactionPermission("SET_MEMBER_ROLE(${roleId.value})") { language, faction -> language["FactionPermissionSetMemberRole", faction.getRole(roleId)?.name ?: ""]} }
        val LIST_ROLES = MfFactionPermission("LIST_ROLES", "FactionPermissionListRoles")

        fun valueOf(name: String, flags: MfFlags): MfFactionPermission? = when {
            name == "ADD_LAW" -> ADD_LAW
            name == "REMOVE_LAW" -> REMOVE_LAW
            name == "LIST_LAWS" -> LIST_LAWS
            name == "REQUEST_ALLIANCE" -> REQUEST_ALLIANCE
            name == "BREAK_ALLIANCE" -> BREAK_ALLIANCE
            name == "TOGGLE_AUTOCLAIM" -> TOGGLE_AUTOCLAIM
            name == "CHAT" -> CHAT
            name == "CLAIM" -> CLAIM
            name == "UNCLAIM" -> UNCLAIM
            name == "DECLARE_INDEPENDENCE" -> DECLARE_INDEPENDENCE
            name == "SWEAR_FEALTY" -> SWEAR_FEALTY
            name == "GRANT_INDEPENDENCE" -> GRANT_INDEPENDENCE
            name == "VASSALIZE" -> VASSALIZE
            name == "DECLARE_WAR" -> DECLARE_WAR
            name == "MAKE_PEACE" -> MAKE_PEACE
            name == "PROMOTE" -> PROMOTE
            name == "DEMOTE" -> DEMOTE
            name == "CHANGE_NAME" -> CHANGE_NAME
            name == "CHANGE_DESCRIPTION" -> CHANGE_DESCRIPTION
            name == "CHANGE_PREFIX" -> CHANGE_PREFIX
            name == "DISBAND" -> DISBAND
            name.matches(Regex("SET_FLAG\\((.+)\\)")) -> Regex("SET_FLAG\\((.+)\\)").find(name)
                ?.groupValues?.get(1)
                ?.let<String, MfFlag<out Any>?>(flags::get)
                ?.let(SET_FLAG)
            name == "VIEW_FLAGS" -> VIEW_FLAGS
            name == "CREATE_GATE" -> CREATE_GATE
            name == "REMOVE_GATE" -> REMOVE_GATE
            name == "RENAME_GATE" -> RENAME_GATE
            name == "LIST_GATES" -> LIST_GATES
            name == "SET_HOME" -> SET_HOME
            name == "GO_HOME" -> GO_HOME
            name == "VIEW_INFO" -> VIEW_INFO
            name == "VIEW_STATS" -> VIEW_STATS
            name == "INVITE" -> INVITE
            name == "INVOKE" -> INVOKE
            name == "KICK" -> KICK
            name.matches(Regex("VIEW_ROLE\\((.+)\\)")) -> Regex("VIEW_ROLE\\((.+)\\)").find(name)
                ?.groupValues?.get(1)
                ?.let(::MfFactionRoleId)
                ?.let(VIEW_ROLE)
            name.matches(Regex("SET_ROLE_PERMISSION\\((.+)\\)")) -> Regex("SET_ROLE_PERMISSION\\((.+)\\)").find(name)
                ?.groupValues?.get(1)
                ?.let { valueOf(it, flags) }
                ?.let(SET_ROLE_PERMISSION)
            name.matches(Regex("MODIFY_ROLE\\((.+)\\)")) -> Regex("MODIFY_ROLE\\((.+)\\)").find(name)
                ?.groupValues?.get(1)
                ?.let(::MfFactionRoleId)
                ?.let(MODIFY_ROLE)
            name.matches(Regex("SET_MEMBER_ROLE\\((.+)\\)")) -> Regex("SET_MEMBER_ROLE\\((.+)\\)").find(name)
                ?.groupValues?.get(1)
                ?.let(::MfFactionRoleId)
                ?.let(SET_MEMBER_ROLE)
            name == "LIST_ROLES" -> LIST_ROLES
            else -> null
        }

        fun values(flags: MfFlags, roles: MfFactionRoles) = values(flags, roles.map(MfFactionRole::id))

        fun values(flags: MfFlags, roleIds: List<MfFactionRoleId>) = buildList {
            add(ADD_LAW)
            add(REMOVE_LAW)
            add(LIST_LAWS)
            add(REQUEST_ALLIANCE)
            add(BREAK_ALLIANCE)
            add(TOGGLE_AUTOCLAIM)
            add(CHAT)
            add(CLAIM)
            add(UNCLAIM)
            add(DECLARE_INDEPENDENCE)
            add(SWEAR_FEALTY)
            add(GRANT_INDEPENDENCE)
            add(VASSALIZE)
            add(DECLARE_WAR)
            add(MAKE_PEACE)
            add(PROMOTE)
            add(DEMOTE)
            add(CHANGE_NAME)
            add(CHANGE_DESCRIPTION)
            add(CHANGE_PREFIX)
            add(DISBAND)
            flags.forEach { flag ->
                add(SET_FLAG(flag))
            }
            add(VIEW_FLAGS)
            add(CREATE_GATE)
            add(REMOVE_GATE)
            add(RENAME_GATE)
            add(LIST_GATES)
            add(SET_HOME)
            add(GO_HOME)
            add(VIEW_INFO)
            add(VIEW_STATS)
            add(INVITE)
            add(INVOKE)
            add(KICK)
            roleIds.forEach { roleId ->
                add(VIEW_ROLE(roleId))
                add(MODIFY_ROLE(roleId))
                add(SET_MEMBER_ROLE(roleId))
            }
            add(LIST_ROLES)

            addAll(map { permission -> SET_ROLE_PERMISSION(permission) })
        }
    }
    
}