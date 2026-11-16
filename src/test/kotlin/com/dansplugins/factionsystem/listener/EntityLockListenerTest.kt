package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.locks.MfLockedEntity
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.Server
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Arrow
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

/** Who may hit, empty and break a locked armour stand or item frame (#1679). */
class EntityLockListenerTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var lockService: MfLockService
    private lateinit var uut: EntityLockListener

    private val ownerUuid = UUID.randomUUID()
    private val accessorUuid = UUID.randomUUID()
    private val strangerUuid = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        val services = mock(Services::class.java)
        lockService = mock(MfLockService::class.java)
        `when`(plugin.services).thenReturn(services)
        `when`(services.lockService).thenReturn(lockService)
        `when`(services.playerService).thenReturn(mock(MfPlayerService::class.java))
        `when`(services.factionService).thenReturn(mock(MfFactionService::class.java))
        val language = mock(Language::class.java)
        `when`(language.get(anyString(), anyString())).thenReturn("locked")
        `when`(plugin.language).thenReturn(language)
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        `when`(server.scheduler).thenReturn(mock(BukkitScheduler::class.java))
        uut = EntityLockListener(plugin)
    }

    @Test
    fun `a stranger cannot hit a locked armour stand`() {
        val stand = locked(ArmorStand::class.java)
        val event = damage(stand, player(strangerUuid))

        uut.onEntityDamageByEntity(event)

        verify(event).isCancelled = true
    }

    @Test
    fun `an accessor cannot break a locked armour stand, but its owner can`() {
        val stand = locked(ArmorStand::class.java)
        val byAccessor = damage(stand, player(accessorUuid))
        val byOwner = damage(stand, player(ownerUuid))

        uut.onEntityDamageByEntity(byAccessor)
        uut.onEntityDamageByEntity(byOwner)

        verify(byAccessor).isCancelled = true
        verify(byOwner, never()).isCancelled = true
    }

    @Test
    fun `mf force unlock lets a player break another player's locked armour stand`() {
        val stand = locked(ArmorStand::class.java)
        val breaker = player(strangerUuid)
        `when`(breaker.hasPermission("mf.force.unlock")).thenReturn(true)
        val event = damage(stand, breaker)

        uut.onEntityDamageByEntity(event)

        verify(event, never()).isCancelled = true
    }

    @Test
    fun `an accessor may take the item out of a locked item frame, a stranger may not`() {
        val frame = locked(ItemFrame::class.java)
        val byAccessor = damage(frame, player(accessorUuid))
        val byStranger = damage(frame, player(strangerUuid))

        uut.onEntityDamageByEntity(byAccessor)
        uut.onEntityDamageByEntity(byStranger)

        verify(byAccessor, never()).isCancelled = true
        verify(byStranger).isCancelled = true
    }

    @Test
    fun `a stranger's arrow counts as the stranger`() {
        val frame = locked(ItemFrame::class.java)
        val arrow = mock(Arrow::class.java)
        val shooter = player(strangerUuid)
        `when`(arrow.shooter).thenReturn(shooter)
        val event = damage(frame, arrow)

        uut.onEntityDamageByEntity(event)

        verify(event).isCancelled = true
    }

    @Test
    fun `damage that does not come from a player is left alone, as for locked blocks`() {
        val stand = locked(ArmorStand::class.java)
        val event = damage(stand, mock(Zombie::class.java))

        uut.onEntityDamageByEntity(event)

        verify(event, never()).isCancelled = true
    }

    @Test
    fun `only the owner may break a locked item frame off its wall`() {
        val frame = locked(ItemFrame::class.java)
        // Players are built before stubbing: building a mock inside thenReturn(...) is unfinished stubbing.
        val accessor = player(accessorUuid)
        val owner = player(ownerUuid)
        val byAccessor = mock(HangingBreakByEntityEvent::class.java)
        `when`(byAccessor.entity).thenReturn(frame)
        `when`(byAccessor.remover).thenReturn(accessor)
        val byOwner = mock(HangingBreakByEntityEvent::class.java)
        `when`(byOwner.entity).thenReturn(frame)
        `when`(byOwner.remover).thenReturn(owner)

        uut.onHangingBreakByEntity(byAccessor)
        uut.onHangingBreakByEntity(byOwner)

        verify(byAccessor).isCancelled = true
        verify(byOwner, never()).isCancelled = true
    }

    @Test
    fun `a stranger cannot take armour off a locked armour stand`() {
        val stand = locked(ArmorStand::class.java)
        val event = mock(PlayerArmorStandManipulateEvent::class.java)
        val stranger = player(strangerUuid)
        `when`(event.player).thenReturn(stranger)
        `when`(event.rightClicked).thenReturn(stand)

        uut.onPlayerArmorStandManipulate(event)

        verify(event).isCancelled = true
    }

    @Test
    fun `an unlocked armour stand is left alone`() {
        val stand = mock(ArmorStand::class.java)
        `when`(stand.uniqueId).thenReturn(UUID.randomUUID())
        val event = damage(stand, player(strangerUuid))

        uut.onEntityDamageByEntity(event)

        verify(event, never()).isCancelled = true
    }

    private fun <T : Entity> locked(type: Class<T>): T {
        val entity = mock(type)
        val entityId = UUID.randomUUID()
        `when`(entity.uniqueId).thenReturn(entityId)
        val lockedEntity = MfLockedEntity(
            entityId = entityId,
            worldId = UUID.randomUUID(),
            chunkX = 0,
            chunkZ = 0,
            playerId = MfPlayerId(ownerUuid.toString()),
            accessors = listOf(MfPlayerId(accessorUuid.toString()))
        )
        `when`(lockService.getLockedEntity(entityId)).thenReturn(lockedEntity)
        return entity
    }

    private fun player(uuid: UUID): Player {
        val player = mock(Player::class.java)
        `when`(player.uniqueId).thenReturn(uuid)
        return player
    }

    private fun damage(entity: Entity, damager: Entity): EntityDamageByEntityEvent {
        val event = mock(EntityDamageByEntityEvent::class.java)
        `when`(event.entity).thenReturn(entity)
        `when`(event.damager).thenReturn(damager)
        return event
    }
}
