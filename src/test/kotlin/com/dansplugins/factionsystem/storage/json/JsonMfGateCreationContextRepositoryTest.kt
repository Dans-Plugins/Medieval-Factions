package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.gate.MfGateCreationContext
import com.dansplugins.factionsystem.player.MfPlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger

class JsonMfGateCreationContextRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfGateCreationContextRepository
    private val playerId = MfPlayerId(UUID.randomUUID().toString())
    private val worldId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        repository = JsonMfGateCreationContextRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
    }

    @Test
    fun `round trips a fully populated context`() {
        val saved = repository.upsert(
            MfGateCreationContext(
                playerId,
                0,
                MfBlockPosition(worldId, 1, 2, 3),
                MfBlockPosition(worldId, 4, 5, 6),
                MfBlockPosition(worldId, 7, 8, 9)
            )
        )
        assertEquals(1, saved.version)

        val read = repository.getContext(playerId)
        assertNotNull(read)
        requireNotNull(read)
        assertEquals(MfBlockPosition(worldId, 1, 2, 3), read.position1)
        assertEquals(MfBlockPosition(worldId, 4, 5, 6), read.position2)
        assertEquals(MfBlockPosition(worldId, 7, 8, 9), read.trigger)
    }

    @Test
    fun `round trips a context whose positions are not yet chosen`() {
        repository.upsert(MfGateCreationContext(playerId))

        val read = repository.getContext(playerId)
        assertNotNull(read)
        requireNotNull(read)
        assertNull(read.position1)
        assertNull(read.position2)
        assertNull(read.trigger)
    }

    @Test
    fun `round trips a partially completed context`() {
        repository.upsert(MfGateCreationContext(playerId, 0, MfBlockPosition(worldId, 1, 2, 3), null, null))

        val read = repository.getContext(playerId)
        assertEquals(MfBlockPosition(worldId, 1, 2, 3), read?.position1)
        assertNull(read?.position2, "an unchosen second position must stay null")
    }

    @Test
    fun `updates a context and rejects a stale version`() {
        val first = repository.upsert(MfGateCreationContext(playerId))
        assertEquals(1, first.version)

        val second = repository.upsert(first.copy(position1 = MfBlockPosition(worldId, 1, 1, 1)))
        assertEquals(2, second.version)
        assertEquals(MfBlockPosition(worldId, 1, 1, 1), repository.getContext(playerId)?.position1)

        assertThrows<OptimisticLockingFailureException> {
            repository.upsert(MfGateCreationContext(playerId, 0))
        }
    }

    @Test
    fun `keeps contexts separate per player`() {
        val otherPlayerId = MfPlayerId(UUID.randomUUID().toString())
        repository.upsert(MfGateCreationContext(playerId, 0, MfBlockPosition(worldId, 1, 1, 1)))
        repository.upsert(MfGateCreationContext(otherPlayerId, 0, MfBlockPosition(worldId, 2, 2, 2)))

        assertEquals(MfBlockPosition(worldId, 1, 1, 1), repository.getContext(playerId)?.position1)
        assertEquals(MfBlockPosition(worldId, 2, 2, 2), repository.getContext(otherPlayerId)?.position1)
    }

    @Test
    fun `deletes a context`() {
        val otherPlayerId = MfPlayerId(UUID.randomUUID().toString())
        repository.upsert(MfGateCreationContext(playerId))
        repository.upsert(MfGateCreationContext(otherPlayerId))

        repository.delete(playerId)

        assertNull(repository.getContext(playerId))
        assertNotNull(repository.getContext(otherPlayerId), "another player's context must be left alone")
    }

    @Test
    fun `returns null for a player with no context`() {
        assertNull(repository.getContext(playerId))
    }
}
