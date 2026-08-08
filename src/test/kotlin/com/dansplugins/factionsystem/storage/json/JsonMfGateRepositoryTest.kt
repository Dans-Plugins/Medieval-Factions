package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateId
import com.dansplugins.factionsystem.gate.MfGateStatus
import org.bukkit.Material
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
import java.io.File
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger

class JsonMfGateRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfGateRepository

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        repository = JsonMfGateRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
    }

    private fun gate(
        id: MfGateId = MfGateId.generate(),
        version: Int = 0,
        factionId: MfFactionId = MfFactionId.generate(),
        worldId: UUID = UUID.randomUUID(),
        material: Material = Material.OAK_FENCE,
        status: MfGateStatus = MfGateStatus.CLOSED
    ) = MfGate(
        plugin,
        id,
        version,
        factionId,
        MfCuboidArea(MfBlockPosition(worldId, 1, 2, 3), MfBlockPosition(worldId, 4, 5, 6)),
        MfBlockPosition(worldId, 7, 8, 9),
        material,
        status
    )

    @Test
    fun `round trips a gate`() {
        val gateId = MfGateId.generate()
        val factionId = MfFactionId.generate()
        val worldId = UUID.randomUUID()

        val saved = repository.upsert(
            gate(id = gateId, factionId = factionId, worldId = worldId, material = Material.IRON_BARS, status = MfGateStatus.OPEN)
        )
        assertEquals(1, saved.version)

        val read = repository.getGate(gateId)
        assertNotNull(read)
        requireNotNull(read)
        assertEquals(gateId, read.id)
        assertEquals(factionId, read.factionId)
        assertEquals(Material.IRON_BARS, read.material)
        assertEquals(MfGateStatus.OPEN, read.status)
        assertEquals(MfBlockPosition(worldId, 1, 2, 3), read.area.position1)
        assertEquals(MfBlockPosition(worldId, 4, 5, 6), read.area.position2)
        assertEquals(MfBlockPosition(worldId, 7, 8, 9), read.trigger)

        val onDisk = File(tempDir.toFile(), "gates.json")
        assertTrue(onDisk.exists())
        assertTrue(!onDisk.readText().contains("\"plugin\""), "serialized gate must not contain a plugin field")
    }

    @Test
    fun `updates an existing gate and rejects a stale version`() {
        val gateId = MfGateId.generate()
        val first = repository.upsert(gate(id = gateId))
        assertEquals(1, first.version)

        val second = repository.upsert(first.copy(status = MfGateStatus.OPENING))
        assertEquals(2, second.version)
        assertEquals(MfGateStatus.OPENING, repository.getGate(gateId)?.status)
        assertEquals(1, repository.getGates().size)

        assertThrows<OptimisticLockingFailureException> { repository.upsert(gate(id = gateId, version = 0)) }
    }

    @Test
    fun `deletes a gate by id and all gates for a faction`() {
        val factionId = MfFactionId.generate()
        val otherFactionId = MfFactionId.generate()
        val doomed = MfGateId.generate()

        repository.upsert(gate(id = doomed, factionId = factionId))
        repository.upsert(gate(factionId = factionId))
        repository.upsert(gate(factionId = otherFactionId))

        repository.delete(doomed)
        assertNull(repository.getGate(doomed))
        assertEquals(2, repository.getGates().size)

        repository.deleteAll(factionId)
        assertEquals(1, repository.getGates().size)
        assertEquals(otherFactionId, repository.getGates().single().factionId)
    }

    @Test
    fun `skips gates referencing an unknown material instead of failing the whole read`() {
        repository.upsert(gate())
        val file = File(tempDir.toFile(), "gates.json")
        file.writeText(file.readText().replace("\"OAK_FENCE\"", "\"NOT_A_REAL_MATERIAL\""))

        assertTrue(repository.getGates().isEmpty())
    }
}
