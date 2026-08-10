package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.law.MfLaw
import com.dansplugins.factionsystem.law.MfLawId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import java.util.logging.Logger

class JsonMfLawRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfLawRepository
    private val factionId = MfFactionId.generate()

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        repository = JsonMfLawRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
    }

    private fun law(
        id: MfLawId = MfLawId.generate(),
        version: Int = 0,
        faction: MfFactionId = factionId,
        text: String = "No griefing",
        number: Int? = 1
    ) = MfLaw(id, version, faction, text, number)

    @Test
    fun `round trips a law`() {
        val saved = repository.upsert(law(text = "Pay your taxes", number = 3))
        assertEquals(1, saved.version)

        val read = repository.getLaw(saved.id)
        assertNotNull(read)
        requireNotNull(read)
        assertEquals("Pay your taxes", read.text)
        assertEquals(3, read.number)
        assertEquals(factionId, read.factionId)
    }

    @Test
    fun `round trips a law with no number`() {
        val saved = repository.upsert(law(number = null))

        assertNull(repository.getLaw(saved.id)?.number)
    }

    @Test
    fun `returns a faction's laws ordered by number`() {
        repository.upsert(law(text = "third", number = 3))
        repository.upsert(law(text = "first", number = 1))
        repository.upsert(law(text = "second", number = 2))
        repository.upsert(law(faction = MfFactionId.generate(), text = "other faction", number = 1))

        assertEquals(listOf("first", "second", "third"), repository.getLaws(factionId).map { it.text })
    }

    @Test
    fun `looks a law up by its index within the faction`() {
        repository.upsert(law(text = "first", number = 1))
        repository.upsert(law(text = "second", number = 2))

        assertEquals("first", repository.getLaw(factionId, 0)?.text)
        assertEquals("second", repository.getLaw(factionId, 1)?.text)
        assertNull(repository.getLaw(factionId, 99), "an out-of-range index should return null")
        assertEquals("first", repository.getLaw(factionId, null)?.text, "a null index returns the first law")
    }

    @Test
    fun `updates an existing law and rejects a stale version`() {
        val id = MfLawId.generate()
        val first = repository.upsert(law(id = id, text = "before"))
        assertEquals(1, first.version)

        val second = repository.upsert(first.copy(text = "after"))
        assertEquals(2, second.version)
        assertEquals("after", repository.getLaw(id)?.text)
        assertEquals(1, repository.getLaws(factionId).size)

        assertThrows<OptimisticLockingFailureException> { repository.upsert(law(id = id, version = 0)) }
    }

    @Test
    fun `moves a law to a new number`() {
        val saved = repository.upsert(law(text = "movable", number = 1))

        repository.move(saved, 5)

        assertEquals(5, repository.getLaw(saved.id)?.number)
    }

    @Test
    fun `deletes a law by id and by instance`() {
        val byId = repository.upsert(law(text = "delete by id", number = 1))
        val byInstance = repository.upsert(law(text = "delete by instance", number = 2))

        repository.delete(byId.id)
        assertNull(repository.getLaw(byId.id))
        assertEquals(1, repository.getLaws(factionId).size)

        repository.delete(byInstance)
        assertNull(repository.getLaw(byInstance.id))
        assertTrue(repository.getLaws(factionId).isEmpty())
    }

    @Test
    fun `returns null for an unknown law`() {
        assertNull(repository.getLaw(MfLawId.generate()))
        assertTrue(repository.getLaws(factionId).isEmpty())
    }
}
