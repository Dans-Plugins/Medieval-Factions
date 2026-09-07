package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.faction.MfFactionId
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class MfClaimedChunkTest {
    private val testUtils = TestUtils()

    @Test
    fun testInitializationWithCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 10
        val z = 20
        val factionId = MfFactionId.generate()

        // execute
        val claimedChunk = MfClaimedChunk(worldId, x, z, factionId)

        // verify
        assertEquals(worldId, claimedChunk.worldId)
        assertEquals(x, claimedChunk.x)
        assertEquals(z, claimedChunk.z)
        assertEquals(factionId, claimedChunk.factionId)
    }

    @Test
    fun testInitializationWithChunk() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 15
        val z = 25
        val factionId = MfFactionId.generate()

        val world = testUtils.createMockWorld(worldId)
        val chunk = testUtils.createMockChunk(world, x, z)

        // execute
        val claimedChunk = MfClaimedChunk(chunk, factionId)

        // verify
        assertEquals(worldId, claimedChunk.worldId)
        assertEquals(x, claimedChunk.x)
        assertEquals(z, claimedChunk.z)
        assertEquals(factionId, claimedChunk.factionId)
    }

    @Test
    fun testInitializationWithChunkPosition() {
        // prepare
        val worldId = UUID.randomUUID()
        val x = 5
        val z = 35
        val factionId = MfFactionId.generate()

        val chunkPosition = MfChunkPosition(worldId, x, z)

        // execute
        val claimedChunk = MfClaimedChunk(chunkPosition, factionId)

        // verify
        assertEquals(worldId, claimedChunk.worldId)
        assertEquals(x, claimedChunk.x)
        assertEquals(z, claimedChunk.z)
        assertEquals(factionId, claimedChunk.factionId)
    }

    @Test
    fun testMultipleClaimsForSameFaction() {
        // prepare
        val worldId = UUID.randomUUID()
        val factionId = MfFactionId.generate()

        // execute
        val claim1 = MfClaimedChunk(worldId, 0, 0, factionId)
        val claim2 = MfClaimedChunk(worldId, 1, 0, factionId)
        val claim3 = MfClaimedChunk(worldId, 0, 1, factionId)

        // verify
        assertEquals(factionId, claim1.factionId)
        assertEquals(factionId, claim2.factionId)
        assertEquals(factionId, claim3.factionId)
        assertEquals(claim1.worldId, claim2.worldId)
        assertEquals(claim1.worldId, claim3.worldId)
    }

    @Test
    fun testClaimsWithNegativeCoordinates() {
        // prepare
        val worldId = UUID.randomUUID()
        val factionId = MfFactionId.generate()

        // execute
        val claimedChunk = MfClaimedChunk(worldId, -10, -20, factionId)

        // verify
        assertEquals(-10, claimedChunk.x)
        assertEquals(-20, claimedChunk.z)
    }

    @Test
    fun testClaimsInDifferentWorlds() {
        // prepare
        val worldId1 = UUID.randomUUID()
        val worldId2 = UUID.randomUUID()
        val factionId = MfFactionId.generate()
        val x = 10
        val z = 20

        // execute
        val claim1 = MfClaimedChunk(worldId1, x, z, factionId)
        val claim2 = MfClaimedChunk(worldId2, x, z, factionId)

        // verify
        assertEquals(worldId1, claim1.worldId)
        assertEquals(worldId2, claim2.worldId)
        assertEquals(claim1.x, claim2.x)
        assertEquals(claim1.z, claim2.z)
        assertEquals(claim1.factionId, claim2.factionId)
    }
}
