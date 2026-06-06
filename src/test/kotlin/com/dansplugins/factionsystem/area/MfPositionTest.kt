package com.dansplugins.factionsystem.area

import com.dansplugins.factionsystem.TestUtils
import org.bukkit.Location
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class MfPositionTest {
    private val testUtils = TestUtils()

    @Test
    fun testInitialization() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 100.5
        val y = 64.0
        val z = -50.25
        val yaw = 90.0f
        val pitch = 45.0f

        // execute
        val position = MfPosition(worldId, x, y, z, yaw, pitch)

        // verify
        assertEquals(worldId, position.worldId)
        assertEquals(x, position.x)
        assertEquals(y, position.y)
        assertEquals(z, position.z)
        assertEquals(yaw, position.yaw)
        assertEquals(pitch, position.pitch)
    }

    @Test
    fun testFromBukkitLocation() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 10.5
        val y = 70.0
        val z = 20.75
        val yaw = 180.0f
        val pitch = -45.0f

        val world = testUtils.createMockWorld(worldId)

        val location = mock(Location::class.java)
        `when`(location.world).thenReturn(world)
        `when`(location.x).thenReturn(x)
        `when`(location.y).thenReturn(y)
        `when`(location.z).thenReturn(z)
        `when`(location.yaw).thenReturn(yaw)
        `when`(location.pitch).thenReturn(pitch)

        // execute
        val position = MfPosition.fromBukkitLocation(location)

        // verify
        assertEquals(worldId, position.worldId)
        assertEquals(x, position.x)
        assertEquals(y, position.y)
        assertEquals(z, position.z)
        assertEquals(yaw, position.yaw)
        assertEquals(pitch, position.pitch)
    }

    @Test
    fun testWithNegativeCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = -100.0
        val y = -64.0
        val z = -200.0

        // execute
        val position = MfPosition(worldId, x, y, z, 0.0f, 0.0f)

        // verify
        assertEquals(x, position.x)
        assertEquals(y, position.y)
        assertEquals(z, position.z)
    }

    @Test
    fun testWithZeroCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()

        // execute
        val position = MfPosition(worldId, 0.0, 0.0, 0.0, 0.0f, 0.0f)

        // verify
        assertEquals(0.0, position.x)
        assertEquals(0.0, position.y)
        assertEquals(0.0, position.z)
        assertEquals(0.0f, position.yaw)
        assertEquals(0.0f, position.pitch)
    }

    @Test
    fun testWithMaxYawAndPitch() {
        // prepare
        val worldId = UUID.randomUUID()
        val yaw = 360.0f
        val pitch = 90.0f

        // execute
        val position = MfPosition(worldId, 0.0, 0.0, 0.0, yaw, pitch)

        // verify
        assertEquals(yaw, position.yaw)
        assertEquals(pitch, position.pitch)
    }

    @Test
    fun testWithMinYawAndPitch() {
        // prepare
        val worldId = UUID.randomUUID()
        val yaw = -180.0f
        val pitch = -90.0f

        // execute
        val position = MfPosition(worldId, 0.0, 0.0, 0.0, yaw, pitch)

        // verify
        assertEquals(yaw, position.yaw)
        assertEquals(pitch, position.pitch)
    }

    @Test
    fun testEquality() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 100.0
        val y = 64.0
        val z = 200.0
        val yaw = 90.0f
        val pitch = 0.0f

        // execute
        val position1 = MfPosition(worldId, x, y, z, yaw, pitch)
        val position2 = MfPosition(worldId, x, y, z, yaw, pitch)

        // verify
        assertEquals(position1, position2)
    }

    @Test
    fun testPrecisionCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 123.456789
        val y = 64.123456
        val z = 987.654321

        // execute
        val position = MfPosition(worldId, x, y, z, 0.0f, 0.0f)

        // verify
        assertEquals(x, position.x)
        assertEquals(y, position.y)
        assertEquals(z, position.z)
    }

    @Test
    fun testFractionalYawAndPitch() {
        // prepare
        val worldId = UUID.randomUUID()
        val yaw = 45.5f
        val pitch = 22.75f

        // execute
        val position = MfPosition(worldId, 0.0, 0.0, 0.0, yaw, pitch)

        // verify
        assertEquals(yaw, position.yaw)
        assertEquals(pitch, position.pitch)
    }
}
