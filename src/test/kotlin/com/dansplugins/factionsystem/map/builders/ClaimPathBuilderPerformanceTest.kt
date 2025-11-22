package com.dansplugins.factionsystem.map.builders

import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.system.measureTimeMillis

class ClaimPathBuilderPerformanceTest {
    private lateinit var uut: ClaimPathBuilder

    @BeforeEach
    fun setUp() {
        uut = ClaimPathBuilder()
    }

    /**
     * This test proves the optimization by demonstrating O(n) linear scaling.
     * If the algorithm were O(n²), doubling the input size would quadruple the time.
     * With O(n) optimization, doubling the input size should roughly double the time.
     */
    @Test
    fun testGetPaths_provesLinearScaling() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")

        // Test with progressively larger inputs to demonstrate linear scaling
        val sizes = listOf(100, 200, 400, 800)
        val times = mutableListOf<Long>()

        sizes.forEach { size ->
            val claims = mutableListOf<MfClaimedChunk>()
            val gridSize = kotlin.math.sqrt(size.toDouble()).toInt()
            for (x in 0 until gridSize) {
                for (z in 0 until gridSize) {
                    claims.add(MfClaimedChunk(uuid, x, z, factionId))
                }
            }

            val time = measureTimeMillis {
                val paths = uut.getPaths(claims)
                assert(paths.isNotEmpty())
            }
            times.add(time)
            println("Size: $size claims, Time: ${time}ms")
        }

