package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.TestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration
import java.time.Instant

class MfTruceServiceTest {

    private val testUtils = TestUtils()
    private var now: Instant = Instant.parse("2026-11-15T12:00:00Z")

    @TempDir
    lateinit var tempDir: File

    private fun service(minutes: Long, file: File? = null) = MfTruceService({ minutes }, file, { now })

    @Test
    fun `truces are off by default and nothing is enforced`() {
        val service = service(0)
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()

        service.startTruce(a, b)

        assertFalse(service.isEnabled)
        assertNull(service.getRemainingTruce(a, b))
    }

    @Test
    fun `a truce blocks both directions until it ends`() {
        val service = service(60)
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()

        service.startTruce(a, b)

        assertEquals(Duration.ofMinutes(60), service.getRemainingTruce(a, b))
        assertEquals(Duration.ofMinutes(60), service.getRemainingTruce(b, a))

        now = now.plus(Duration.ofMinutes(45))
        assertEquals(Duration.ofMinutes(15), service.getRemainingTruce(b, a))

        now = now.plus(Duration.ofMinutes(15))
        assertNull(service.getRemainingTruce(a, b))
    }

    @Test
    fun `a truce does not affect other factions`() {
        val service = service(60)
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        val c = testUtils.createFactionId()

        service.startTruce(a, b)

        assertNull(service.getRemainingTruce(a, c))
        assertNull(service.getRemainingTruce(c, b))
    }

    @Test
    fun `turning truces off stops enforcing existing truces`() {
        var minutes = 60L
        val service = MfTruceService({ minutes }, null, { now })
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        service.startTruce(a, b)

        minutes = 0

        assertNull(service.getRemainingTruce(a, b))
    }

    @Test
    fun `truces survive a restart`() {
        val file = File(tempDir, "truces.yml")
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        service(60, file).startTruce(a, b)
        assertTrue(file.exists())

        now = now.plus(Duration.ofMinutes(10))
        val reloaded = service(60, file)

        val remaining = reloaded.getRemainingTruce(b, a)
        assertNotNull(remaining)
        assertEquals(Duration.ofMinutes(50), remaining)
    }

    @Test
    fun `expired truces are dropped on load`() {
        val file = File(tempDir, "truces.yml")
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        service(60, file).startTruce(a, b)

        now = now.plus(Duration.ofMinutes(61))
        val reloaded = service(60, file)

        assertNull(reloaded.getRemainingTruce(a, b))
    }
}
