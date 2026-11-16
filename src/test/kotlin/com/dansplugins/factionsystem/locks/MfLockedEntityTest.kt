package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.player.MfPlayerId
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.GlowItemFrame
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Painting
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.util.UUID

class MfLockedEntityTest {

    private val owner = MfPlayerId(UUID.randomUUID().toString())
    private val accessor = MfPlayerId(UUID.randomUUID().toString())
    private val lock = MfLockedEntity(
        entityId = UUID.randomUUID(),
        worldId = UUID.randomUUID(),
        chunkX = 0,
        chunkZ = 0,
        playerId = owner,
        accessors = listOf(accessor)
    )

    @Test
    fun `the owner and listed players have access`() {
        assertTrue(lock.hasAccess(owner))
        assertTrue(lock.hasAccess(MfPlayerId(accessor.value)))
        assertFalse(lock.hasAccess(MfPlayerId(UUID.randomUUID().toString())))
    }

    @Test
    fun `armour stands and item frames can be locked, other entities cannot`() {
        assertTrue(MfLockedEntity.isLockable(mock(ArmorStand::class.java)))
        assertTrue(MfLockedEntity.isLockable(mock(ItemFrame::class.java)))
        assertTrue(MfLockedEntity.isLockable(mock(GlowItemFrame::class.java)))
        assertFalse(MfLockedEntity.isLockable(mock(Painting::class.java)))
        assertFalse(MfLockedEntity.isLockable(mock(Entity::class.java)))
    }
}
