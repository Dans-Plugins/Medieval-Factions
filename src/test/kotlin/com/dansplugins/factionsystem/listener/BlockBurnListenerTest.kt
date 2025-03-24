package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBurnEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlockBurnListenerTest {
    private lateinit var fixture: BlockBurnListenerTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var gateService: MfGateService
    private lateinit var uut: BlockBurnListener

    @BeforeEach
    fun setUp() {
        fixture = createFixture()
        plugin = mock(MedievalFactions::class.java)
        mockServices()
        uut = BlockBurnListener(plugin)
    }

    @Test
    fun onBlockBurn_ShouldCancelEvent_WhenBlockIsPartOfGate() {
        val block = mock(Block::class.java)
        val world = mock(World::class.java) // Mock the World
        val worldUid = mock(java.util.UUID::class.java)
        `when`(block.world).thenReturn(world) // Ensure getWorld() returns a non-null mock
        `when`(block.x).thenReturn(0)
        `when`(block.y).thenReturn(0)
        `when`(block.z).thenReturn(0)
        `when`(world.uid).thenReturn(worldUid) // Ensure getUID() returns a non-null mock

        val event = mock(BlockBurnEvent::class.java)
        `when`(event.block).thenReturn(block)
        val blockPosition = MfBlockPosition.fromBukkitBlock(block)
        `when`(gateService.getGatesAt(blockPosition)).thenReturn(listOf(mock(MfGate::class.java)))

        uut.onBlockBurn(event)

        verify(event, times(1)).isCancelled = true
    }

    @Test
    fun onBlockBurn_ShouldNotCancelEvent_WhenBlockIsNotPartOfGate() {
        val block = mock(Block::class.java)
        val world = mock(World::class.java) // Mock the World
        val worldUid = mock(java.util.UUID::class.java)
        `when`(block.world).thenReturn(world) // Ensure getWorld() returns a non-null mock
        `when`(block.x).thenReturn(0)
        `when`(block.y).thenReturn(0)
        `when`(block.z).thenReturn(0)
        `when`(world.uid).thenReturn(worldUid) // Ensure getUID() returns a non-null mock

        val event = mock(BlockBurnEvent::class.java)
        `when`(event.block).thenReturn(block)
        val blockPosition = MfBlockPosition.fromBukkitBlock(block)
        `when`(gateService.getGatesAt(blockPosition)).thenReturn(emptyList())

        uut.onBlockBurn(event)

        verify(event, times(0)).isCancelled = true
    }

    // Helper functions

    private fun createFixture(): BlockBurnListenerTestFixture {
        val block = mock(Block::class.java)
        val player = mock(org.bukkit.entity.Player::class.java)
        val event = mock(org.bukkit.event.block.BlockBurnEvent::class.java)
        return BlockBurnListenerTestFixture(block, player, event)
    }

    private data class BlockBurnListenerTestFixture(
        val block: Block,
        val player: Player,
        val event: BlockBurnEvent
    )

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        gateService = mock(MfGateService::class.java)
        `when`(services.gateService).thenReturn(gateService)
    }
}
