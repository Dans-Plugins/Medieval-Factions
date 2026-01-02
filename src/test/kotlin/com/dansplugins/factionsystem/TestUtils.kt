package com.dansplugins.factionsystem

import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * A utility class for creating mock objects and events, commonly used in testing scenarios.
 */
class TestUtils {

    /**
     * Creates a mock implementation of a Block with default properties and a specified or mocked world.
     *
     * @param world The World object used for the mocked Block. Defaults to a mocked World instance if not provided.
     * @return A mocked Block instance with predefined properties and associations.
     */
    fun createMockBlock(world: World = mock(World::class.java)): Block {
        val block = mock(Block::class.java)
        `when`(block.world).thenReturn(world)
        `when`(block.x).thenReturn(0)
        `when`(block.y).thenReturn(0)
        `when`(block.z).thenReturn(0)
        `when`(block.chunk).thenReturn(mock(org.bukkit.Chunk::class.java))
        return block
    }

    /**
     * Creates and returns a mocked instance of a `World` object.
     * The returned `World` mock has its `uid` property set to a mocked `UUID`.
     *
     * @return A mocked `World` instance with a mocked `uid`.
     */
    fun createMockWorld(): World {
        val world = mock(World::class.java)
        val worldUid = mock(java.util.UUID::class.java)
        `when`(world.uid).thenReturn(worldUid)
        return world
    }

    /**
     * Creates a mocked instance of a BlockBreakEvent with a specified block and player.
     *
     * @param block The block involved in the block break event.
     * @param player The player who broke the block.
     * @return A mocked BlockBreakEvent with the provided block and player set.
     */
    fun createBlockBreakEvent(block: Block, player: Player): BlockBreakEvent {
        val event = mock(BlockBreakEvent::class.java)
        `when`(event.block).thenReturn(block)
        `when`(event.player).thenReturn(player)
        return event
    }

    /**
     * Creates a mocked instance of a BlockPlaceEvent with a specified block and player.
     *
     * @param block The block involved in the block place event.
     * @param player The player who placed the block.
     * @return A mocked BlockPlaceEvent with the provided block and player set.
     */
    fun createBlockPlaceEvent(block: Block, player: Player): BlockPlaceEvent {
        val event = mock(BlockPlaceEvent::class.java)
        `when`(event.block).thenReturn(block)
        `when`(event.player).thenReturn(player)
        return event
    }

    /**
     * Creates a mocked instance of a PlayerInteractEvent with specified parameters.
     *
     * @param player The player who triggered the interact event.
     * @param action The action type (e.g., RIGHT_CLICK_BLOCK).
     * @param item The item stack in the player's hand (nullable).
     * @param clickedBlock The block that was clicked (nullable).
     * @param hand The equipment slot used for the interaction.
     * @return A mocked PlayerInteractEvent with the provided parameters set.
     */
    fun createPlayerInteractEvent(
        player: Player,
        action: Action = Action.RIGHT_CLICK_BLOCK,
        item: ItemStack? = null,
        clickedBlock: Block? = null,
        hand: EquipmentSlot = EquipmentSlot.HAND
    ): PlayerInteractEvent {
        val event = mock(PlayerInteractEvent::class.java)
        `when`(event.player).thenReturn(player)
        `when`(event.action).thenReturn(action)
        `when`(event.item).thenReturn(item)
        `when`(event.clickedBlock).thenReturn(clickedBlock)
        `when`(event.hand).thenReturn(hand)
        return event
    }

    /**
     * Creates a mocked `CommandTestFixture` containing mocked instances of `Player`, `Command`, and `CommandSender`.
     *
     * @return A `CommandTestFixture` object with mocked components.
     */
    fun createCommandTestFixture(): CommandTestFixture {
        val player = mock(Player::class.java)
        val command = mock(Command::class.java)
        val sender = mock(CommandSender::class.java)
        return CommandTestFixture(player, command, sender)
    }

    /**
     * A data class representing a fixture for testing commands in a mocked environment.
     *
     * @property player The mocked player involved in the command test.
     * @property command The mocked command under test.
     * @property sender The mocked command sender executing the command.
     */
    data class CommandTestFixture(
        val player: Player,
        val command: Command,
        val sender: CommandSender
    )
}
