package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.gate.MfGateService
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.utils.MfServerVersion
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityExplodeEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.*

class EntityExplodeListenerTest {

    private val testUtils = TestUtils()

    private lateinit var medievalFactions: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var factionService: MfFactionService
    private lateinit var gateService: MfGateService
    private lateinit var uut: EntityExplodeListener
    private lateinit var savedVersionProvider: () -> String

    @BeforeEach
    fun setUp() {
        savedVersionProvider = MfServerVersion.versionProvider
        MfServerVersion.versionProvider = { "1.21-R0.1-SNAPSHOT" }
        MfServerVersion.resetForTesting()

        medievalFactions = mock(MedievalFactions::class.java)
        gateService = mock(MfGateService::class.java)
        playerService = mock(MfPlayerService::class.java)
        claimService = mock(MfClaimService::class.java)
        factionService = mock(MfFactionService::class.java)

        val services = mock(com.dansplugins.factionsystem.service.Services::class.java)
        `when`(medievalFactions.services).thenReturn(services)
        `when`(services.gateService).thenReturn(gateService)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.factionService).thenReturn(factionService)

        uut = EntityExplodeListener(medievalFactions)
    }

    @AfterEach
    fun tearDown() {
        MfServerVersion.versionProvider = savedVersionProvider
        MfServerVersion.resetForTesting()
    }

    @Test
    fun onEntityExplode_WindChargeInEnemyTerritory_ShouldRemoveProtectedBlocks() {
        val chunk = testUtils.createMockChunk()
        val block = createMockBlockInChunk(chunk)
        val entity = createWindChargeEntity()
        val event = createExplodeEvent(entity, listOf(block))

        val shooter = mockShooter(chunk, block, event)
        setupEnemyClaim(shooter, chunk)

        uut.onEntityExplode(event)

        assert(!event.blockList().contains(block))
    }

    @Test
    fun onEntityExplode_WindChargeInOwnTerritory_ShouldKeepBlocks() {
        val chunk = testUtils.createMockChunk()
        val block = createMockBlockInChunk(chunk)
        val entity = createWindChargeEntity()
        val event = createExplodeEvent(entity, listOf(block))

        val shooter = mockShooter(chunk, block, event)
        setupOwnClaim(shooter, chunk)

        uut.onEntityExplode(event)

        assert(event.blockList().contains(block))
    }

    @Test
    fun onEntityExplode_WindCharge_Pre121_ShouldNotCheck() {
        MfServerVersion.versionProvider = { "1.17-R0.1-SNAPSHOT" }
        MfServerVersion.resetForTesting()
        val chunk = testUtils.createMockChunk()
        val block = createMockBlockInChunk(chunk)
        val entity = createWindChargeEntity()
        val event = createExplodeEvent(entity, listOf(block))

        val shooter = mock(Player::class.java)
        `when`((entity as Projectile).shooter).thenReturn(shooter)

        uut.onEntityExplode(event)

        assert(event.blockList().contains(block))
        verifyNoInteractions(claimService)
    }

    @Test
    fun onEntityExplode_WindCharge_BypassEnabled_ShouldKeepBlocks() {
        val chunk = testUtils.createMockChunk()
        val block = createMockBlockInChunk(chunk)
        val entity = createWindChargeEntity()
        val event = createExplodeEvent(entity, listOf(block))

        val shooter = mock(Player::class.java)
        val shooterId = UUID.randomUUID()
        `when`(shooter.uniqueId).thenReturn(shooterId)
        `when`((entity as Projectile).shooter).thenReturn(shooter)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(shooterId.toString())
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(true)
        `when`(playerService.getPlayer(shooter)).thenReturn(mfPlayer)
        `when`(shooter.hasPermission("mf.bypass")).thenReturn(true)

        uut.onEntityExplode(event)

        assert(event.blockList().contains(block))
    }

    @Test
    fun onEntityExplode_WindChargeInUnclaimedTerritory_ShouldKeepBlocks() {
        val chunk = testUtils.createMockChunk()
        val block = createMockBlockInChunk(chunk)
        val entity = createWindChargeEntity()
        val event = createExplodeEvent(entity, listOf(block))

        val shooter = mockShooter(chunk, block, event)
        `when`(claimService.getClaim(chunk)).thenReturn(null)

        uut.onEntityExplode(event)

        assert(event.blockList().contains(block))
    }

    @Test
    fun onEntityExplode_TntInEnemyTerritory_ShouldNotAffectBlocks() {
        val chunk = testUtils.createMockChunk()
        val block = createMockBlockInChunk(chunk)
        val entity = mock(Entity::class.java)
        val entityType = mock(EntityType::class.java)
        `when`(entityType.name).thenReturn("PRIMED_TNT")
        `when`(entity.type).thenReturn(entityType)
        val event = createExplodeEvent(entity, listOf(block))

        uut.onEntityExplode(event)

        assert(event.blockList().contains(block))
    }

    @Test
    fun onEntityExplode_WindCharge_MixedBlocks_ShouldOnlyRemoveProtected() {
        val enemyChunk = testUtils.createMockChunk()
        val ownChunk = testUtils.createMockChunk()
        val enemyBlock = createMockBlockInChunk(enemyChunk)
        val ownBlock = createMockBlockInChunk(ownChunk)
        val entity = createWindChargeEntity()
        val event = createExplodeEvent(entity, mutableListOf(enemyBlock, ownBlock))

        val shooter = mock(Player::class.java)
        val shooterId = UUID.randomUUID()
        `when`(shooter.uniqueId).thenReturn(shooterId)
        `when`((entity as Projectile).shooter).thenReturn(shooter)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(shooterId.toString())
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(playerService.getPlayer(shooter)).thenReturn(mfPlayer)
        `when`(shooter.hasPermission("mf.bypass")).thenReturn(false)

        val enemyClaim = mock(MfClaimedChunk::class.java)
        val ownClaim = mock(MfClaimedChunk::class.java)
        `when`(claimService.getClaim(enemyChunk)).thenReturn(enemyClaim)
        `when`(claimService.getClaim(ownChunk)).thenReturn(ownClaim)
        `when`(claimService.isInteractionAllowed(playerId, enemyClaim)).thenReturn(false)
        `when`(claimService.isInteractionAllowed(playerId, ownClaim)).thenReturn(true)

        uut.onEntityExplode(event)

        assert(!event.blockList().contains(enemyBlock))
        assert(event.blockList().contains(ownBlock))
    }

    private fun createMockBlockInChunk(chunk: Chunk): Block {
        val world = testUtils.createMockWorld()
        val block = mock(Block::class.java)
        `when`(block.chunk).thenReturn(chunk)
        `when`(block.world).thenReturn(world)
        `when`(block.type).thenReturn(Material.OAK_DOOR)
        return block
    }

    private fun createWindChargeEntity(): Projectile {
        val entityType = mock(EntityType::class.java)
        `when`(entityType.name).thenReturn("WIND_CHARGE")
        val projectile = mock(Projectile::class.java)
        `when`(projectile.type).thenReturn(entityType)
        return projectile
    }

    private fun mockShooter(
        chunk: Chunk,
        block: Block,
        event: EntityExplodeEvent
    ): Player {
        val shooter = mock(Player::class.java)
        val shooterId = UUID.randomUUID()
        `when`(shooter.uniqueId).thenReturn(shooterId)
        `when`((event.entity as Projectile).shooter).thenReturn(shooter)

        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(shooterId.toString())
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(mfPlayer.isBypassEnabled).thenReturn(false)
        `when`(playerService.getPlayer(shooter)).thenReturn(mfPlayer)
        `when`(shooter.hasPermission("mf.bypass")).thenReturn(false)
        return shooter
    }

    private fun setupEnemyClaim(shooter: Player, chunk: Chunk) {
        val mfPlayer = playerService.getPlayer(shooter)!!
        val claim = mock(MfClaimedChunk::class.java)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(claimService.isInteractionAllowed(mfPlayer.id, claim)).thenReturn(false)
    }

    private fun setupOwnClaim(shooter: Player, chunk: Chunk) {
        val mfPlayer = playerService.getPlayer(shooter)!!
        val claim = mock(MfClaimedChunk::class.java)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(claimService.isInteractionAllowed(mfPlayer.id, claim)).thenReturn(true)
    }

    private fun createExplodeEvent(entity: Entity, blocks: List<Block>): EntityExplodeEvent {
        val event = mock(EntityExplodeEvent::class.java)
        `when`(event.entity).thenReturn(entity)
        val blockList = blocks.toMutableList() as MutableList<Block>
        `when`(event.blockList()).thenReturn(blockList)
        return event
    }
}
