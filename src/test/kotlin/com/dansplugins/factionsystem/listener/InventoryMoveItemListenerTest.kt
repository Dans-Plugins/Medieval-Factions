package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.locks.MfLockedBlock
import com.dansplugins.factionsystem.service.Services
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InventoryMoveItemListenerTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: InventoryMoveItemListenerTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var lockService: MfLockService
    private lateinit var uut: InventoryMoveItemListener

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        mockServices()
        uut = InventoryMoveItemListener(plugin)
    }

    @Test
    fun onInventoryMoveItem_SourceIsLockedSingleChest_ShouldCancelEvent() {
        // Arrange
        fixture = createSingleChestFixture()
        val event = fixture.event
        val sourceBlock = fixture.sourceBlock

        val blockPosition = MfBlockPosition.fromBukkitBlock(sourceBlock)
        val lockedBlock = mock(MfLockedBlock::class.java)
        `when`(lockService.getLockedBlock(blockPosition)).thenReturn(lockedBlock)

        // Act
        uut.onInventoryMoveItem(event)

        // Assert
        verify(event).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_SourceIsUnlockedSingleChest_ShouldNotCancelEvent() {
        // Arrange
        fixture = createSingleChestFixture()
        val event = fixture.event
        val sourceBlock = fixture.sourceBlock

        val blockPosition = MfBlockPosition.fromBukkitBlock(sourceBlock)
        `when`(lockService.getLockedBlock(blockPosition)).thenReturn(null)

        // Act
        uut.onInventoryMoveItem(event)

        // Assert
        verify(event, never()).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_SourceIsLockedDoubleChest_LeftSideLocked_ShouldCancelEvent() {
        // Arrange
        fixture = createDoubleChestFixture()
        val event = fixture.event
        val leftBlock = fixture.leftBlock!!

        val leftBlockPosition = MfBlockPosition.fromBukkitBlock(leftBlock)
        val lockedBlock = mock(MfLockedBlock::class.java)
        `when`(lockService.getLockedBlock(leftBlockPosition)).thenReturn(lockedBlock)

        // Act
        uut.onInventoryMoveItem(event)

        // Assert
        verify(event).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_SourceIsLockedDoubleChest_RightSideLocked_ShouldCancelEvent() {
        // Arrange
        fixture = createDoubleChestFixture()
        val event = fixture.event
        val rightBlock = fixture.rightBlock!!

        val rightBlockPosition = MfBlockPosition.fromBukkitBlock(rightBlock)
        val lockedBlock = mock(MfLockedBlock::class.java)
        `when`(lockService.getLockedBlock(rightBlockPosition)).thenReturn(lockedBlock)

        // Act
        uut.onInventoryMoveItem(event)

        // Assert
        verify(event).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_SourceIsUnlockedDoubleChest_ShouldNotCancelEvent() {
        // Arrange
        fixture = createDoubleChestFixture()
        val event = fixture.event
        val leftBlock = fixture.leftBlock!!
        val rightBlock = fixture.rightBlock!!

        val leftBlockPosition = MfBlockPosition.fromBukkitBlock(leftBlock)
        val rightBlockPosition = MfBlockPosition.fromBukkitBlock(rightBlock)
        `when`(lockService.getLockedBlock(leftBlockPosition)).thenReturn(null)
        `when`(lockService.getLockedBlock(rightBlockPosition)).thenReturn(null)

        // Act
        uut.onInventoryMoveItem(event)

        // Assert
        verify(event, never()).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_SourceIsNotBlockInventoryHolder_ShouldReturn() {
        // Arrange
        val sourceInventory = mock(Inventory::class.java)
        val destinationInventory = mock(Inventory::class.java)
        val item = mock(ItemStack::class.java)
        val event = mock(InventoryMoveItemEvent::class.java)

        // Source holder is not a BlockInventoryHolder or DoubleChest
        val sourceHolder = mock(InventoryHolder::class.java)
        `when`(sourceInventory.holder).thenReturn(sourceHolder)
        `when`(event.source).thenReturn(sourceInventory)
        `when`(event.destination).thenReturn(destinationInventory)
        `when`(event.item).thenReturn(item)

        // Act
        uut.onInventoryMoveItem(event)

        // Assert
        verify(event, never()).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_DestinationIsLockedSingleChest_ShouldCancelEvent() {
        // Arrange
        fixture = createSingleChestDestinationFixture()
        val event = fixture.event
        val destinationBlock = fixture.destinationBlock!!

        val blockPosition = MfBlockPosition.fromBukkitBlock(destinationBlock)
        val lockedBlock = mock(MfLockedBlock::class.java)
        `when`(lockService.getLockedBlock(blockPosition)).thenReturn(lockedBlock)

        // Act
        uut.onInventoryMoveItem(event)

        // Assert
        verify(event).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_DestinationIsUnlockedSingleChest_ShouldNotCancelEvent() {
        // Arrange
        fixture = createSingleChestDestinationFixture()
        val event = fixture.event
        val destinationBlock = fixture.destinationBlock!!

        val blockPosition = MfBlockPosition.fromBukkitBlock(destinationBlock)
        `when`(lockService.getLockedBlock(blockPosition)).thenReturn(null)

        // Act
        uut.onInventoryMoveItem(event)

        // Assert
        verify(event, never()).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_DestinationIsLockedDoubleChest_LeftSideLocked_ShouldCancelEvent() {
        // Arrange
        fixture = createDoubleChestDestinationFixture()
        val event = fixture.event
        val leftBlock = fixture.leftBlock!!

        val leftBlockPosition = MfBlockPosition.fromBukkitBlock(leftBlock)
        val lockedBlock = mock(MfLockedBlock::class.java)
        `when`(lockService.getLockedBlock(leftBlockPosition)).thenReturn(lockedBlock)

        // Act
        uut.onInventoryMoveItem(event)

        // Assert
        verify(event).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_DestinationIsLockedDoubleChest_RightSideLocked_ShouldCancelEvent() {
        // Arrange
        fixture = createDoubleChestDestinationFixture()
        val event = fixture.event
        val rightBlock = fixture.rightBlock!!

        val rightBlockPosition = MfBlockPosition.fromBukkitBlock(rightBlock)
        val lockedBlock = mock(MfLockedBlock::class.java)
        `when`(lockService.getLockedBlock(rightBlockPosition)).thenReturn(lockedBlock)

        // Act
        uut.onInventoryMoveItem(event)

        // Assert
        verify(event).isCancelled = true
    }

    // Helper functions

    private fun createSingleChestFixture(): InventoryMoveItemListenerTestFixture {
        val world = testUtils.createMockWorld()
        val sourceBlock = testUtils.createMockBlock(world)

        val sourceChest = mock(Chest::class.java)
        val sourceInventory = mock(Inventory::class.java)
        `when`(sourceChest.block).thenReturn(sourceBlock)
        `when`(sourceChest.inventory).thenReturn(sourceInventory)
        `when`(sourceInventory.holder).thenReturn(sourceChest)

        val destinationBlock = testUtils.createMockBlock(world)
        val destinationHolder = mock(BlockInventoryHolder::class.java)
        val destinationInventory = mock(Inventory::class.java)
        `when`(destinationHolder.block).thenReturn(destinationBlock)
        `when`(destinationInventory.holder).thenReturn(destinationHolder)

        val item = mock(ItemStack::class.java)
        val event = mock(InventoryMoveItemEvent::class.java)
        `when`(event.source).thenReturn(sourceInventory)
        `when`(event.destination).thenReturn(destinationInventory)
        `when`(event.item).thenReturn(item)

        return InventoryMoveItemListenerTestFixture(world, sourceBlock, event, null, null, null)
    }

    private fun createDoubleChestFixture(): InventoryMoveItemListenerTestFixture {
        val world = testUtils.createMockWorld()
        val leftBlock = testUtils.createMockBlock(world)
        val rightBlock = testUtils.createMockBlock(world)

        val leftChest = mock(Chest::class.java)
        val rightChest = mock(Chest::class.java)
        `when`(leftChest.block).thenReturn(leftBlock)
        `when`(rightChest.block).thenReturn(rightBlock)

        val doubleChest = mock(DoubleChest::class.java)
        `when`(doubleChest.leftSide).thenReturn(leftChest)
        `when`(doubleChest.rightSide).thenReturn(rightChest)

        val sourceInventory = mock(Inventory::class.java)
        `when`(sourceInventory.holder).thenReturn(doubleChest)

        val destinationBlock = testUtils.createMockBlock(world)
        val destinationHolder = mock(BlockInventoryHolder::class.java)
        val destinationInventory = mock(Inventory::class.java)
        `when`(destinationHolder.block).thenReturn(destinationBlock)
        `when`(destinationInventory.holder).thenReturn(destinationHolder)

        val item = mock(ItemStack::class.java)
        val event = mock(InventoryMoveItemEvent::class.java)
        `when`(event.source).thenReturn(sourceInventory)
        `when`(event.destination).thenReturn(destinationInventory)
        `when`(event.item).thenReturn(item)

        return InventoryMoveItemListenerTestFixture(world, leftBlock, event, leftBlock, rightBlock, null)
    }

    private fun createSingleChestDestinationFixture(): InventoryMoveItemListenerTestFixture {
        val world = testUtils.createMockWorld()
        val destinationBlock = testUtils.createMockBlock(world)

        val destinationChest = mock(Chest::class.java)
        val destinationInventory = mock(Inventory::class.java)
        `when`(destinationChest.block).thenReturn(destinationBlock)
        `when`(destinationChest.inventory).thenReturn(destinationInventory)
        `when`(destinationInventory.holder).thenReturn(destinationChest)

        val sourceBlock = testUtils.createMockBlock(world)
        val sourceHolder = mock(BlockInventoryHolder::class.java)
        val sourceInventory = mock(Inventory::class.java)
        `when`(sourceHolder.block).thenReturn(sourceBlock)
        `when`(sourceInventory.holder).thenReturn(sourceHolder)

        val item = mock(ItemStack::class.java)
        val event = mock(InventoryMoveItemEvent::class.java)
        `when`(event.source).thenReturn(sourceInventory)
        `when`(event.destination).thenReturn(destinationInventory)
        `when`(event.item).thenReturn(item)

        return InventoryMoveItemListenerTestFixture(world, sourceBlock, event, null, null, destinationBlock)
    }

    private fun createDoubleChestDestinationFixture(): InventoryMoveItemListenerTestFixture {
        val world = testUtils.createMockWorld()
        val leftBlock = testUtils.createMockBlock(world)
        val rightBlock = testUtils.createMockBlock(world)

        val leftChest = mock(Chest::class.java)
        val rightChest = mock(Chest::class.java)
        `when`(leftChest.block).thenReturn(leftBlock)
        `when`(rightChest.block).thenReturn(rightBlock)

        val doubleChest = mock(DoubleChest::class.java)
        `when`(doubleChest.leftSide).thenReturn(leftChest)
        `when`(doubleChest.rightSide).thenReturn(rightChest)

        val destinationInventory = mock(Inventory::class.java)
        `when`(destinationInventory.holder).thenReturn(doubleChest)

        val sourceBlock = testUtils.createMockBlock(world)
        val sourceHolder = mock(BlockInventoryHolder::class.java)
        val sourceInventory = mock(Inventory::class.java)
        `when`(sourceHolder.block).thenReturn(sourceBlock)
        `when`(sourceInventory.holder).thenReturn(sourceHolder)

        val item = mock(ItemStack::class.java)
        val event = mock(InventoryMoveItemEvent::class.java)
        `when`(event.source).thenReturn(sourceInventory)
        `when`(event.destination).thenReturn(destinationInventory)
        `when`(event.item).thenReturn(item)

        return InventoryMoveItemListenerTestFixture(world, sourceBlock, event, leftBlock, rightBlock, null)
    }

    private data class InventoryMoveItemListenerTestFixture(
        val world: World,
        val sourceBlock: Block,
        val event: InventoryMoveItemEvent,
        val leftBlock: Block?,
        val rightBlock: Block?,
        val destinationBlock: Block?,
    )

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        lockService = mock(MfLockService::class.java)
        `when`(services.lockService).thenReturn(lockService)
    }
}
