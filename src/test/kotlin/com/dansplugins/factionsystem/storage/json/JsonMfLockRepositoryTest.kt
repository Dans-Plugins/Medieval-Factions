package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.locks.MfLockedBlock
import com.dansplugins.factionsystem.locks.MfLockedBlockId
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
import java.util.UUID
import java.util.logging.Logger

class JsonMfLockRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfLockRepository
    private val worldId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        repository = JsonMfLockRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
    }

    private fun lockedBlock(
        id: MfLockedBlockId = MfLockedBlockId.generate(),
        version: Int = 0,
        x: Int = 1,
        y: Int = 2,
        z: Int = 3,
        playerId: MfPlayerId = MfPlayerId(UUID.randomUUID().toString()),
        accessors: List<MfPlayerId> = emptyList()
    ) = MfLockedBlock(id, version, MfBlockPosition(worldId, x, y, z), x shr 4, z shr 4, playerId, accessors)

    @Test
    fun `round trips a locked block`() {
        val owner = MfPlayerId(UUID.randomUUID().toString())
        val saved = repository.upsert(lockedBlock(playerId = owner, x = 10, y = 64, z = -20))
        assertEquals(1, saved.version)

        val read = repository.getLockedBlock(saved.id)
        assertNotNull(read)
        requireNotNull(read)
        assertEquals(owner, read.playerId)
        assertEquals(MfBlockPosition(worldId, 10, 64, -20), read.block)
        assertEquals(0, read.chunkX)
        assertEquals(-2, read.chunkZ)
    }

    /**
     * MfPlayerId is a value class. Kotlin flattens it to its underlying String when it is a field, but
     * inside a generic List it is boxed, so the accessor list serializes differently from playerId. This
     * pins the round trip regardless of which representation lands on disk.
     */
    @Test
    fun `round trips the accessor list`() {
        val accessors = listOf(
            MfPlayerId(UUID.randomUUID().toString()),
            MfPlayerId(UUID.randomUUID().toString()),
            MfPlayerId(UUID.randomUUID().toString())
        )

        val saved = repository.upsert(lockedBlock(accessors = accessors))

        val read = repository.getLockedBlock(saved.id)
        assertNotNull(read)
        assertEquals(accessors, read?.accessors, "every accessor must survive the round trip, in order")
    }

    @Test
    fun `round trips an empty accessor list`() {
        val saved = repository.upsert(lockedBlock(accessors = emptyList()))

        assertEquals(emptyList<MfPlayerId>(), repository.getLockedBlock(saved.id)?.accessors)
    }

    @Test
    fun `finds a locked block by world coordinates`() {
        val saved = repository.upsert(lockedBlock(x = 5, y = 6, z = 7))

        assertEquals(saved.id, repository.getLockedBlock(worldId, 5, 6, 7)?.id)
        assertNull(repository.getLockedBlock(worldId, 5, 6, 8), "a different block must not match")
        assertNull(repository.getLockedBlock(UUID.randomUUID(), 5, 6, 7), "a different world must not match")
    }

    @Test
    fun `lists every locked block`() {
        repository.upsert(lockedBlock(x = 1, y = 1, z = 1))
        repository.upsert(lockedBlock(x = 2, y = 2, z = 2))

        assertEquals(2, repository.getLockedBlocks().size)
    }

    @Test
    fun `updates an existing lock and rejects a stale version`() {
        val id = MfLockedBlockId.generate()
        val first = repository.upsert(lockedBlock(id = id))
        assertEquals(1, first.version)

        val extraAccessor = MfPlayerId(UUID.randomUUID().toString())
        val second = repository.upsert(first.copy(accessors = listOf(extraAccessor)))
        assertEquals(2, second.version)
        assertEquals(listOf(extraAccessor), repository.getLockedBlock(id)?.accessors)
        assertEquals(1, repository.getLockedBlocks().size)

        assertThrows<OptimisticLockingFailureException> { repository.upsert(lockedBlock(id = id, version = 0)) }
    }

    @Test
    fun `deletes by block position`() {
        val doomed = repository.upsert(lockedBlock(x = 1, y = 1, z = 1))
        repository.upsert(lockedBlock(x = 2, y = 2, z = 2))

        repository.delete(MfBlockPosition(worldId, 1, 1, 1))

        assertNull(repository.getLockedBlock(doomed.id))
        assertEquals(1, repository.getLockedBlocks().size)
    }

    @Test
    fun `returns null for an unknown lock`() {
        assertNull(repository.getLockedBlock(MfLockedBlockId.generate()))
        assertTrue(repository.getLockedBlocks().isEmpty())
    }
}
