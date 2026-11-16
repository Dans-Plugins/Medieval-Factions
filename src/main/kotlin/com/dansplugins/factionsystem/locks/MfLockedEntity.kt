package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.player.MfPlayerId
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemFrame
import java.util.UUID

/**
 * A lock on an armour stand or item frame (#1679), the entity counterpart of [MfLockedBlock].
 *
 * The entity is identified by its UUID, which Minecraft keeps for the life of the entity. The chunk it
 * was locked in is kept so the lock is removed with the claim, as block locks are.
 */
data class MfLockedEntity(
    @get:JvmName("getId")
    val id: MfLockedEntityId = MfLockedEntityId.generate(),
    val version: Int = 0,
    val entityId: UUID,
    val worldId: UUID,
    val chunkX: Int,
    val chunkZ: Int,
    @get:JvmName("getPlayerId")
    val playerId: MfPlayerId,
    @get:JvmName("getAccessorPlayerIds")
    val accessors: List<MfPlayerId> = emptyList()
) {

    /** Whether [playerId] may use the entity: its owner or a player on its access list. */
    @JvmName("hasAccess")
    fun hasAccess(playerId: MfPlayerId): Boolean =
        playerId.value == this.playerId.value || accessors.any { it.value == playerId.value }

    companion object {
        /** Whether [entity] is a kind of entity that can be locked: an armour stand or an item frame (glow item frames included). */
        @JvmStatic
        fun isLockable(entity: Entity): Boolean = entity is ArmorStand || entity is ItemFrame
    }
}
