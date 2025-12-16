package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateService
import com.dansplugins.factionsystem.lang.Language
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class BlockPlaceListenerTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: BlockPlaceListenerTestFixture
    private lateinit var medievalFactions: MedievalFactions
    private lateinit var gateService: MfGateService
    private lateinit var claimService: MfClaimService
    private lateinit var uut: BlockPlaceListener

    @BeforeEach
    fun setUp() {
        fixture = createBasicFixture()
        medievalFactions = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        uut = BlockPlaceListener(medievalFactions)
    }

    @Test
    fun onBlockPlace_BlockIsInGate_ShouldCancelAndInformPlayer() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        val blockPosition = MfBlockPosition.fromBukkitBlock(block)
        `when`(gateService.getGatesAt(blockPosition)).thenReturn(listOf(mock(MfGate::class.java)))

        // Act
        uut.onBlockPlace(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}Cannot place block in gate")
    }

    @Test
    fun onBlockPlace_BlockInWilderness_WildernessPreventBlockPlaceSetToTrue_ShouldCancelAndInformPlayer() {
        // Arrange
        val block = fixture.block
        val player = fixture.player
        val event = fixture.event

        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("wilderness.place.prevent", false)).thenReturn(true)
        `when`(medievalFactions.config.getBoolean("wilderness.place.alert", true)).thenReturn(true)

        // Act
        uut.onBlockPlace(event)

        // Assert
        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}Cannot place block in wilderness")
    }

    @Test
    fun onBlockPlace_BlockInWilderness_WildernessPreventBlockPlaceSetToFalse_ShouldReturn() {
        // Arrange
        val block = fixture.block
        val event = fixture.event

        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
        `when`(medievalFactions.config.getBoolean("wilderness.place.prevent", false)).thenReturn(false)

        // Act
        uut.onBlockPlace(event)

        // Assert
        verify(event, never()).isCancelled = true
        verify(event, never()).isCancelled = false
    }

    // Helper functions

    private fun createBasicFixture(): BlockPlaceListenerTestFixture {
        val world = testUtils.createMockWorld()
        val block = testUtils.createMockBlock(world)
        val player = mock(Player::class.java)
        val event = testUtils.createBlockPlaceEvent(block, player)
        return BlockPlaceListenerTestFixture(world, block, player, event)
    }

    private data class BlockPlaceListenerTestFixture(
        val world: World,
        val block: Block,
        val player: Player,
        val event: BlockPlaceEvent
    )

    private fun mockServices() {
        gateService = mock(MfGateService::class.java)
        claimService = mock(MfClaimService::class.java)

        val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.gateService).thenReturn(gateService)
        `when`(services.claimService).thenReturn(claimService)
    }

    private fun mockLanguageSystem() {
        val language = mock(Language::class.java)
        `when`(language["CannotPlaceBlockInGate"]).thenReturn("Cannot place block in gate")
        `when`(language["CannotPlaceBlockInWilderness"]).thenReturn("Cannot place block in wilderness")
        `when`(medievalFactions.language).thenReturn(language)
    }
}
