package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateService
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.event.block.BlockBurnEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class BlockBurnListenerTest {

    private lateinit var medievalFactions: MedievalFactions
    private lateinit var gateService: MfGateService
    private lateinit var blockBurnListener: BlockBurnListener

    @BeforeEach
    fun setUp() {
        medievalFactions = mock(MedievalFactions::class.java)
        gateService = mock(MfGateService::class.java)

        // Mocking services object in MedievalFactions
        val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.gateService).thenReturn(gateService)

        blockBurnListener = BlockBurnListener(medievalFactions)
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

        blockBurnListener.onBlockBurn(event)

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

        blockBurnListener.onBlockBurn(event)

        verify(event, times(0)).isCancelled = true
    }
}
