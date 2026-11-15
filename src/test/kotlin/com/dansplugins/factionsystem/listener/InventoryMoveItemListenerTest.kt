package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.locks.MfLockedBlock
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.service.Services
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.Hopper
import org.bukkit.configuration.file.FileConfiguration
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
    private lateinit var config: FileConfiguration
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

    @Test
    fun onInventoryMoveItem_SameOwnerLockedChestAndHopper_OptionEnabled_ShouldNotCancelEvent() {
        // Arrange
        enableSameOwnerTransfers()
        val transfer = createChestToHopperFixture()
        stubLock(transfer.chestBlock, OWNER)
        stubLock(transfer.hopperBlock, OWNER)

        // Act
        uut.onInventoryMoveItem(transfer.event)

        // Assert
        verify(transfer.event, never()).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_SameOwnerLockedHopperIntoChest_OptionEnabled_ShouldNotCancelEvent() {
        // Arrange
        enableSameOwnerTransfers()
        val transfer = createChestToHopperFixture(hopperIsSource = true)
        stubLock(transfer.chestBlock, OWNER)
        stubLock(transfer.hopperBlock, OWNER)

        // Act
        uut.onInventoryMoveItem(transfer.event)

        // Assert
        verify(transfer.event, never()).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_SameOwnerLockedChestAndHopper_OptionDisabled_ShouldCancelEvent() {
        // Arrange
        val transfer = createChestToHopperFixture()
        stubLock(transfer.chestBlock, OWNER)
        stubLock(transfer.hopperBlock, OWNER)

        // Act
        uut.onInventoryMoveItem(transfer.event)

        // Assert
        verify(transfer.event).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_DifferentOwnerLocks_OptionEnabled_ShouldCancelEvent() {
        // Arrange
        enableSameOwnerTransfers()
        val transfer = createChestToHopperFixture()
        stubLock(transfer.chestBlock, OWNER)
        stubLock(transfer.hopperBlock, OTHER_PLAYER)

        // Act
        uut.onInventoryMoveItem(transfer.event)

        // Assert
        verify(transfer.event).isCancelled = true
    }

    @Test
    fun onInventoryMoveItem_LockedChestUnlockedHopper_OptionEnabled_ShouldCancelEvent() {
        // Arrange
        enableSameOwnerTransfers()
        val transfer = createChestToHopperFixture()
        stubLock(transfer.chestBlock, OWNER)

        // Act
        uut.onInventoryMoveItem(transfer.event)

        // Assert
        verify(transfer.event).isCancelled = true
    }

    // Helper functions

    private fun enableSameOwnerTransfers() {
        `when`(config.getBoolean("factions.allowHopperTransfersBetweenSameOwnerLocks")).thenReturn(true)
    }

    private fun stubLock(block: Block, owner: MfPlayerId) {
        val position = MfBlockPosition.fromBukkitBlock(block)
        val lockedBlock = MfLockedBlock(
            block = position,
            chunkX = 0,
            chunkZ = 0,
            playerId = owner,
            accessors = emptyList()
        )
        `when`(lockService.getLockedBlock(position)).thenReturn(lockedBlock)
    }

    // A chest at y=64 and a hopper below it at y=63, so the two blocks have distinct positions.
    private fun createChestToHopperFixture(hopperIsSource: Boolean = false): ChestHopperTransfer {
        val world = testUtils.createMockWorld()
        val chestBlock = testUtils.createMockBlock(world, 0, 64, 0)
        val hopperBlock = testUtils.createMockBlock(world, 0, 63, 0)

        val chest = mock(Chest::class.java)
        val chestInventory = mock(Inventory::class.java)
        `when`(chest.block).thenReturn(chestBlock)
        `when`(chestInventory.holder).thenReturn(chest)

        val hopper = mock(Hopper::class.java)
        val hopperInventory = mock(Inventory::class.java)
        `when`(hopper.block).thenReturn(hopperBlock)
        `when`(hopperInventory.holder).thenReturn(hopper)

        val event = mock(InventoryMoveItemEvent::class.java)
        `when`(event.source).thenReturn(if (hopperIsSource) hopperInventory else chestInventory)
        `when`(event.destination).thenReturn(if (hopperIsSource) chestInventory else hopperInventory)
        `when`(event.item).thenReturn(mock(ItemStack::class.java))

        return ChestHopperTransfer(chestBlock, hopperBlock, event)
    }

    private data class ChestHopperTransfer(
        val chestBlock: Block,
        val hopperBlock: Block,
        val event: InventoryMoveItemEvent
    )

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
        val destinationBlock: Block?
    )

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        lockService = mock(MfLockService::class.java)
        `when`(services.lockService).thenReturn(lockService)

        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
    }

    companion object {
        private val OWNER = MfPlayerId("00000000-0000-0000-0000-000000000001")
        private val OTHER_PLAYER = MfPlayerId("00000000-0000-0000-0000-000000000002")
    }
}
