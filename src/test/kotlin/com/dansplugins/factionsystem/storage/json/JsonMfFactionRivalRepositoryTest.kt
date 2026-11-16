package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.relationship.rival.MfFactionRival
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.logging.Logger

class JsonMfFactionRivalRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfFactionRivalRepository
    private val factionId = MfFactionId.generate()
    private val targetId = MfFactionId.generate()

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        repository = JsonMfFactionRivalRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
    }

    @Test
    fun `a storage folder from before rivals existed loads with no rivals and is left unchanged`() {
        File(tempDir.toFile(), "relationships.json").writeText("""{"relationships":[]}""")

        assertEquals(emptyList<MfFactionRival>(), repository.getRivals())
        assertFalse(File(tempDir.toFile(), "rivals.json").exists(), "reading must not create rivals.json")
    }

    @Test
    fun `round trips a rival including when it was named`() {
        val createdAt = Instant.ofEpochMilli(1_700_000_000_000)
        repository.insert(MfFactionRival(factionId, targetId, createdAt))

        assertEquals(listOf(MfFactionRival(factionId, targetId, createdAt)), repository.getRivals())
        assertTrue(File(tempDir.toFile(), "rivals.json").exists())
    }

    @Test
    fun `naming the same rival twice keeps the first and the original date`() {
        val first = Instant.ofEpochMilli(1_000)
        repository.insert(MfFactionRival(factionId, targetId, first))
        val second = repository.insert(MfFactionRival(factionId, targetId, Instant.ofEpochMilli(2_000)))

        assertEquals(first, second.createdAt)
        assertEquals(1, repository.getRivals().size)
    }

    @Test
    fun `delete removes only that rivalry`() {
        val otherTarget = MfFactionId.generate()
        repository.insert(MfFactionRival(factionId, targetId))
        repository.insert(MfFactionRival(factionId, otherTarget))
        repository.insert(MfFactionRival(targetId, factionId))

        repository.delete(factionId, targetId)

        assertEquals(
            setOf(factionId to otherTarget, targetId to factionId),
            repository.getRivals().map { it.factionId to it.targetId }.toSet()
        )
    }
}
