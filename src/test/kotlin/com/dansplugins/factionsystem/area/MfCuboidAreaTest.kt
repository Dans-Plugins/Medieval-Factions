package com.dansplugins.factionsystem.area

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MfCuboidAreaTest {

    private val worldId: UUID = UUID.randomUUID()

    private fun position(x: Int, y: Int, z: Int, world: UUID = worldId) = MfBlockPosition(world, x, y, z)

    @Test
    fun testInitializationRejectsMismatchedWorlds() {
        // prepare
        val otherWorldId = UUID.randomUUID()
        val position1 = position(0, 64, 0)
        val position2 = position(1, 64, 1, otherWorldId)

        // execute
        val exception = assertFailsWith<IllegalStateException> { MfCuboidArea(position1, position2) }

        // verify
        assertEquals("Position worlds do not match", exception.message)
    }

    @Test
    fun testMinAndMaxPositionWhenCornersAreOrdered() {
        // prepare
        val position1 = position(0, 64, 0)
        val position2 = position(4, 70, 8)

        // execute
        val area = MfCuboidArea(position1, position2)

        // verify
        assertEquals(position(0, 64, 0), area.minPosition)
        assertEquals(position(4, 70, 8), area.maxPosition)
    }

    @Test
    fun testMinAndMaxPositionNormalizeReversedCorners() {
        // prepare - corners supplied in the opposite order
        val position1 = position(4, 70, 8)
        val position2 = position(0, 64, 0)

        // execute
        val area = MfCuboidArea(position1, position2)

        // verify
        assertEquals(position(0, 64, 0), area.minPosition)
        assertEquals(position(4, 70, 8), area.maxPosition)
    }

    @Test
    fun testMinAndMaxPositionNormalizePerAxis() {
        // prepare - each axis is inverted independently
        val position1 = position(4, 64, 8)
        val position2 = position(0, 70, 0)

        // execute
        val area = MfCuboidArea(position1, position2)

        // verify
        assertEquals(position(0, 64, 0), area.minPosition)
        assertEquals(position(4, 70, 8), area.maxPosition)
    }

    @Test
    fun testDimensionsAreSpansNotBlockCounts() {
        // prepare
        val area = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))

        // execute / verify - width/height/depth are max - min, so they are one less than the block count
        assertEquals(4, area.width)
        assertEquals(6, area.height)
        assertEquals(8, area.depth)
    }

    @Test
    fun testDimensionsOfSingleBlockAreaAreZero() {
        // prepare
        val area = MfCuboidArea(position(10, 64, 10), position(10, 64, 10))

        // execute / verify
        assertEquals(0, area.width)
        assertEquals(0, area.height)
        assertEquals(0, area.depth)
    }

    @Test
    fun testDimensionsAreOrderIndependent() {
        // prepare
        val forwards = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))
        val backwards = MfCuboidArea(position(4, 70, 8), position(0, 64, 0))

        // execute / verify
        assertEquals(forwards.width, backwards.width)
        assertEquals(forwards.height, backwards.height)
        assertEquals(forwards.depth, backwards.depth)
    }

    @Test
    fun testCenterPositionIsMidpointOfTheSuppliedCorners() {
        // prepare
        val area = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))

        // execute / verify
        assertEquals(position(2, 67, 4), area.centerPosition)
    }

    @Test
    fun testCenterPositionTruncatesTowardsZero() {
        // prepare - the midpoints do not land on whole blocks
        val area = MfCuboidArea(position(0, 64, 0), position(3, 65, 5))

        // execute / verify - integer division truncates rather than rounding
        assertEquals(position(1, 64, 2), area.centerPosition)
    }

    @Test
    fun testCenterPositionTruncatesTowardsZeroForNegativeCoordinates() {
        // prepare
        val area = MfCuboidArea(position(-3, -5, -7), position(0, 0, 0))

        // execute / verify - truncation is towards zero, so negative midpoints round up
        assertEquals(position(-1, -2, -3), area.centerPosition)
    }

    @Test
    fun testBlocksEnumeratesEveryBlockInclusively() {
        // prepare - a 2x2x2 region
        val area = MfCuboidArea(position(0, 64, 0), position(1, 65, 1))

        // execute
        val blocks = area.blocks

        // verify
        assertEquals(8, blocks.size)
        assertEquals(
            listOf(
                position(0, 64, 0),
                position(0, 64, 1),
                position(0, 65, 0),
                position(0, 65, 1),
                position(1, 64, 0),
                position(1, 64, 1),
                position(1, 65, 0),
                position(1, 65, 1)
            ),
            blocks
        )
    }

    @Test
    fun testBlocksOfSingleBlockAreaContainsThatBlock() {
        // prepare
        val area = MfCuboidArea(position(10, 64, 10), position(10, 64, 10))

        // execute
        val blocks = area.blocks

        // verify
        assertEquals(listOf(position(10, 64, 10)), blocks)
    }

    @Test
    fun testBlocksCountMatchesInclusiveDimensions() {
        // prepare
        val area = MfCuboidArea(position(0, 64, 0), position(2, 67, 4))

        // execute
        val blocks = area.blocks

        // verify - (width + 1) * (height + 1) * (depth + 1)
        assertEquals(3 * 4 * 5, blocks.size)
    }

    @Test
    fun testBlocksIsUnaffectedByCornerOrder() {
        // prepare
        val forwards = MfCuboidArea(position(0, 64, 0), position(1, 65, 1))
        val backwards = MfCuboidArea(position(1, 65, 1), position(0, 64, 0))

        // execute / verify
        assertEquals(forwards.blocks, backwards.blocks)
    }

    @Test
    fun testContainsIncludesInteriorAndBoundaryBlocks() {
        // prepare
        val area = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))

        // execute / verify
        assertTrue(area.contains(position(2, 67, 4)))
        assertTrue(area.contains(position(0, 64, 0)))
        assertTrue(area.contains(position(4, 70, 8)))
    }

    @Test
    fun testContainsExcludesBlocksOutsideEachBound() {
        // prepare
        val area = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))

        // execute / verify
        assertFalse(area.contains(position(-1, 67, 4)))
        assertFalse(area.contains(position(5, 67, 4)))
        assertFalse(area.contains(position(2, 63, 4)))
        assertFalse(area.contains(position(2, 71, 4)))
        assertFalse(area.contains(position(2, 67, -1)))
        assertFalse(area.contains(position(2, 67, 9)))
    }

    @Test
    fun testContainsExcludesIdenticalCoordinatesInAnotherWorld() {
        // prepare
        val area = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))
        val otherWorldPosition = position(2, 67, 4, UUID.randomUUID())

        // execute / verify
        assertFalse(area.contains(otherWorldPosition))
    }

    @Test
    fun testDistanceSquaredIsZeroForContainedPositions() {
        // prepare
        val area = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))

        // execute / verify
        assertEquals(0, area.distanceSquared(position(2, 67, 4)))
        assertEquals(0, area.distanceSquared(position(0, 64, 0)))
        assertEquals(0, area.distanceSquared(position(4, 70, 8)))
    }

    @Test
    fun testDistanceSquaredMeasuresToTheNearestFace() {
        // prepare
        val area = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))

        // execute / verify - three blocks beyond the maximum x bound, aligned on the other axes
        assertEquals(9, area.distanceSquared(position(7, 67, 4)))
        // two blocks below the minimum y bound
        assertEquals(4, area.distanceSquared(position(2, 62, 4)))
    }

    @Test
    fun testDistanceSquaredMeasuresToTheNearestCorner() {
        // prepare
        val area = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))

        // execute - offset by 1 on x, 2 on y and 3 on z from the minimum corner
        val distanceSquared = area.distanceSquared(position(-1, 62, -3))

        // verify
        assertEquals(1 + 4 + 9, distanceSquared)
    }

    @Test
    fun testDistanceSquaredIgnoresTheWorldOfTheGivenPosition() {
        // prepare - distanceSquared does not compare worlds, unlike contains
        val area = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))
        val otherWorldPosition = position(2, 67, 4, UUID.randomUUID())

        // execute / verify
        assertEquals(0, area.distanceSquared(otherWorldPosition))
    }

    @Test
    fun testDistanceSquaredOverflowsForVeryDistantPositions() {
        // prepare - the return type is Int, so squared distances beyond Int.MAX_VALUE wrap around.
        // See https://github.com/Dans-Plugins/Medieval-Factions/issues/1991 - this test characterizes
        // the current behaviour rather than endorsing it.
        val area = MfCuboidArea(position(60000, 64, 0), position(60000, 64, 0))

        // execute
        val distanceSquared = area.distanceSquared(position(0, 64, 0))

        // verify - 60000 * 60000 is 3,600,000,000, which wraps to a negative Int
        assertEquals(-694967296, distanceSquared)
        assertTrue(distanceSquared < 0)
    }

    @Test
    fun testEqualityDependsOnCornerOrderNotOnTheRegionCovered() {
        // prepare - both areas cover the same region
        val forwards = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))
        val backwards = MfCuboidArea(position(4, 70, 8), position(0, 64, 0))

        // execute / verify - equality is the data class default over the raw corners
        assertNotEquals(forwards, backwards)
        assertEquals(forwards.minPosition, backwards.minPosition)
        assertEquals(forwards.maxPosition, backwards.maxPosition)
    }

    @Test
    fun testEqualityHoldsForIdenticalCorners() {
        // prepare
        val area1 = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))
        val area2 = MfCuboidArea(position(0, 64, 0), position(4, 70, 8))

        // execute / verify
        assertEquals(area1, area2)
        assertEquals(area1.hashCode(), area2.hashCode())
    }
}
