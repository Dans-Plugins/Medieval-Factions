package com.dansplugins.factionsystem.listener

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockFertilizeEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause
import org.bukkit.event.entity.EntityPlaceEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.UUID

/**
 * Each listener only turns its event into a [WorldActionProtection.protect] call; the rules themselves
 * are covered by [WorldActionProtectionTest].
 */
class WorldActionListenersTest {
    private lateinit var protection: WorldActionProtection
    private lateinit var player: Player
    private lateinit var block: Block
    private lateinit var trimmer: GrowthBorderTrimmer
    private lateinit var world: World
    private val worldId: UUID = UUID.randomUUID()
    private val grown: MutableList<BlockState> = mutableListOf()

    @BeforeEach
    fun setUp() {
        protection = mock(WorldActionProtection::class.java)
        player = mock(Player::class.java)
        block = mock(Block::class.java)
        trimmer = mock(GrowthBorderTrimmer::class.java)
        world = mock(World::class.java)
    }

    private fun ignite(cause: IgniteCause, player: Player?): BlockIgniteEvent {
        val event = mock(BlockIgniteEvent::class.java)
        `when`(event.cause).thenReturn(cause)
        `when`(event.player).thenReturn(player)
        `when`(event.block).thenReturn(block)
        return event
    }

    @Test
    fun blockIgnite_FlintAndSteel_ShouldProtectWithFlintAndSteel() {
        val event = ignite(IgniteCause.FLINT_AND_STEEL, player)
        BlockIgniteListener(protection).onBlockIgnite(event)
        verify(protection).protect(event, player, block, Material.FLINT_AND_STEEL)
    }

    @Test
    fun blockIgnite_FireCharge_ShouldProtectWithFireCharge() {
        val event = ignite(IgniteCause.FIREBALL, player)
        BlockIgniteListener(protection).onBlockIgnite(event)
        verify(protection).protect(event, player, block, Material.FIRE_CHARGE)
    }

    @Test
    fun blockIgnite_Spread_ShouldBeIgnored() {
        BlockIgniteListener(protection).onBlockIgnite(ignite(IgniteCause.SPREAD, player))
        verifyNoInteractions(protection)
    }

    @Test
    fun blockIgnite_NoPlayer_ShouldBeIgnored() {
        BlockIgniteListener(protection).onBlockIgnite(ignite(IgniteCause.FIREBALL, null))
        verifyNoInteractions(protection)
    }

    private fun fertilizeEvent(player: Player?): BlockFertilizeEvent {
        val event = mock(BlockFertilizeEvent::class.java)
        `when`(event.player).thenReturn(player)
        `when`(event.block).thenReturn(block)
        `when`(block.world).thenReturn(world)
        `when`(world.uid).thenReturn(worldId)
        `when`(block.x).thenReturn(33)
        `when`(block.z).thenReturn(-7)
        `when`(event.blocks).thenReturn(grown)
        return event
    }

    @Test
    fun blockFertilize_ByPlayer_Allowed_ShouldProtectWithBoneMealThenTrimTheGrowth() {
        val event = fertilizeEvent(player)
        BlockFertilizeListener(protection, trimmer).onBlockFertilize(event)
        verify(protection).protect(event, player, block, Material.BONE_MEAL)
        verify(trimmer).trim(worldId, 33, -7, grown)
    }

    @Test
    fun blockFertilize_ByPlayer_Refused_ShouldNotTrim() {
        val event = fertilizeEvent(player)
        `when`(protection.protect(event, player, block, Material.BONE_MEAL)).thenReturn(true)
        BlockFertilizeListener(protection, trimmer).onBlockFertilize(event)
        verifyNoInteractions(trimmer)
    }

    @Test
    fun blockFertilize_ByDispenser_ShouldOnlyTrimTheGrowth() {
        val event = fertilizeEvent(null)
        BlockFertilizeListener(protection, trimmer).onBlockFertilize(event)
        verifyNoInteractions(protection)
        verify(trimmer).trim(worldId, 33, -7, grown)
    }

    @Test
    fun structureGrow_ShouldTrimFromTheGrowthsOrigin() {
        val event = mock(StructureGrowEvent::class.java)
        val location = mock(Location::class.java)
        `when`(location.world).thenReturn(world)
        `when`(world.uid).thenReturn(worldId)
        `when`(location.blockX).thenReturn(-17)
        `when`(location.blockZ).thenReturn(40)
        `when`(event.location).thenReturn(location)
        `when`(event.blocks).thenReturn(grown)

        StructureGrowListener(trimmer).onStructureGrow(event)

        verify(trimmer).trim(worldId, -17, 40, grown)
    }

    @Test
    fun entityPlace_ShouldProtectWithTheItemInTheHandUsed() {
        val event = mock(EntityPlaceEvent::class.java)
        `when`(event.player).thenReturn(player)
        `when`(event.block).thenReturn(block)
        `when`(event.hand).thenReturn(EquipmentSlot.OFF_HAND)
        val inventory = mock(PlayerInventory::class.java)
        `when`(player.inventory).thenReturn(inventory)
        val stack = mock(ItemStack::class.java)
        `when`(stack.type).thenReturn(Material.OAK_BOAT)
        `when`(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(stack)

        EntityPlaceListener(protection).onEntityPlace(event)

        verify(protection).protect(event, player, block, Material.OAK_BOAT)
    }

    @Test
    fun entityPlace_NoPlayer_ShouldBeIgnored() {
        val event = mock(EntityPlaceEvent::class.java)
        EntityPlaceListener(protection).onEntityPlace(event)
        verifyNoInteractions(protection)
    }

    @Test
    fun hangingPlace_ShouldProtectWithTheItemHung() {
        val event = mock(HangingPlaceEvent::class.java)
        `when`(event.player).thenReturn(player)
        `when`(event.block).thenReturn(block)
        val stack = mock(ItemStack::class.java)
        `when`(stack.type).thenReturn(Material.ITEM_FRAME)
        `when`(event.itemStack).thenReturn(stack)

        HangingPlaceListener(protection).onHangingPlace(event)

        verify(protection).protect(event, player, block, Material.ITEM_FRAME)
    }

    @Test
    fun hangingPlace_NoItemStack_ShouldProtectWithoutAnItem() {
        val event = mock(HangingPlaceEvent::class.java)
        `when`(event.player).thenReturn(player)
        `when`(event.block).thenReturn(block)

        HangingPlaceListener(protection).onHangingPlace(event)

        verify(protection).protect(event, player, block, null)
    }

    @Test
    fun hangingPlace_NoPlayer_ShouldBeIgnored() {
        val event = mock(HangingPlaceEvent::class.java)
        HangingPlaceListener(protection).onHangingPlace(event)
        verifyNoInteractions(protection)
    }
}
