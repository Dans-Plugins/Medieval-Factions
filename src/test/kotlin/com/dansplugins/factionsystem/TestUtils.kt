package com.dansplugins.factionsystem

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayerId
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.*

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
     * The returned `World` mock has its `uid` property set to a specified UUID.
     *
     * @param uid The UUID to use for the world. Defaults to a random UUID if not provided.
     * @return A mocked `World` instance with the specified `uid`.
     */
    fun createMockWorld(uid: UUID = UUID.randomUUID()): World {
        val world = mock(World::class.java)
        `when`(world.uid).thenReturn(uid)
        return world
    }

    /**
     * Creates a mocked Chunk with specified coordinates and world.
     *
     * @param world The World object for the chunk. Defaults to a mocked World instance if not provided.
     * @param x The x-coordinate of the chunk. Defaults to 0 if not provided.
     * @param z The z-coordinate of the chunk. Defaults to 0 if not provided.
     * @return A mocked Chunk instance with the specified properties.
     */
    fun createMockChunk(world: World = createMockWorld(), x: Int = 0, z: Int = 0): Chunk {
        val chunk = mock(Chunk::class.java)
        `when`(chunk.world).thenReturn(world)
        `when`(chunk.x).thenReturn(x)
        `when`(chunk.z).thenReturn(z)
        return chunk
    }

    /**
     * Creates a MfPlayerId for testing.
     *
     * @param value The string value for the player ID. Defaults to a random UUID if not provided.
     * @return A MfPlayerId instance.
     */
    fun createPlayerId(value: String = UUID.randomUUID().toString()): MfPlayerId {
        return MfPlayerId(value)
    }

    /**
     * Creates a MfFactionId for testing.
     *
     * @return A MfFactionId instance with a generated UUID.
     */
    fun createFactionId(): MfFactionId {
        return MfFactionId.generate()
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
