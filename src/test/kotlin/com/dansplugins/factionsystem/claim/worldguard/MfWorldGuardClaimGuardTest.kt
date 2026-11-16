package com.dansplugins.factionsystem.claim.worldguard

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunkRepository
import com.dansplugins.factionsystem.claim.MfRegionClaimGuard
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.protection.ApplicableRegionSet
import com.sk89q.worldguard.protection.flags.Flag
import com.sk89q.worldguard.protection.flags.StateFlag
import com.sk89q.worldguard.protection.flags.StringFlag
import com.sk89q.worldguard.protection.managers.RegionManager
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

/**
 * Covers the WorldGuard claim block (#1630): which regions block a claim, the area queried for a chunk, and the
 * `worldGuard.blockClaimsInRegions` switch in [MfClaimService]. WorldGuard itself is replaced by a mocked
 * [RegionManager], so no server or WorldGuard instance is needed.
 */
class MfWorldGuardClaimGuardTest {
    private val worldId = UUID.randomUUID()

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var world: World
    private lateinit var regionManager: RegionManager
    private lateinit var applicableRegions: ApplicableRegionSet

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        world = mock(World::class.java)
        `when`(world.uid).thenReturn(worldId)
        `when`(world.minHeight).thenReturn(-64)
        `when`(world.maxHeight).thenReturn(320)
        `when`(server.getWorld(worldId)).thenReturn(world)
        regionManager = mock(RegionManager::class.java)
        applicableRegions = mock(ApplicableRegionSet::class.java)
        `when`(regionManager.getApplicableRegions(org.mockito.ArgumentMatchers.any(ProtectedRegion::class.java))).thenReturn(applicableRegions)
        `when`(applicableRegions.regions).thenReturn(emptySet())
    }

    // isBlockingRegion

    @Test
    fun anOrdinaryRegionBlocksClaims() {
        assertTrue(MfWorldGuardClaimGuard.isBlockingRegion(region("spawn"), emptySet(), emptySet()))
    }

    @Test
    fun theGlobalRegionNeverBlocksClaims() {
        val global = GlobalProtectedRegion(ProtectedRegion.GLOBAL_REGION)

        assertFalse(MfWorldGuardClaimGuard.isBlockingRegion(global, emptySet(), emptySet()))
    }

    @Test
    fun anExemptRegionIdDoesNotBlockClaims() {
        assertFalse(MfWorldGuardClaimGuard.isBlockingRegion(region("Market"), setOf("market"), emptySet()))
        assertTrue(MfWorldGuardClaimGuard.isBlockingRegion(region("spawn"), setOf("market"), emptySet()))
    }

    @Test
    fun anExemptStateFlagOnlyExemptsTheRegionWhenSetToAllow() {
        val build = StateFlag("build", false)

        assertFalse(MfWorldGuardClaimGuard.isBlockingRegion(region("farms", mapOf(build to StateFlag.State.ALLOW)), emptySet(), setOf("build")))
        assertTrue(MfWorldGuardClaimGuard.isBlockingRegion(region("farms", mapOf(build to StateFlag.State.DENY)), emptySet(), setOf("build")))
        assertTrue(MfWorldGuardClaimGuard.isBlockingRegion(region("farms"), emptySet(), setOf("build")))
    }

    @Test
    fun anExemptNonStateFlagExemptsTheRegionWhenSetToAnyValue() {
        val greeting = StringFlag("greeting")

        assertFalse(MfWorldGuardClaimGuard.isBlockingRegion(region("town", mapOf(greeting to "Welcome")), emptySet(), setOf("greeting")))
    }

    @Test
    fun aFlagThatIsNotListedDoesNotExemptTheRegion() {
        val greeting = StringFlag("greeting")

        assertTrue(MfWorldGuardClaimGuard.isBlockingRegion(region("town", mapOf(greeting to "Welcome")), emptySet(), setOf("build")))
    }

    // overlapsProtectedRegion

    @Test
    fun theWholeChunkColumnIsQueried() {
        val captor = ArgumentCaptor.forClass(ProtectedRegion::class.java)

        guard().overlapsProtectedRegion(MfChunkPosition(worldId, 2, -3))

        verify(regionManager).getApplicableRegions(captor.capture())
        assertEquals(BlockVector3.at(32, -64, -48), captor.value.minimumPoint)
        assertEquals(BlockVector3.at(47, 319, -33), captor.value.maximumPoint)
    }

    @Test
    fun aChunkOverlappingARegionIsReported() {
        val spawn = region("spawn")
        `when`(applicableRegions.regions).thenReturn(setOf(spawn))

        assertTrue(guard().overlapsProtectedRegion(MfChunkPosition(worldId, 0, 0)))
    }

    @Test
    fun aChunkOverlappingOnlyExemptRegionsIsNotReported() {
        val market = region("market")
        `when`(applicableRegions.regions).thenReturn(setOf(market))
        `when`(config.getStringList("worldGuard.exemptRegions")).thenReturn(listOf("MARKET"))

        assertFalse(guard().overlapsProtectedRegion(MfChunkPosition(worldId, 0, 0)))
    }

    @Test
    fun aChunkOutsideEveryRegionIsNotReported() {
        assertFalse(guard().overlapsProtectedRegion(MfChunkPosition(worldId, 0, 0)))
    }

    @Test
    fun aWorldWithoutARegionManagerIsNotReported() {
        val uut = MfWorldGuardClaimGuard(plugin) { null }

        assertFalse(uut.overlapsProtectedRegion(MfChunkPosition(worldId, 0, 0)))
    }

    @Test
    fun anUnloadedWorldIsNotQueried() {
        assertFalse(guard().overlapsProtectedRegion(MfChunkPosition(UUID.randomUUID(), 0, 0)))
        verify(regionManager, never()).getApplicableRegions(org.mockito.ArgumentMatchers.any(ProtectedRegion::class.java))
    }

    // MfClaimService.isClaimBlockedByWorldGuard

    @Test
    fun claimServiceIgnoresRegionsWhenTheOptionIsOff() {
        val regionGuard = mock(MfRegionClaimGuard::class.java)
        val chunk = MfChunkPosition(worldId, 0, 0)
        `when`(regionGuard.overlapsProtectedRegion(chunk)).thenReturn(true)
        `when`(config.getBoolean("worldGuard.blockClaimsInRegions")).thenReturn(false)

        assertFalse(claimService(regionGuard).isClaimBlockedByWorldGuard(chunk))
        verify(regionGuard, never()).overlapsProtectedRegion(chunk)
    }

    @Test
    fun claimServiceRefusesARegionChunkWhenTheOptionIsOn() {
        val regionGuard = mock(MfRegionClaimGuard::class.java)
        val chunk = MfChunkPosition(worldId, 0, 0)
        `when`(regionGuard.overlapsProtectedRegion(chunk)).thenReturn(true)
        `when`(config.getBoolean("worldGuard.blockClaimsInRegions")).thenReturn(true)

        assertTrue(claimService(regionGuard).isClaimBlockedByWorldGuard(chunk))
        assertFalse(claimService(regionGuard).isClaimBlockedByWorldGuard(MfChunkPosition(worldId, 5, 5)))
    }

    @Test
    fun claimServiceAllowsClaimsWhenWorldGuardIsNotInstalled() {
        `when`(config.getBoolean("worldGuard.blockClaimsInRegions")).thenReturn(true)

        assertFalse(claimService(null).isClaimBlockedByWorldGuard(MfChunkPosition(worldId, 0, 0)))
    }

    private fun guard() = MfWorldGuardClaimGuard(plugin) { requestedWorld -> if (requestedWorld == world) regionManager else null }

    private fun claimService(regionGuard: MfRegionClaimGuard?): MfClaimService {
        val repository = mock(MfClaimedChunkRepository::class.java)
        `when`(repository.getClaims()).thenReturn(emptyList())
        return MfClaimService(plugin, repository, regionGuard)
    }

    private fun region(id: String, flags: Map<Flag<*>, Any> = emptyMap()): ProtectedRegion {
        val region = mock(ProtectedRegion::class.java)
        `when`(region.id).thenReturn(id)
        `when`(region.flags).thenReturn(flags)
        return region
    }
}
