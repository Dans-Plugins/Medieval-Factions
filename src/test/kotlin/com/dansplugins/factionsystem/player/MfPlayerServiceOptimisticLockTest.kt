package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import com.dansplugins.factionsystem.player.JooqMfPlayerRepositoryTest.Companion.migratedH2Dsl
import com.dansplugins.factionsystem.player.JooqMfPlayerRepositoryTest.Companion.powerConfiguredPlugin
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Exercises MfPlayerService on top of the real jOOQ repository and H2, covering the save paths
 * that #2017's corrected optimistic-lock check makes able to conflict.
 */
class MfPlayerServiceOptimisticLockTest {

    private val initialPower = 5.0
    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JooqMfPlayerRepository
    private lateinit var service: MfPlayerService

    @BeforeEach
    fun setUp() {
        plugin = powerConfiguredPlugin(initialPower)
        val services = mock(Services::class.java)
        `when`(services.mapService).thenReturn(null)
        `when`(plugin.services).thenReturn(services)
        repository = JooqMfPlayerRepository(plugin, migratedH2Dsl())
        service = MfPlayerService(plugin, repository)
    }

    @Test
    fun `a stale save conflicts, keeps the newer row and refreshes the cache`() {
        val snapshot = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))
        // The power task bumps power and version in storage; the service cache still holds the snapshot.
        repository.increaseOnlinePlayerPower(listOf(snapshot.id))
        val afterTask = repository.getPlayer(snapshot.id)!!
        assertEquals(snapshot, service.getPlayer(snapshot.id))

        val result = service.save(snapshot.copy(isBypassEnabled = true))

        assertTrue(result is Failure && result.reason.type == CONFLICT, "expected a CONFLICT, was $result")
        assertEquals(afterTask, repository.getPlayer(snapshot.id), "the stale write must not reach storage")
        assertEquals(afterTask, service.getPlayer(snapshot.id), "the cache must be refreshed after a conflict")
    }

    @Test
    fun `update re-applies the change to the current row when the power task ran in between`() {
        val snapshot = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))
        // Power task lands between the read and the write, outside the cached entity.
        repository.increaseOnlinePlayerPower(listOf(snapshot.id))
        val afterTask = repository.getPlayer(snapshot.id)!!
        assertTrue(afterTask.power > snapshot.power)

        val result = service.update(snapshot) { it.copy(power = it.power - 1.0) }

        val updated = (result as Success).value
        assertEquals(afterTask.power - 1.0, updated.power, 1e-9, "both the power gain and the death penalty must be kept")
        assertEquals(afterTask.version + 1, updated.version)
        assertEquals(updated, service.getPlayer(snapshot.id))
    }

    @Test
    fun `quit-style update records powerAtLogout without rolling back a concurrent power change`() {
        val snapshot = saved(MfPlayer(newId(), power = 8.0, powerAtLogout = 3.0))
        repository.increaseOnlinePlayerPower(listOf(snapshot.id))
        val afterTask = repository.getPlayer(snapshot.id)!!

        val updated = (service.update(snapshot) { it.copy(powerAtLogout = snapshot.power) } as Success).value

        assertEquals(afterTask.power, updated.power, 1e-9)
        assertEquals(snapshot.power, updated.powerAtLogout, 1e-9)
    }

    @Test
    fun `update gives up with CONFLICT after the bounded number of attempts`() {
        val snapshot = saved(MfPlayer(newId(), power = 5.0, powerAtLogout = 5.0))
        val attempts = AtomicInteger()

        val result = service.update(snapshot) { current ->
            attempts.incrementAndGet()
            // Another writer gets in between every read and write.
            repository.upsert(repository.getPlayer(current.id)!!.copy(power = repository.getPlayer(current.id)!!.power + 1.0))
            current.copy(isBypassEnabled = true)
        }

        assertTrue(result is Failure && result.reason.type == CONFLICT, "expected a CONFLICT, was $result")
        assertEquals(MfPlayerService.MAX_UPDATE_ATTEMPTS, attempts.get())
        assertEquals(false, repository.getPlayer(snapshot.id)!!.isBypassEnabled)
    }

    @Test
    fun `a default create that loses a race keeps the stored row instead of resetting it`() {
        val id = newId()
        repository.upsert(MfPlayer(id, name = "Steve", power = 17.0, powerAtLogout = 17.0)) // not in the cache

        val result = service.save(MfPlayer(id, name = "Steve", power = initialPower, powerAtLogout = initialPower))

        assertEquals(17.0, (result as Success).value.power, "the stored player must not be reset to the initial power")
        assertEquals(17.0, repository.getPlayer(id)!!.power)
        assertEquals(17.0, service.getPlayer(id)!!.power)
    }

    @Test
    fun `a non-default create that loses a race is reported as a conflict`() {
        val id = newId()
        repository.upsert(MfPlayer(id, power = 17.0, powerAtLogout = 17.0))

        val result = service.save(MfPlayer(id, power = initialPower, powerAtLogout = initialPower, isBypassEnabled = true))

        assertTrue(result is Failure && result.reason.type == CONFLICT, "expected a CONFLICT, was $result")
        assertEquals(17.0, repository.getPlayer(id)!!.power)
        assertEquals(false, repository.getPlayer(id)!!.isBypassEnabled)
    }

    @Test
    fun `update applies a change that equals the defaults to a stored row the cache did not know about`() {
        val id = newId()
        repository.upsert(MfPlayer(id, power = 17.0, powerAtLogout = 17.0)) // not in the cache
        val base = service.getPlayer(id) ?: MfPlayer(id, power = initialPower, powerAtLogout = initialPower)

        val updated = (service.update(base) { it.copy(power = initialPower) } as Success).value

        assertEquals(initialPower, updated.power, "an admin power set to the initial value must not be swallowed")
        assertEquals(initialPower, repository.getPlayer(id)!!.power)
    }

    @Test
    fun `concurrent updates from stale snapshots never lose an applied change`() {
        val snapshot = saved(MfPlayer(newId(), power = 0.0, powerAtLogout = 0.0))
        val threads = 4
        val updatesPerThread = 25
        val successes = AtomicInteger()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threads)
        repeat(threads) {
            executor.submit {
                start.await()
                repeat(updatesPerThread) {
                    // Every update starts from the same stale snapshot, as cached callers do.
                    if (service.update(snapshot) { it.copy(powerAtLogout = it.powerAtLogout + 1.0) } is Success) {
                        successes.incrementAndGet()
                    }
                }
            }
        }
        start.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS))

        val stored = repository.getPlayer(snapshot.id)!!
        assertTrue(successes.get() > 0)
        assertEquals(successes.get().toDouble(), stored.powerAtLogout, 1e-9, "every reported success must be in storage")
        assertEquals(snapshot.version + successes.get(), stored.version)
    }

    private fun saved(player: MfPlayer) = (service.save(player) as Success).value

    private fun newId() = MfPlayerId(UUID.randomUUID().toString())
}