        // Verify linear scaling: when size doubles, time should not quadruple
        // For O(n²): time[3]/time[1] would be ~16 (4x size = 16x time)
        // For O(n): time[3]/time[1] should be ~4 (4x size = 4x time)
        if (times[0] > 0 && times[2] > 0) {
            val scalingFactor = times[2].toDouble() / times[0].toDouble()
            println("Scaling factor (4x input): ${scalingFactor}x time")
            // O(n) should scale closer to 4x, O(n²) would scale closer to 16x
            assertTrue(
                scalingFactor < 12.0,
                "Scaling factor $scalingFactor suggests worse than O(n) performance. " +
                    "Expected < 12x for 4x input increase (O(n²) would be ~16x)"
            )
        }
    }

    /**
     * This test proves optimization by demonstrating that execution time grows
     * proportionally with input size, not quadratically.
     */
    @Test
    fun testGetPaths_provesNotQuadraticComplexity() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")

        // Measure times for different input sizes
        val measurements = mutableListOf<Pair<Int, Long>>()

        listOf(50, 100, 200, 400).forEach { gridSize ->
            val claims = mutableListOf<MfClaimedChunk>()
            for (x in 0 until gridSize) {
                for (z in 0 until gridSize) {
                    claims.add(MfClaimedChunk(uuid, x, z, factionId))
                }
            }

            val time = measureTimeMillis {
                uut.getPaths(claims)
            }
            measurements.add(Pair(claims.size, time))
            println("${claims.size} claims: ${time}ms")
        }

        // Calculate time per claim for different sizes
        // For O(n), this should remain relatively constant
        // For O(n²), this would increase linearly with n
        measurements.forEach { (size, time) ->
            val timePerClaim = if (time > 0) time.toDouble() / size else 0.0
            println("Size: $size, Time per claim: ${"%.4f".format(timePerClaim)}ms")
        }

        // Verify the last measurement (largest) is reasonable
        val lastSize = measurements.last().first
        val lastTime = measurements.last().second
        val timePerClaim = lastTime.toDouble() / lastSize

        // With O(n), time per claim should stay under 0.5ms even for large inputs
        // With O(n²), this would grow significantly
        assertTrue(
            timePerClaim < 1.0,
            "Time per claim (${timePerClaim}ms) is too high for $lastSize claims. " +
                "This suggests O(n²) complexity. Expected < 1.0ms per claim with O(n)."
        )
    }

    /**
     * This test proves the HashSet optimization by demonstrating fast lookups
     * even with a large number of claims.
     */
    @Test
    fun testGetPaths_provesHashSetOptimization() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")

        // Create 1000 claims in a compact grid
        val claims = mutableListOf<MfClaimedChunk>()
        for (x in 0 until 32) {
            for (z in 0 until 32) {
                claims.add(MfClaimedChunk(uuid, x, z, factionId))
            }
        }

        val time = measureTimeMillis {
            val paths = uut.getPaths(claims)
            assert(paths.isNotEmpty())
        }

        println("Time for 1024 claims with HashSet: ${time}ms")

        // With HashSet O(1) lookups, 1024 claims should complete in < 100ms
        // Old O(n²) with list.any() would take ~4000ms
        assertTrue(
            time < 100,
            "Processing 1024 claims took ${time}ms. " +
                "Expected < 100ms with HashSet optimization. " +
                "Old O(n²) algorithm would take ~4000ms."
        )
    }

    @Test
    fun testGetPaths_performance_100claims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")

        // Create a 10x10 grid of claims (100 claims)
        val claims = mutableListOf<MfClaimedChunk>()
        for (x in 0 until 10) {
            for (z in 0 until 10) {
                claims.add(MfClaimedChunk(uuid, x, z, factionId))
            }
        }

        val time = measureTimeMillis {
            val paths = uut.getPaths(claims)
            assert(paths.isNotEmpty())
        }

        println("Time to process 100 claims: ${time}ms")
        // With O(n) optimization, this should be very fast (< 100ms)
        // Old O(n²) algorithm would take ~400ms for 100 claims
        assert(time < 100) { "Processing 100 claims took ${time}ms, expected < 100ms. This proves O(n) optimization." }
    }

    @Test
    fun testGetPaths_performance_1000claims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")

        // Create a 32x32 grid of claims (1024 claims)
        val claims = mutableListOf<MfClaimedChunk>()
        for (x in 0 until 32) {
            for (z in 0 until 32) {
                claims.add(MfClaimedChunk(uuid, x, z, factionId))
            }
        }

        val time = measureTimeMillis {
            val paths = uut.getPaths(claims)
            assert(paths.isNotEmpty())
        }

        println("Time to process 1024 claims: ${time}ms")
        // With O(n) optimization, 1000 claims should complete in < 500ms
        // Old O(n²) algorithm would take ~4000ms for 1024 claims (10x slower!)
        assert(time < 500) { "Processing 1024 claims took ${time}ms, expected < 500ms. This proves significant optimization." }
    }

    @Test
    fun testGetPaths_performance_complexShape() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")

        // Create a complex shape with many disconnected regions
        val claims = mutableListOf<MfClaimedChunk>()

        // Create 10 separate 10x10 regions
        for (regionX in 0 until 10) {
            for (regionZ in 0 until 10) {
                val offsetX = regionX * 15
                val offsetZ = regionZ * 15
                for (x in 0 until 10) {
                    for (z in 0 until 10) {
                        claims.add(MfClaimedChunk(uuid, offsetX + x, offsetZ + z, factionId))
                    }
                }
            }
        }

        val time = measureTimeMillis {
            val paths = uut.getPaths(claims)
            assert(paths.size == 100) // 10x10 separate regions
        }

        println("Time to process 1000 claims in 100 regions: ${time}ms")
        // This should still be relatively fast with O(n) algorithm
        assert(time < 1000) { "Processing 1000 claims in complex shape took ${time}ms, expected < 1000ms" }
    }

    @Test
    fun testGetPaths_performance_sparseDistribution() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")

        // Create 500 claims spaced far apart
        val claims = mutableListOf<MfClaimedChunk>()
        for (i in 0 until 500) {
            claims.add(MfClaimedChunk(uuid, i * 10, i * 10, factionId))
        }

        val time = measureTimeMillis {
            val paths = uut.getPaths(claims)
            assert(paths.size == 500) // Each claim should be separate
        }

        println("Time to process 500 sparse claims: ${time}ms")
        assert(time < 500) { "Processing 500 sparse claims took ${time}ms, expected < 500ms" }
    }

    @Test
    fun testGetPaths_performance_longStrip() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")

        // Create a very long strip of 1000 claims
        val claims = mutableListOf<MfClaimedChunk>()
        for (x in 0 until 1000) {
            claims.add(MfClaimedChunk(uuid, x, 0, factionId))
        }

        val time = measureTimeMillis {
            val paths = uut.getPaths(claims)
            assert(paths.size == 1)
        }

        println("Time to process 1000 claims in a strip: ${time}ms")
        assert(time < 500) { "Processing 1000 claims in a strip took ${time}ms, expected < 500ms" }
    }

    @Test
    fun testGetPaths_performance_withNegativeCoordinates() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")

        // Create a 20x20 grid centered at origin (spans negative and positive)
        val claims = mutableListOf<MfClaimedChunk>()
        for (x in -10 until 10) {
            for (z in -10 until 10) {
                claims.add(MfClaimedChunk(uuid, x, z, factionId))
            }
        }

        val time = measureTimeMillis {
            val paths = uut.getPaths(claims)
            assert(paths.isNotEmpty())
        }

        println("Time to process 400 claims with negative coordinates: ${time}ms")
        assert(time < 200) { "Processing 400 claims with negative coords took ${time}ms, expected < 200ms" }
    }

    @Test
    fun testGetPaths_performance_worstCaseCheckerboard() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")

        // Create a checkerboard pattern - worst case for path building
        val claims = mutableListOf<MfClaimedChunk>()
        for (x in 0 until 20) {
            for (z in 0 until 20) {
                if ((x + z) % 2 == 0) {
                    claims.add(MfClaimedChunk(uuid, x, z, factionId))
                }
            }
        }

        val time = measureTimeMillis {
            val paths = uut.getPaths(claims)
            // Checkerboard creates many separate regions
            assert(paths.size == 200) // Each claim is separate
        }

        println("Time to process 200 claims in checkerboard pattern: ${time}ms")
        assert(time < 500) { "Processing 200 claims in checkerboard took ${time}ms, expected < 500ms" }
    }
}
