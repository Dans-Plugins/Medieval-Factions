package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger

class JsonMfClaimedChunkRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfClaimedChunkRepository
    private val worldId: UUID = UUID.randomUUID()
    private val factionId = MfFactionId.generate()

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        repository = JsonMfClaimedChunkRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
    }

    @Test
    fun `round trips a claim including its world id`() {
        repository.upsert(MfClaimedChunk(worldId, 4, -7, factionId))

        val read = repository.getClaim(worldId, 4, -7)
        assertNotNull(read)
        requireNotNull(read)
        assertEquals(worldId, read.worldId, "the world UUID must survive the round trip")
        assertEquals(4, read.x)
        assertEquals(-7, read.z)
        assertEquals(factionId, read.factionId)
    }

    @Test
    fun `distinguishes claims by world`() {
        val otherWorldId = UUID.randomUUID()
        repository.upsert(MfClaimedChunk(worldId, 0, 0, factionId))
        repository.upsert(MfClaimedChunk(otherWorldId, 0, 0, factionId))

        assertEquals(worldId, repository.getClaim(worldId, 0, 0)?.worldId)
        assertEquals(otherWorldId, repository.getClaim(otherWorldId, 0, 0)?.worldId)
        assertEquals(2, repository.getClaims().size, "same coordinates in different worlds are distinct claims")
    }

    @Test
    fun `upserting the same chunk reassigns it rather than duplicating`() {
        val newOwner = MfFactionId.generate()
        repository.upsert(MfClaimedChunk(worldId, 1, 1, factionId))
        repository.upsert(MfClaimedChunk(worldId, 1, 1, newOwner))

        assertEquals(1, repository.getClaims().size)
        assertEquals(newOwner, repository.getClaim(worldId, 1, 1)?.factionId)
    }

    @Test
    fun `returns claims for a faction only`() {
        val otherFaction = MfFactionId.generate()
        repository.upsert(MfClaimedChunk(worldId, 1, 1, factionId))
        repository.upsert(MfClaimedChunk(worldId, 2, 2, factionId))
        repository.upsert(MfClaimedChunk(worldId, 3, 3, otherFaction))

        assertEquals(2, repository.getClaims(factionId).size)
        assertEquals(1, repository.getClaims(otherFaction).size)
        assertEquals(3, repository.getClaims().size)
    }

    @Test
    fun `deletes a single claim`() {
        repository.upsert(MfClaimedChunk(worldId, 1, 1, factionId))
        repository.upsert(MfClaimedChunk(worldId, 2, 2, factionId))

        repository.delete(worldId, 1, 1)

        assertNull(repository.getClaim(worldId, 1, 1))
        assertEquals(1, repository.getClaims().size)
    }

    @Test
    fun `deletes every claim belonging to a faction`() {
        val otherFaction = MfFactionId.generate()
        repository.upsert(MfClaimedChunk(worldId, 1, 1, factionId))
        repository.upsert(MfClaimedChunk(worldId, 2, 2, factionId))
        repository.upsert(MfClaimedChunk(worldId, 3, 3, otherFaction))

        repository.deleteAll(factionId)

        assertTrue(repository.getClaims(factionId).isEmpty())
        assertEquals(1, repository.getClaims().size, "other factions' claims must be left alone")
    }

    @Test
    fun `returns null for an unclaimed chunk`() {
        assertNull(repository.getClaim(worldId, 0, 0))
        assertTrue(repository.getClaims().isEmpty())
    }
}
