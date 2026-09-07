package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.interaction.MfInteractionStatus
import com.dansplugins.factionsystem.player.MfPlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger

class JsonMfInteractionStatusRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfInteractionStatusRepository

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        repository = JsonMfInteractionStatusRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
    }

    private fun playerId() = MfPlayerId(UUID.randomUUID().toString())

    @Test
    fun `round trips every interaction status`() {
        MfInteractionStatus.values().forEach { status ->
            val playerId = playerId()
            repository.setInteractionStatus(playerId, status)
            assertEquals(status, repository.getInteractionStatus(playerId), "status $status should round trip")
        }
    }

    @Test
    fun `overwrites a player's previous status`() {
        val playerId = playerId()
        repository.setInteractionStatus(playerId, MfInteractionStatus.LOCKING)
        repository.setInteractionStatus(playerId, MfInteractionStatus.UNLOCKING)

        assertEquals(MfInteractionStatus.UNLOCKING, repository.getInteractionStatus(playerId))
    }

    @Test
    fun `setting a null status clears it`() {
        val playerId = playerId()
        repository.setInteractionStatus(playerId, MfInteractionStatus.CHECKING_ACCESS)

        repository.setInteractionStatus(playerId, null)

        assertNull(repository.getInteractionStatus(playerId))
    }

    @Test
    fun `keeps players' statuses independent`() {
        val first = playerId()
        val second = playerId()
        repository.setInteractionStatus(first, MfInteractionStatus.ADDING_ACCESSOR)
        repository.setInteractionStatus(second, MfInteractionStatus.SELECTING_GATE_TRIGGER)

        repository.setInteractionStatus(first, null)

        assertNull(repository.getInteractionStatus(first))
        assertEquals(
            MfInteractionStatus.SELECTING_GATE_TRIGGER,
            repository.getInteractionStatus(second),
            "clearing one player's status must not disturb another's"
        )
    }

    @Test
    fun `returns null for a player with no status`() {
        assertNull(repository.getInteractionStatus(playerId()))
    }

    @Test
    fun `clearing an unknown player is a no-op`() {
        repository.setInteractionStatus(playerId(), null)

        assertNull(repository.getInteractionStatus(playerId()))
    }
}
