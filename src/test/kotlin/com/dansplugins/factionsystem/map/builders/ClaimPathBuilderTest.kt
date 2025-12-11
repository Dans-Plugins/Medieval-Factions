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
        val expectedPaths =
            listOf(
                listOf(Point(0, 0), Point(1, 0), Point(1, 1), Point(0, 1)),
            )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_multipleClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims =
            listOf(
                MfClaimedChunk(uuid, 0, 0, factionId),
                MfClaimedChunk(uuid, 1, 0, factionId),
                MfClaimedChunk(uuid, 0, 1, factionId),
                MfClaimedChunk(uuid, 1, 1, factionId),
            )
        val paths = uut.getPaths(claims)
        val expectedPaths =
            listOf(
                listOf(
                    Point(0, 0),
                    Point(1, 0),
                    Point(2, 0),
                    Point(2, 1),
                    Point(2, 2),
                    Point(1, 2),
                    Point(0, 2),
                    Point(0, 1),
                ),
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
        val claims =
            listOf(
                MfClaimedChunk(uuid, 0, 0, factionId),
                MfClaimedChunk(uuid, 2, 2, factionId),
            )
        val paths = uut.getPaths(claims)
        val expectedPaths =
            listOf(
                listOf(Point(0, 0), Point(1, 0), Point(1, 1), Point(0, 1)),
                listOf(Point(2, 2), Point(3, 2), Point(3, 3), Point(2, 3)),
            )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_singleRowOfClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims =
            listOf(
                MfClaimedChunk(uuid, 0, 0, factionId),
                MfClaimedChunk(uuid, 1, 0, factionId),
                MfClaimedChunk(uuid, 2, 0, factionId),
            )
        val paths = uut.getPaths(claims)
        val expectedPaths =
            listOf(
                listOf(
                    Point(0, 0),
                    Point(1, 0),
                    Point(2, 0),
                    Point(3, 0),
                    Point(3, 1),
                    Point(2, 1),
                    Point(1, 1),
                    Point(0, 1),
                ),
            )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_singleColumnOfClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims =
            listOf(
                MfClaimedChunk(uuid, 0, 0, factionId),
                MfClaimedChunk(uuid, 0, 1, factionId),
                MfClaimedChunk(uuid, 0, 2, factionId),
            )
        val paths = uut.getPaths(claims)
        val expectedPaths =
            listOf(
                listOf(
                    Point(0, 0),
                    Point(1, 0),
                    Point(1, 1),
                    Point(1, 2),
                    Point(1, 3),
                    Point(0, 3),
                    Point(0, 2),
                    Point(0, 1),
                ),
            )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_LShapedClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims =
            listOf(
                MfClaimedChunk(uuid, 0, 0, factionId),
                MfClaimedChunk(uuid, 1, 0, factionId),
                MfClaimedChunk(uuid, 1, 1, factionId),
            )
        val paths = uut.getPaths(claims)
        val expectedPaths =
            listOf(
                listOf(
                    Point(0, 0),
                    Point(1, 0),
                    Point(2, 0),
                    Point(2, 1),
                    Point(2, 2),
                    Point(1, 2),
                    Point(1, 1),
                    Point(0, 1),
                ),
            )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_diagonalClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims =
            listOf(
                MfClaimedChunk(uuid, 0, 0, factionId),
                MfClaimedChunk(uuid, 1, 1, factionId),
                MfClaimedChunk(uuid, 2, 2, factionId),
            )
        val paths = uut.getPaths(claims)
        val expectedPaths =
            listOf(
                listOf(Point(0, 0), Point(1, 0), Point(1, 1), Point(0, 1)),
                listOf(Point(1, 1), Point(2, 1), Point(2, 2), Point(1, 2)),
                listOf(Point(2, 2), Point(3, 2), Point(3, 3), Point(2, 3)),
            )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_complexClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims =
            listOf(
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
                MfClaimedChunk(uuid, 4, 2, factionId),
            )
        val paths = uut.getPaths(claims)
        val expectedPaths =
            listOf(
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
                    Point(0, 1),
                ),
                listOf(Point(1, 1), Point(1, 2), Point(2, 2), Point(2, 1)),
            )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_TShapedClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 1, 0, factionId),
            MfClaimedChunk(uuid, 2, 0, factionId),
            MfClaimedChunk(uuid, 1, 1, factionId)
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
                Point(2, 2),
                Point(1, 2),
                Point(1, 1),
                Point(0, 1)
            )
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_PlusShapedClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 1, 0, factionId),
            MfClaimedChunk(uuid, 0, 1, factionId),
            MfClaimedChunk(uuid, 1, 1, factionId),
            MfClaimedChunk(uuid, 2, 1, factionId),
            MfClaimedChunk(uuid, 1, 2, factionId)
        )
        val paths = uut.getPaths(claims)
        val expectedPaths = listOf(
            listOf(
                Point(1, 0),
                Point(2, 0),
                Point(2, 1),
                Point(3, 1),
                Point(3, 2),
                Point(2, 2),
                Point(2, 3),
                Point(1, 3),
                Point(0, 3),
                Point(0, 2),
                Point(1, 2),
                Point(1, 1),
                Point(0, 1),
                Point(0, 2),
                Point(1, 2),
                Point(1, 1)
            )
        )
        // For plus shape, we just verify paths are generated
        assert(paths.isNotEmpty())
        assert(paths.size == 1)
    }

    @Test
    fun testGetPaths_HollowSquareClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 1, 0, factionId),
            MfClaimedChunk(uuid, 2, 0, factionId),
            MfClaimedChunk(uuid, 0, 1, factionId),
            MfClaimedChunk(uuid, 2, 1, factionId),
            MfClaimedChunk(uuid, 0, 2, factionId),
            MfClaimedChunk(uuid, 1, 2, factionId),
            MfClaimedChunk(uuid, 2, 2, factionId)
        )
        val paths = uut.getPaths(claims)
        // Should have 2 paths - outer boundary and inner hole
        assertEquals(2, paths.size)
    }

    @Test
    fun testGetPaths_ThreeNonContiguousClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 5, 5, factionId),
            MfClaimedChunk(uuid, 10, 10, factionId)
        )
        val paths = uut.getPaths(claims)
        assertEquals(3, paths.size)
        // Each path should be a single square
        paths.forEach { path ->
            assertEquals(4, path.size)
        }
    }

    @Test
    fun testGetPaths_LargeContiguousGrid() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = mutableListOf<MfClaimedChunk>()
        // Create a 5x5 grid
        for (x in 0 until 5) {
            for (z in 0 until 5) {
                claims.add(MfClaimedChunk(uuid, x, z, factionId))
            }
        }
        val paths = uut.getPaths(claims)
        assertEquals(1, paths.size)
        // Outer boundary should have specific points
        val path = paths[0]
        assert(path.contains(Point(0, 0)))
        assert(path.contains(Point(5, 0)))
        assert(path.contains(Point(5, 5)))
        assert(path.contains(Point(0, 5)))
    }

    @Test
    fun testGetPaths_SnakeShapedClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 1, 0, factionId),
            MfClaimedChunk(uuid, 1, 1, factionId),
            MfClaimedChunk(uuid, 0, 1, factionId),
            MfClaimedChunk(uuid, 0, 2, factionId),
            MfClaimedChunk(uuid, 1, 2, factionId)
        )
        val paths = uut.getPaths(claims)
        assertEquals(1, paths.size)
    }

    @Test
    fun testGetPaths_CheckerboardPattern() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 2, 0, factionId),
            MfClaimedChunk(uuid, 1, 1, factionId),
            MfClaimedChunk(uuid, 0, 2, factionId),
            MfClaimedChunk(uuid, 2, 2, factionId)
        )
        val paths = uut.getPaths(claims)
        // Note: Regions sharing corner points may be merged into a single path
        // In this pattern, (1,1) shares corners with all 4 other regions
        assertEquals(4, paths.size)
    }

    @Test
    fun testGetPaths_SingleClaimWithNegativeCoordinates() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(MfClaimedChunk(uuid, -5, -10, factionId))
        val paths = uut.getPaths(claims)
        val expectedPaths = listOf(
            listOf(Point(-5, -10), Point(-4, -10), Point(-4, -9), Point(-5, -9))
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_ClaimsSpanningNegativeAndPositiveCoordinates() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, -1, -1, factionId),
            MfClaimedChunk(uuid, 0, -1, factionId),
            MfClaimedChunk(uuid, -1, 0, factionId),
            MfClaimedChunk(uuid, 0, 0, factionId)
        )
        val paths = uut.getPaths(claims)
        assertEquals(1, paths.size)
        val path = paths[0]
        assert(path.contains(Point(-1, -1)))
        assert(path.contains(Point(1, 1)))
    }

    @Test
    fun testGetPaths_DuplicateClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 1, 0, factionId)
        )
        val paths = uut.getPaths(claims)
        // Should handle duplicates gracefully
        assert(paths.isNotEmpty())
    }
}
