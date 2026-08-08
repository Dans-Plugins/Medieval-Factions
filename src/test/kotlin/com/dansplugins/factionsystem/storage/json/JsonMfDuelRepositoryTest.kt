package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.duel.MfDuel
import com.dansplugins.factionsystem.duel.MfDuelId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.player.MfPlayerId
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
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger

class JsonMfDuelRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfDuelRepository

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        repository = JsonMfDuelRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
    }

    private fun duel(
        id: MfDuelId = MfDuelId.generate(),
        version: Int = 0,
        endTime: Instant = Instant.parse("2026-08-08T18:30:00Z"),
        challengerLocation: MfPosition? = null,
        challengedLocation: MfPosition? = null
    ) = MfDuel(
        id,
        version,
        MfPlayerId(UUID.randomUUID().toString()),
        MfPlayerId(UUID.randomUUID().toString()),
        20.0,
        18.5,
        endTime,
        challengerLocation,
        challengedLocation
    )

    @Test
    fun `round trips a duel including its Instant end time`() {
        val endTime = Instant.parse("2026-08-08T18:30:00Z")
        val original = duel(endTime = endTime)

        val saved = repository.upsert(original)
        assertEquals(1, saved.version)

        val read = repository.getDuels().singleOrNull()
        assertNotNull(read, "the duel should be readable after being written")
        requireNotNull(read)
        assertEquals(original.id, read.id)
        assertEquals(original.challengerId, read.challengerId)
        assertEquals(original.challengedId, read.challengedId)
        assertEquals(20.0, read.challengerHealth)
        assertEquals(18.5, read.challengedHealth)
        assertEquals(endTime, read.endTime, "the end time must survive the round trip")
    }

    @Test
    fun `round trips duel locations`() {
        val worldId = UUID.randomUUID()
        val challengerLocation = MfPosition(worldId, 1.5, 64.0, -2.5, 90.0f, 12.5f)
        val challengedLocation = MfPosition(worldId, -8.0, 70.0, 3.25, 180.0f, -5.0f)

        val saved = repository.upsert(duel(challengerLocation = challengerLocation, challengedLocation = challengedLocation))

        val read = repository.getDuels().single { it.id == saved.id }
        assertEquals(challengerLocation, read.challengerLocation)
        assertEquals(challengedLocation, read.challengedLocation)
    }

    @Test
    fun `round trips a duel with no recorded locations`() {
        val saved = repository.upsert(duel())

        val read = repository.getDuels().single { it.id == saved.id }
        assertNull(read.challengerLocation)
        assertNull(read.challengedLocation)
    }

    @Test
    fun `updates an existing duel and increments its version`() {
        val first = repository.upsert(duel())
        assertEquals(1, first.version)

        val second = repository.upsert(first.copy(challengerHealth = 4.0))
        assertEquals(2, second.version)
        assertEquals(1, repository.getDuels().size)
        assertEquals(4.0, repository.getDuels().single().challengerHealth)
    }

    @Test
    fun `rejects a stale version`() {
        val id = MfDuelId.generate()
        repository.upsert(duel(id = id))

        assertThrows<OptimisticLockingFailureException> { repository.upsert(duel(id = id, version = 0)) }
    }

    @Test
    fun `deletes a duel`() {
        val doomed = repository.upsert(duel())
        repository.upsert(duel())

        repository.delete(doomed.id)

        assertEquals(1, repository.getDuels().size)
        assertTrue(repository.getDuels().none { it.id == doomed.id })
    }

    @Test
    fun `returns no duels when nothing has been stored`() {
        assertTrue(repository.getDuels().isEmpty())
    }
}
