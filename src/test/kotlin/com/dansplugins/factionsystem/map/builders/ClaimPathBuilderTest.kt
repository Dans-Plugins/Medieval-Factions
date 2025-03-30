package com.dansplugins.factionsystem.map.builders

import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.map.aliases.Path
import com.dansplugins.factionsystem.map.aliases.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class ClaimPathBuilderTest {
    private lateinit var uut: ClaimPathBuilder

    @BeforeEach
    fun setUp() {
        uut = ClaimPathBuilder()
    }

    @Test
    fun testGetPaths_singleClaim() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(MfClaimedChunk(uuid, 0, 0, factionId))
        val paths = uut.getPaths(claims)
        val expectedPaths = listOf(
            listOf(Point(0, 0), Point(1, 0), Point(1, 1), Point(0, 1))
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_multipleClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 1, 0, factionId),
            MfClaimedChunk(uuid, 0, 1, factionId),
            MfClaimedChunk(uuid, 1, 1, factionId)
        )
        val paths = uut.getPaths(claims)
        val expectedPaths = listOf(
            listOf(
                Point(0, 0),
                Point(1, 0),
                Point(2, 0),
                Point(2, 1),
                Point(2, 2),
                Point(1, 2),
                Point(0, 2),
                Point(0, 1)
            )
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_noClaims() {
        val claims = emptyList<MfClaimedChunk>()
        val paths = uut.getPaths(claims)
        val expectedPaths = emptyList<Path>()
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_nonContiguousClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 2, 2, factionId)
        )
        val paths = uut.getPaths(claims)
        val expectedPaths = listOf(
            listOf(Point(0, 0), Point(1, 0), Point(1, 1), Point(0, 1)),
            listOf(Point(2, 2), Point(3, 2), Point(3, 3), Point(2, 3))
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_singleRowOfClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 1, 0, factionId),
            MfClaimedChunk(uuid, 2, 0, factionId)
        )
        val paths = uut.getPaths(claims)
        val expectedPaths = listOf(
            listOf(
                Point(0, 0),
                Point(1, 0),
                Point(2, 0),
                Point(3, 0),
                Point(3, 1),
                Point(2, 1),
                Point(1, 1),
                Point(0, 1)
            )
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_singleColumnOfClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 0, 1, factionId),
            MfClaimedChunk(uuid, 0, 2, factionId)
        )
        val paths = uut.getPaths(claims)
        val expectedPaths = listOf(
            listOf(
                Point(0, 0),
                Point(1, 0),
                Point(1, 1),
                Point(1, 2),
                Point(1, 3),
                Point(0, 3),
                Point(0, 2),
                Point(0, 1)
            )
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_LShapedClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 1, 0, factionId),
            MfClaimedChunk(uuid, 1, 1, factionId)
        )
        val paths = uut.getPaths(claims)
        val expectedPaths = listOf(
            listOf(
                Point(0, 0),
                Point(1, 0),
                Point(2, 0),
                Point(2, 1),
                Point(2, 2),
                Point(1, 2),
                Point(1, 1),
                Point(0, 1)
            )
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_diagonalClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 1, 1, factionId),
            MfClaimedChunk(uuid, 2, 2, factionId)
        )
        val paths = uut.getPaths(claims)
        val expectedPaths = listOf(
            listOf(Point(0, 0), Point(1, 0), Point(1, 1), Point(0, 1)),
            listOf(Point(1, 1), Point(2, 1), Point(2, 2), Point(1, 2)),
            listOf(Point(2, 2), Point(3, 2), Point(3, 3), Point(2, 3))
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_complexClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 1, 0, factionId),
            MfClaimedChunk(uuid, 2, 0, factionId),
            MfClaimedChunk(uuid, 2, 1, factionId),
            MfClaimedChunk(uuid, 2, 2, factionId),
            MfClaimedChunk(uuid, 1, 2, factionId),
            MfClaimedChunk(uuid, 0, 2, factionId),
            MfClaimedChunk(uuid, 0, 1, factionId),
            MfClaimedChunk(uuid, 3, 0, factionId),
            MfClaimedChunk(uuid, 3, 1, factionId),
            MfClaimedChunk(uuid, 3, 2, factionId),
            MfClaimedChunk(uuid, 4, 2, factionId)
        )
        val paths = uut.getPaths(claims)
        val expectedPaths = listOf(
            listOf(
                Point(0, 0),
                Point(1, 0),
                Point(2, 0),
                Point(3, 0),
                Point(4, 0),
                Point(4, 1),
                Point(4, 2),
                Point(5, 2),
                Point(5, 3),
                Point(4, 3),
                Point(3, 3),
                Point(2, 3),
                Point(1, 3),
                Point(0, 3),
                Point(0, 2),
                Point(0, 1)
            ),
            listOf(Point(1, 1), Point(1, 2), Point(2, 2), Point(2, 1))
        )
        assertEquals(expectedPaths, paths)
    }
}
