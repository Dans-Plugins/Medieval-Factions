package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.notification.MfNotification
import com.dansplugins.factionsystem.player.MfPlayerId
import java.util.Collections.emptyList

data class MfFaction(
    private val plugin: MedievalFactions,
    @get:JvmName("getId")
    val id: MfFactionId = MfFactionId.generate(),
    val version: Int = 0,
    val name: String,
    val description: String = "",
    val members: List<MfFactionMember> = emptyList(),
    val invites: List<MfFactionInvite> = emptyList(),
    val flags: MfFlagValues = plugin.flags.defaults(),
    val prefix: String? = null,
    val home: MfPosition? = null,
    val bonusPower: Double = 0.0,
    val autoclaim: Boolean = false,
    val autounclaim: Boolean = false,
    val roles: MfFactionRoles = MfFactionRoles.defaults(plugin, id),
    val defaultPermissionsByName: Map<String, Boolean> = plugin.factionPermissions.permissionsFor(id, roles).associate { it.name to it.default },
    val applications: List<MfFactionApplication> = emptyList()
) {

    val memberPower
        get() = members.sumOf { plugin.services.playerService.getPlayer(it.playerId)?.power ?: 0.0 }
    val maxMemberPower
        get() = members.size * plugin.config.getDouble("players.maxPower")
    val vassalPower
        get() = plugin.services.factionRelationshipService.getVassals(id)
            .mapNotNull(plugin.services.factionService::getFaction)
            .sumOf { it.power * plugin.config.getDouble("factions.vassalPowerContributionMultiplier") }
    val maxVassalPower
        get() = plugin.services.factionRelationshipService.getVassals(id)
            .mapNotNull(plugin.services.factionService::getFaction)
            .sumOf { it.maxPower * plugin.config.getDouble("factions.vassalPowerContributionMultiplier") }

    val power: Double
        get() = memberPower + (if (memberPower >= maxMemberPower / 2.0) { vassalPower } else { 0.0 }) + (if (flags[plugin.flags.acceptBonusPower]) bonusPower else 0.0)

    val maxPower: Double
        get() = maxMemberPower + maxVassalPower + (if (flags[plugin.flags.acceptBonusPower]) bonusPower else 0.0)

    // A faction saves every permission's default when it is created, and a role with no entry of its own falls back
    // to that saved value before the permission's built-in default. Factions created before role management was
    // made deny-by-default (GHSA-hqm3-5qwr-49gp) saved SET_MEMBER_ROLE, MODIFY_ROLE and DELETE_ROLE as true, so the
    // default Member role could still manage roles. Those three are always read as false; nothing sets a faction's
    // saved defaults after creation, so no choice made by the faction is overridden. A save writes the false back.
    val savedDefaultPermissionsByName: Map<String, Boolean>
        get() = defaultPermissionsByName.mapValues { (key, value) -> value && !key.matches(ROLE_MANAGEMENT) }

    val defaultPermissions: Map<MfFactionPermission, Boolean>
        get() = savedDefaultPermissionsByName.toList().map { (key, value) -> plugin.factionPermissions.parse(key) to value }
            .filter { (key, _) -> key != null }
            .associate { (key, value) -> key!! to value }

    @JvmName("getRoleByPlayerId")
    fun getRole(playerId: MfPlayerId): MfFactionRole? = members.singleOrNull { it.playerId == playerId }?.role

    @JvmName("getRoleByRoleId")
    fun getRole(roleId: MfFactionRoleId): MfFactionRole? = roles.getRole(roleId)

    @JvmName("getRoleByName")
    fun getRole(name: String): MfFactionRole? = roles.getRole(name)

    fun sendMessage(title: String, message: String) {
        members.map { it.playerId }
            .forEach { mfPlayer ->
                val offlinePlayer = mfPlayer.toBukkitPlayer()
                val player = offlinePlayer.player
                if (player != null) {
                    player.sendMessage("$title - $message")
                } else {
                    plugin.server.scheduler.runTaskAsynchronously(
                        plugin,
                        Runnable {
                            plugin.services.notificationService.sendNotification(
                                mfPlayer,
                                MfNotification(
                                    title,
                                    message
                                )
                            )
                        }
                    )
                }
            }
    }

    companion object {
        private val ROLE_MANAGEMENT = Regex("(SET_MEMBER_ROLE|MODIFY_ROLE|DELETE_ROLE)\\(.+\\)")
    }
}
