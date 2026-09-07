package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipId
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
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
import java.util.logging.Logger

class JsonMfFactionRelationshipRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfFactionRelationshipRepository
    private val factionId = MfFactionId.generate()
    private val targetId = MfFactionId.generate()

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        repository = JsonMfFactionRelationshipRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
    }

    private fun relationship(
        id: MfFactionRelationshipId = MfFactionRelationshipId.generate(),
        from: MfFactionId = factionId,
        to: MfFactionId = targetId,
        type: MfFactionRelationshipType = MfFactionRelationshipType.ALLY
    ) = MfFactionRelationship(id, from, to, type)

    @Test
    fun `round trips a relationship including its type`() {
        val saved = repository.upsert(relationship(type = MfFactionRelationshipType.VASSAL))

        val read = repository.getFactionRelationship(saved.id)
        assertNotNull(read)
        requireNotNull(read)
        assertEquals(factionId, read.factionId)
        assertEquals(targetId, read.targetId)
        assertEquals(MfFactionRelationshipType.VASSAL, read.type, "the enum must survive the round trip")
    }

    @Test
    fun `round trips every relationship type`() {
        MfFactionRelationshipType.values().forEach { type ->
            val saved = repository.upsert(relationship(to = MfFactionId.generate(), type = type))
            assertEquals(type, repository.getFactionRelationship(saved.id)?.type, "type $type should round trip")
        }
        assertEquals(MfFactionRelationshipType.values().size, repository.getFactionRelationships().size)
    }

    @Test
    fun `filters relationships by faction, target and type`() {
        val otherTarget = MfFactionId.generate()
        repository.upsert(relationship(type = MfFactionRelationshipType.ALLY))
        repository.upsert(relationship(to = otherTarget, type = MfFactionRelationshipType.AT_WAR))
        repository.upsert(relationship(from = MfFactionId.generate(), type = MfFactionRelationshipType.ALLY))

        assertEquals(2, repository.getFactionRelationships(factionId).size)
        assertEquals(1, repository.getFactionRelationships(factionId, targetId).size)
        assertEquals(1, repository.getFactionRelationships(factionId, MfFactionRelationshipType.ALLY).size)
        assertEquals(1, repository.getFactionRelationships(factionId, MfFactionRelationshipType.AT_WAR).size)
        assertEquals(3, repository.getFactionRelationships().size)
    }

    @Test
    fun `upserting the same id replaces rather than duplicating`() {
        val id = MfFactionRelationshipId.generate()
        repository.upsert(relationship(id = id, type = MfFactionRelationshipType.ALLY))
        repository.upsert(relationship(id = id, type = MfFactionRelationshipType.AT_WAR))

        assertEquals(1, repository.getFactionRelationships().size)
        assertEquals(MfFactionRelationshipType.AT_WAR, repository.getFactionRelationship(id)?.type)
    }

    @Test
    fun `deletes a relationship`() {
        val doomed = repository.upsert(relationship())
        repository.upsert(relationship(to = MfFactionId.generate()))

        repository.delete(doomed.id)

        assertNull(repository.getFactionRelationship(doomed.id))
        assertEquals(1, repository.getFactionRelationships().size)
    }

    @Test
    fun `returns nothing when no relationships are stored`() {
        assertNull(repository.getFactionRelationship(MfFactionRelationshipId.generate()))
        assertTrue(repository.getFactionRelationships().isEmpty())
        assertTrue(repository.getFactionRelationships(factionId).isEmpty())
    }
}
