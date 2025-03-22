package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
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

 private lateinit var medievalFactions: MedievalFactions
 private lateinit var gateService: MfGateService
 private lateinit var claimService: MfClaimService
 private lateinit var blockPlaceListener: BlockPlaceListener

 @BeforeEach
 fun setUp() {
  medievalFactions = mock(MedievalFactions::class.java)
  gateService = mock(MfGateService::class.java)
  claimService = mock(MfClaimService::class.java)

  // Mock services
  val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
  `when`(medievalFactions.services).thenReturn(services)
  `when`(services.gateService).thenReturn(gateService)
  `when`(services.claimService).thenReturn(claimService)

  // Mock language system
  val language = mock(Language::class.java)
  `when`(language["CannotPlaceBlockInGate"]).thenReturn("Cannot place block in gate")
  `when`(language["CannotPlaceBlockInWilderness"]).thenReturn("Cannot place block in wilderness")
  `when`(medievalFactions.language).thenReturn(language)

  blockPlaceListener = BlockPlaceListener(medievalFactions)
 }

 @Test
 fun onBlockPlace_BlockIsInGate_ShouldCancelAndInformPlayer() {
  // Arrange
  val block = mock(Block::class.java)
  val world = mock(World::class.java)
  val player = mock(Player::class.java)
  val worldUid = mock(java.util.UUID::class.java)

  `when`(block.world).thenReturn(world)
  `when`(world.uid).thenReturn(worldUid)
  `when`(block.x).thenReturn(0)
  `when`(block.y).thenReturn(0)
  `when`(block.z).thenReturn(0)

  val event = mock(BlockPlaceEvent::class.java)
  `when`(event.block).thenReturn(block)
  `when`(event.player).thenReturn(player)

  val blockPosition = MfBlockPosition.fromBukkitBlock(block)
  `when`(gateService.getGatesAt(blockPosition)).thenReturn(listOf(mock(MfGate::class.java)))

  // Act
  blockPlaceListener.onBlockPlace(event)

  // Assert
  verify(event).isCancelled = true
  verify(player).sendMessage("${ChatColor.RED}Cannot place block in gate")
 }

 @Test
 fun onBlockPlace_BlockInWilderness_WildernessPreventBlockPlaceSetToTrue_ShouldCancelAndInformPlayer() {
  // Arrange
  val block = mock(Block::class.java)
  val world = mock(World::class.java)
  val player = mock(Player::class.java)
  val worldUid = mock(java.util.UUID::class.java)

  `when`(block.world).thenReturn(world)
  `when`(world.uid).thenReturn(worldUid)
  `when`(block.x).thenReturn(0)
  `when`(block.y).thenReturn(0)
  `when`(block.z).thenReturn(0)
  `when`(block.chunk).thenReturn(mock(org.bukkit.Chunk::class.java))
  `when`(claimService.getClaim(block.chunk)).thenReturn(null)

  val event = mock(BlockPlaceEvent::class.java)
  `when`(event.block).thenReturn(block)
  `when`(event.player).thenReturn(player)

  `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
  `when`(medievalFactions.config.getBoolean("wilderness.preventBlockPlace", false)).thenReturn(true)

  // Act
  blockPlaceListener.onBlockPlace(event)

  // Assert
  verify(event).isCancelled = true
  verify(player).sendMessage("${ChatColor.RED}Cannot place block in wilderness")
 }

 @Test
 fun onBlockPlace_BlockInWilderness_WildernessPreventBlockPlaceSetToFalse_ShouldReturn() {
  // Arrange
  val block = mock(Block::class.java)
  val world = mock(World::class.java)
  val player = mock(Player::class.java)
  val worldUid = mock(java.util.UUID::class.java)

  `when`(block.world).thenReturn(world)
  `when`(world.uid).thenReturn(worldUid)
  `when`(block.x).thenReturn(0)
  `when`(block.y).thenReturn(0)
  `when`(block.z).thenReturn(0)
  `when`(block.chunk).thenReturn(mock(org.bukkit.Chunk::class.java))
  `when`(claimService.getClaim(block.chunk)).thenReturn(null)

  val event = mock(BlockPlaceEvent::class.java)
  `when`(event.block).thenReturn(block)
  `when`(event.player).thenReturn(player)

  `when`(medievalFactions.config).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration::class.java))
  `when`(medievalFactions.config.getBoolean("wilderness.preventBlockPlace", false)).thenReturn(false)

  // Act
  blockPlaceListener.onBlockPlace(event)

  // Assert
  verify(event, never()).isCancelled = true
  verify(event, never()).isCancelled = false
 }
}