package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.service.Services
import org.bukkit.block.BlockState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class GrowthBorderTrimmerTest {
    private val worldId = UUID.randomUUID()
    private val alpha = MfFactionId("alpha")
    private val bravo = MfFactionId("bravo")
    private lateinit var claimService: MfClaimService
    private lateinit var uut: GrowthBorderTrimmer

    @BeforeEach
    fun setUp() {
        val plugin = mock(MedievalFactions::class.java)
        val services = mock(Services::class.java)
        claimService = mock(MfClaimService::class.java)
        `when`(plugin.services).thenReturn(services)
        `when`(services.claimService).thenReturn(claimService)
        uut = GrowthBorderTrimmer(plugin)
    }

    private fun claim(chunkX: Int, chunkZ: Int, faction: MfFactionId) {
        val c = mock(MfClaimedChunk::class.java)
        `when`(c.factionId).thenReturn(faction)
        `when`(claimService.getClaim(worldId, chunkX, chunkZ)).thenReturn(c)
    }

    private fun state(x: Int, z: Int): BlockState {
        val s = mock(BlockState::class.java)
        `when`(s.x).thenReturn(x)
        `when`(s.z).thenReturn(z)
        return s
    }

    @Test
    fun trim_GrowthFromAClaim_ShouldDropOnlyBlocksInAnotherFactionsClaim() {
        // origin in Alpha's chunk (0,0); chunk (1,0) is Bravo's; chunk (0,1) is wilderness
        claim(0, 0, alpha)
        claim(1, 0, bravo)
        val own = state(15, 3)
        val foreign = state(16, 3)
        val wild = state(5, 16)
        val blocks = mutableListOf(own, foreign, wild)

        val removed = uut.trim(worldId, 8, 8, blocks)

        assertEquals(1, removed)
        assertEquals(listOf(own, wild), blocks)
    }

    @Test
    fun trim_GrowthFromAClaim_ShouldKeepBlocksInTheSameFactionsOtherChunks() {
        claim(0, 0, alpha)
        claim(-1, 0, alpha)
        val neighbour = state(-1, 4)
        val blocks = mutableListOf(neighbour)

        assertEquals(0, uut.trim(worldId, 2, 2, blocks))
        assertEquals(listOf(neighbour), blocks)
    }

    @Test
    fun trim_GrowthFromWilderness_ShouldDropEveryBlockInAnyClaim() {
        claim(1, 0, bravo)
        val wild = state(10, 10)
        val claimed = state(20, 10)
        val blocks = mutableListOf(wild, claimed)

        assertEquals(1, uut.trim(worldId, 10, 10, blocks))
        assertEquals(listOf(wild), blocks)
    }

    @Test
    fun trim_NegativeCoordinates_ShouldUseFloorChunks() {
        // x = -1 is chunk -1, not chunk 0: an arithmetic shift, not a division
        claim(-1, 0, bravo)
        val foreign = state(-1, 0)
        val blocks = mutableListOf(foreign)

        assertEquals(1, uut.trim(worldId, 0, 0, blocks))
    }
}
