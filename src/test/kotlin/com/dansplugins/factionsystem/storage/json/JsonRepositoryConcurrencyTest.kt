package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Repository writes are dispatched from many independent async tasks, so two callers can hit the same
 * file at once. Each mutating method loads the file, changes the loaded copy and writes it back; unless
 * that whole cycle holds the file's write lock, the second writer overwrites the first writer's change
 * with a snapshot that predates it. These tests fail without [JsonStorageManager.withFileLock].
 */
class JsonRepositoryConcurrencyTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var storageManager: JsonStorageManager

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        storageManager = JsonStorageManager(plugin, tempDir.toString())
    }

    private fun runConcurrently(threads: Int, action: (Int) -> Unit) {
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        val failures = mutableListOf<Throwable>()
        repeat(threads) { index ->
            pool.submit {
                try {
                    start.await()
                    action(index)
                } catch (e: Throwable) {
                    synchronized(failures) { failures.add(e) }
                } finally {
                    done.countDown()
                }
            }
        }
        start.countDown()
        check(done.await(60, TimeUnit.SECONDS)) { "concurrent writers did not finish in time" }
        pool.shutdownNow()
        synchronized(failures) {
            if (failures.isNotEmpty()) throw AssertionError("concurrent writers failed", failures.first())
        }
    }

    @Test
    fun `concurrent claim writes are not lost`() {
        val repository = JsonMfClaimedChunkRepository(plugin, storageManager)
        val factionId = MfFactionId.generate()
        val worldId = UUID.randomUUID()
        val writers = 16

        runConcurrently(writers) { index ->
            repository.upsert(MfClaimedChunk(worldId, index, index, factionId))
        }

        assertEquals(writers, repository.getClaims().size, "every concurrent claim should have been persisted")
        assertEquals((0 until writers).toSet(), repository.getClaims().map { it.x }.toSet())
    }

    @Test
    fun `concurrent deletes leave the remaining claims intact`() {
        val repository = JsonMfClaimedChunkRepository(plugin, storageManager)
        val factionId = MfFactionId.generate()
        val worldId = UUID.randomUUID()
        repeat(20) { repository.upsert(MfClaimedChunk(worldId, it, it, factionId)) }

        runConcurrently(10) { index -> repository.delete(worldId, index, index) }

        assertEquals(10, repository.getClaims().size)
        assertEquals((10 until 20).toSet(), repository.getClaims().map { it.x }.toSet())
    }
}
