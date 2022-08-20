package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
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
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.LIST_GATES
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.LIST_LAWS
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.LIST_ROLES
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.MAKE_PEACE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.MODIFY_ROLE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.PROMOTE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.REMOVE_GATE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.REMOVE_LAW
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.RENAME_GATE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.REQUEST_ALLIANCE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.SET_FLAG
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.SET_HOME
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.SET_ROLE_PERMISSION
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.SWEAR_FEALTY
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.TOGGLE_AUTOCLAIM
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.UNCLAIM
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VASSALIZE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VIEW_FLAGS
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VIEW_INFO
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VIEW_ROLE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VIEW_STATS
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.notification.MfNotification
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import kotlin.math.roundToInt

data class MfFaction(
    private val plugin: MedievalFactions,
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
    val roles: MfFactionRoles = MfFactionRoles.defaults(plugin.flags),
    val defaultPermissions: Map<MfFactionPermission, Boolean> = buildMap {
        put(ADD_LAW, false)
        put(REMOVE_LAW, false)
        put(LIST_LAWS, true)
        put(REQUEST_ALLIANCE, false)
        put(BREAK_ALLIANCE, false)
        put(TOGGLE_AUTOCLAIM, false)
        put(CHAT, true)
        put(CLAIM, false)
        put(UNCLAIM, false)
        put(DECLARE_INDEPENDENCE, false)
        put(SWEAR_FEALTY, false)
        put(GRANT_INDEPENDENCE, false)
        put(VASSALIZE, false)
        put(DECLARE_WAR, false)
        put(MAKE_PEACE, false)
        put(PROMOTE, false)
        put(DEMOTE, false)
        put(CHANGE_NAME, false)
        put(CHANGE_DESCRIPTION, false)
        put(CHANGE_PREFIX, false)
        put(DISBAND, false)
        put(VIEW_FLAGS, true)
        plugin.flags.forEach { flag ->
            put(SET_FLAG(flag), false)
        }
        put(CREATE_GATE, false)
        put(REMOVE_GATE, false)
        put(RENAME_GATE, false)
        put(LIST_GATES, false)
        put(SET_HOME, false)
        put(GO_HOME, true)
        put(VIEW_INFO, true)
        put(VIEW_STATS, true)
        put(INVITE, true)
        put(INVOKE, false)
        put(KICK, false)
        roles.forEach { role ->
            put(VIEW_ROLE(role.id), true)
            put(MODIFY_ROLE(role.id), false)
        }
        put(LIST_ROLES, true)

        putAll(keys.map { permission -> SET_ROLE_PERMISSION(permission) to false })
    }
) {

    private val memberPower
        get() = members.sumOf { member -> member.player.power }
    private val maxMemberPower
        get() = members.size * plugin.config.getInt("players.maxPower")
    private val vassalPower
        get() = plugin.services.factionRelationshipService.getRelationships(id, VASSAL)
            .mapNotNull { relationship -> plugin.services.factionService.getFaction(relationship.targetId) }
            .sumOf { it.power * plugin.config.getDouble("factions.vassalPowerContributionMultiplier") }
            .roundToInt()

    val power: Int
        get() = memberPower + (if (memberPower >= maxMemberPower / 2) { vassalPower } else { 0 }) + bonusPower

    constructor(plugin: MedievalFactions, name: String) : this(
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
    fun getRole(name: String): MfFactionRole? = roles.getRole(name)

    fun sendMessage(title: String, message: String) {
        members.map { it.player }
            .forEach { mfPlayer ->
                val offlinePlayer = mfPlayer.toBukkit()
                val player = offlinePlayer.player
                if (player != null) {
                    player.sendMessage("$title - $message")
                } else {
                    plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                        plugin.notificationDispatcher.sendNotification(mfPlayer, MfNotification(
                            title,
                            message
                        ))
                    })
                }
            }
    }

}