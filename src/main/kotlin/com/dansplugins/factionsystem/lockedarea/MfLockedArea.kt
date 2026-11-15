package com.dansplugins.factionsystem.lockedarea

import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.player.MfPlayerId

/**
 * A named cuboid inside a faction's territory in which only the listed players, and members holding
 * one of the listed roles, may build, break and use blocks (#1393). It applies on top of territory
 * protection: anyone refused by the claim is still refused.
 */
data class MfLockedArea(
    @get:JvmName("getId")
    val id: MfLockedAreaId = MfLockedAreaId.generate(),
    val version: Int = 0,
    @get:JvmName("getFactionId")
    val factionId: MfFactionId,
    val name: String,
    val area: MfCuboidArea,
    @get:JvmName("getAccessorPlayerIds")
    val accessors: List<MfPlayerId> = emptyList(),
    @get:JvmName("getAccessorRoleIds")
    val accessorRoleIds: List<MfFactionRoleId> = emptyList()
) {

    operator fun contains(position: MfBlockPosition): Boolean {
        val min = area.minPosition
        val max = area.maxPosition
        return position.worldId == min.worldId &&
            position.x in min.x..max.x &&
            position.y in min.y..max.y &&
            position.z in min.z..max.z
    }

    /** Whether the player [playerId], holding [roleId] in the area's faction (null if not a member), is listed. */
    @JvmName("allows")
    fun allows(playerId: MfPlayerId, roleId: MfFactionRoleId?): Boolean =
        accessors.any { it.value == playerId.value } ||
            (roleId != null && accessorRoleIds.any { it.value == roleId.value })
}
