package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.lockedarea.MfLockedArea
import com.dansplugins.factionsystem.player.MfPlayerId
import org.junit.jupiter.api.Assertions.assertEquals
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

class JsonMfLockedAreaRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
    }

    private fun repository() = JsonMfLockedAreaRepository(JsonStorageManager(plugin, tempDir.toString()))

    @Test
    fun `storage without locked_areas json has no areas`() {
        assertTrue(repository().getLockedAreas().isEmpty())
    }

    @Test
    fun `round trips an area, rejects a stale save and deletes it`() {
        val world = UUID.randomUUID()
        val area = MfLockedArea(
            factionId = MfFactionId.generate(),
            name = "Vault",
            area = MfCuboidArea(MfBlockPosition(world, 0, 60, 0), MfBlockPosition(world, 10, 70, 10)),
            accessors = listOf(MfPlayerId(UUID.randomUUID().toString())),
            accessorRoleIds = listOf(MfFactionRoleId.generate())
        )
        val saved = repository().upsert(area)
        assertEquals(1, saved.version)

        assertEquals(saved, repository().getLockedAreas().single())

        repository().upsert(saved.copy(accessors = emptyList()))
        assertThrows<OptimisticLockingFailureException> { repository().upsert(saved) }

        repository().delete(saved.id)
        assertTrue(repository().getLockedAreas().isEmpty())
    }
}
