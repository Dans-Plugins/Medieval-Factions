package com.dansplugins.factionsystem.map.builders

import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
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
        // With O(n) optimization, this should be very fast (< 10ms)
        assert(time < 100) { "Processing 100 claims took ${time}ms, expected < 100ms" }
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
        // With O(n) optimization, 1000 claims should still be fast (< 100ms)
        // Old O(n²) algorithm would take ~4000ms for 1024 claims
        assert(time < 500) { "Processing 1024 claims took ${time}ms, expected < 500ms" }
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
}
