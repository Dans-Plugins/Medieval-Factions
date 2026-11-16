package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.player.MfPlayerId

data class MfLockedBlock(
    @get:JvmName("getId")
    val id: MfLockedBlockId = MfLockedBlockId.generate(),
    val version: Int = 0,
    val block: MfBlockPosition,
    val chunkX: Int,
    val chunkZ: Int,
    @get:JvmName("getPlayerId")
    val playerId: MfPlayerId,
    @get:JvmName("getAccessorPlayerIds")
    val accessors: List<MfPlayerId>,
    /**
     * Roles of the lock owner's faction whose holders may also access the block (#1569). A role id
     * only grants access while the lock owner is in the faction the role belongs to.
     */
    @get:JvmName("getAccessorRoleIds")
    val accessorRoleIds: List<MfFactionRoleId> = emptyList()
) {

    /**
     * Whether [playerId] may access this block: the owner, a player on the access list, or a member
     * of [ownerFaction] whose role is on the role access list.
     *
     * @param ownerFaction the faction the lock owner is currently in, or null if they are in none. It
     * is only consulted for role accessors, so callers may pass null when [accessorRoleIds] is empty.
     */
    @JvmName("hasAccess")
    fun hasAccess(playerId: MfPlayerId, ownerFaction: MfFaction?): Boolean {
        if (playerId.value == this.playerId.value) return true
        if (accessors.any { it.value == playerId.value }) return true
        if (accessorRoleIds.isEmpty() || ownerFaction == null) return false
        if (ownerFaction.members.none { it.playerId.value == this.playerId.value }) return false
        val role = ownerFaction.getRole(playerId) ?: return false
        return accessorRoleIds.any { it.value == role.id.value }
    }
}
