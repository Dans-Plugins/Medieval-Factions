package com.dansplugins.factionsystem.area

import com.dansplugins.factionsystem.TestUtils
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class MfBlockPositionTest {
    private val testUtils = TestUtils()

    @Test
    fun testInitialization() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 100
        val y = 64
        val z = -50

        // execute
        val blockPosition = MfBlockPosition(worldId, x, y, z)

        // verify
        assertEquals(worldId, blockPosition.worldId)
        assertEquals(x, blockPosition.x)
        assertEquals(y, blockPosition.y)
        assertEquals(z, blockPosition.z)
    }

    @Test
    fun testFromBukkitBlock() {
        // prepare
        val worldId = testUtils.createRandomUUID()
        val x = 10
        val y = 70
        val z = 20

        val world = testUtils.createMockWorld(worldId)
        val block = testUtils.createMockBlock(world, x, y, z)

        // execute
        val blockPosition = MfBlockPosition.fromBukkitBlock(block)

        // verify
        assertEquals(worldId, blockPosition.worldId)
        assertEquals(x, blockPosition.x)
        assertEquals(y, blockPosition.y)
        assertEquals(z, blockPosition.z)
    }

    @Test
    fun testWithNegativeCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = -100
        val y = -64
        val z = -200

        // execute
        val blockPosition = MfBlockPosition(worldId, x, y, z)

        // verify
        assertEquals(x, blockPosition.x)
        assertEquals(y, blockPosition.y)
        assertEquals(z, blockPosition.z)
    }

    @Test
    fun testWithZeroCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()

        // execute
        val blockPosition = MfBlockPosition(worldId, 0, 0, 0)

        // verify
        assertEquals(0, blockPosition.x)
        assertEquals(0, blockPosition.y)
        assertEquals(0, blockPosition.z)
    }

    @Test
    fun testEquality() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 100
        val y = 64
        val z = 200

        // execute
        val position1 = MfBlockPosition(worldId, x, y, z)
        val position2 = MfBlockPosition(worldId, x, y, z)

        // verify
        assertEquals(position1, position2)
    }

    @Test
    fun testDifferentWorldsSameCoordinates() {
        // prepare
        val worldId1 = UUID.randomUUID()
        val worldId2 = UUID.randomUUID()
        val x = 10
        val y = 64
        val z = 20

        // execute
        val position1 = MfBlockPosition(worldId1, x, y, z)
        val position2 = MfBlockPosition(worldId2, x, y, z)

        // verify - positions should be different because worlds are different
        assertEquals(position1.x, position2.x)
        assertEquals(position1.y, position2.y)
        assertEquals(position1.z, position2.z)
        assertEquals(worldId1, position1.worldId)
        assertEquals(worldId2, position2.worldId)
    }

    @Test
    fun testWithLargeCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 30000000
        val y = 256
        val z = -30000000

        // execute
        val blockPosition = MfBlockPosition(worldId, x, y, z)

        // verify
        assertEquals(x, blockPosition.x)
        assertEquals(y, blockPosition.y)
        assertEquals(z, blockPosition.z)
    }

    @Test
    fun testYCoordinateRange() {
        // prepare
        val worldId = UUID.randomUUID()
        // Minecraft Y-coordinate range boundaries (typically -64 to 320 in modern versions)
        val minY = -64
        val normalY = 64
        val maxY = 320

        // execute - test various Y coordinates
        val position1 = MfBlockPosition(worldId, 0, minY, 0)
        val position2 = MfBlockPosition(worldId, 0, normalY, 0)
        val position3 = MfBlockPosition(worldId, 0, maxY, 0)

        // verify
        assertEquals(minY, position1.y)
        assertEquals(normalY, position2.y)
        assertEquals(maxY, position3.y)
    }
}
