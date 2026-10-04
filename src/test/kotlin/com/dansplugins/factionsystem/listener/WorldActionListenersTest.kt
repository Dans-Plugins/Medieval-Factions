package com.dansplugins.factionsystem.listener

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockFertilizeEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause
import org.bukkit.event.entity.EntityPlaceEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

/**
 * Each listener only turns its event into a [WorldActionProtection.protect] call; the rules themselves
 * are covered by [WorldActionProtectionTest].
 */
class WorldActionListenersTest {
    private lateinit var protection: WorldActionProtection
    private lateinit var player: Player
    private lateinit var block: Block

    @BeforeEach
    fun setUp() {
        protection = mock(WorldActionProtection::class.java)
        player = mock(Player::class.java)
        block = mock(Block::class.java)
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

    @Test
    fun blockFertilize_ByPlayer_ShouldProtectWithBoneMeal() {
        val event = mock(BlockFertilizeEvent::class.java)
        `when`(event.player).thenReturn(player)
        `when`(event.block).thenReturn(block)
        BlockFertilizeListener(protection).onBlockFertilize(event)
        verify(protection).protect(event, player, block, Material.BONE_MEAL)
    }

    @Test
    fun blockFertilize_ByDispenser_ShouldBeIgnored() {
        val event = mock(BlockFertilizeEvent::class.java)
        `when`(event.block).thenReturn(block)
        BlockFertilizeListener(protection).onBlockFertilize(event)
        verifyNoInteractions(protection)
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
