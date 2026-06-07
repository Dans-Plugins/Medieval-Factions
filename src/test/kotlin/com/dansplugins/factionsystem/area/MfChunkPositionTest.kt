package com.dansplugins.factionsystem.area

import com.dansplugins.factionsystem.TestUtils
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MfChunkPositionTest {
    private val testUtils = TestUtils()

    @Test
    fun testInitialization() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 10
        val z = 20

        // execute
        val chunkPosition = MfChunkPosition(worldId, x, z)

        // verify
        assertEquals(worldId, chunkPosition.worldId)
        assertEquals(x, chunkPosition.x)
        assertEquals(z, chunkPosition.z)
    }

    @Test
    fun testFromBukkit() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 5
        val z = 15

        val world = testUtils.createMockWorld(worldId)
        val chunk = testUtils.createMockChunk(world, x, z)

        // execute
        val chunkPosition = MfChunkPosition.fromBukkit(chunk)

        // verify
        assertEquals(worldId, chunkPosition.worldId)
        assertEquals(x, chunkPosition.x)
        assertEquals(z, chunkPosition.z)
    }

    @Test
    fun testWithNegativeCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = -10
        val z = -20

        // execute
        val chunkPosition = MfChunkPosition(worldId, x, z)

        // verify
        assertEquals(x, chunkPosition.x)
        assertEquals(z, chunkPosition.z)
    }

    @Test
    fun testEquality() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 10
        val z = 20

        // execute
        val position1 = MfChunkPosition(worldId, x, z)
        val position2 = MfChunkPosition(worldId, x, z)

        // verify
        assertEquals(position1, position2)
    }

    @Test
    fun testDifferentWorldsSameCoordinates() {
        // prepare
        val worldId1 = UUID.randomUUID()
        val worldId2 = UUID.randomUUID()
        val x = 10
        val z = 20

        // execute
        val position1 = MfChunkPosition(worldId1, x, z)
        val position2 = MfChunkPosition(worldId2, x, z)

        // verify - positions should be different because worlds are different
        assertEquals(position1.x, position2.x)
        assertEquals(position1.z, position2.z)
        assertEquals(worldId1, position1.worldId)
        assertEquals(worldId2, position2.worldId)
        assertNotEquals(position1, position2) // Different worlds means different positions
    }

    @Test
    fun testZeroCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()

        // execute
        val chunkPosition = MfChunkPosition(worldId, 0, 0)

        // verify
        assertEquals(0, chunkPosition.x)
        assertEquals(0, chunkPosition.z)
    }

    @Test
    fun testLargeCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 1000000
        val z = 2000000

        // execute
        val chunkPosition = MfChunkPosition(worldId, x, z)

        // verify
        assertEquals(x, chunkPosition.x)
        assertEquals(z, chunkPosition.z)
    }
}
