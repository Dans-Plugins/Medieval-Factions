package dansplugins.factionsystem.faction.role

import dansplugins.factionsystem.faction.permission.MfFactionPermission.*

data class MfFactionRoles(
    val default: MfFactionRole,
    val roles: List<MfFactionRole>
) : List<MfFactionRole> by roles {
    companion object {
        fun defaults(): MfFactionRoles {
            val owner = MfFactionRole(
                name = "Owner",
                permissions = mapOf(
                    ADD_LAW to true,
                    REMOVE_LAW to true,
                    LIST_LAWS to true,
                    REQUEST_ALLIANCE to true,
                    BREAK_ALLIANCE to true,
                    TOGGLE_AUTOCLAIM to true,
                    CHAT to true,
                    CLAIM to true,
                    UNCLAIM to true,
                    DECLARE_INDEPENDENCE to true,
                    SWEAR_FEALTY to true,
                    GRANT_INDEPENDENCE to true,
                    VASSALIZE to true,
                    DECLARE_WAR to true,
                    MAKE_PEACE to true,
                    PROMOTE to true,
                    DEMOTE to true,
                    CHANGE_NAME to true,
                    CHANGE_DESCRIPTION to true,
                    CHANGE_PREFIX to true,
                    DISBAND to true,
                    SET_FLAG to true,
                    VIEW_FLAGS to true,
                    CREATE_GATE to true,
                    REMOVE_GATE to true,
                    RENAME_GATE to true,
                    LIST_GATES to true,
                    SET_HOME to true,
                    GO_HOME to true,
                    VIEW_INFO to true,
                    VIEW_STATS to true,
                    INVITE to true,
                    INVOKE to true,
                    KICK to true
                )
            )
            val officer = MfFactionRole(
                name = "Officer",
                permissions = mapOf(
                    REQUEST_ALLIANCE to true,
                    DECLARE_WAR to true,
                    MAKE_PEACE to true,
                    PROMOTE to true,
                    DEMOTE to true,
                    SET_HOME to true,
                    KICK to true
                )
            )
            val member = MfFactionRole(name = "Member")
            return MfFactionRoles(member, listOf(owner, officer, member))
        }
    }
    fun getRole(roleId: MfFactionRoleId) = roles.singleOrNull { it.id.value == roleId.value }
}