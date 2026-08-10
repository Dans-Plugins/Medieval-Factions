package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.duel.MfDuelInvite
import com.dansplugins.factionsystem.player.MfPlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger

class JsonMfDuelInviteRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfDuelInviteRepository
    private val inviter = MfPlayerId(UUID.randomUUID().toString())
    private val invitee = MfPlayerId(UUID.randomUUID().toString())

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        repository = JsonMfDuelInviteRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
    }

    @Test
    fun `round trips an invite`() {
        repository.upsert(MfDuelInvite(inviter, invitee))

        val read = repository.getInvite(inviter, invitee)
        assertNotNull(read)
        assertEquals(inviter, read?.inviterId)
        assertEquals(invitee, read?.inviteeId)
    }

    @Test
    fun `treats the invite direction as significant`() {
        repository.upsert(MfDuelInvite(inviter, invitee))

        assertNotNull(repository.getInvite(inviter, invitee))
        assertNull(repository.getInvite(invitee, inviter), "an invite is directional and must not match reversed")
    }

    @Test
    fun `does not duplicate a repeated invite`() {
        repository.upsert(MfDuelInvite(inviter, invitee))
        repository.upsert(MfDuelInvite(inviter, invitee))

        assertEquals(1, repository.getInvites().size)
    }

    @Test
    fun `keeps invites from different players separate`() {
        val otherInviter = MfPlayerId(UUID.randomUUID().toString())
        repository.upsert(MfDuelInvite(inviter, invitee))
        repository.upsert(MfDuelInvite(otherInviter, invitee))

        assertEquals(2, repository.getInvites().size)
        assertNotNull(repository.getInvite(inviter, invitee))
        assertNotNull(repository.getInvite(otherInviter, invitee))
    }

    @Test
    fun `deletes only the named invite`() {
        val otherInviter = MfPlayerId(UUID.randomUUID().toString())
        repository.upsert(MfDuelInvite(inviter, invitee))
        repository.upsert(MfDuelInvite(otherInviter, invitee))

        repository.deleteInvite(inviter, invitee)

        assertNull(repository.getInvite(inviter, invitee))
        assertNotNull(repository.getInvite(otherInviter, invitee))
        assertEquals(1, repository.getInvites().size)
    }

    @Test
    fun `returns nothing when no invites are stored`() {
        assertNull(repository.getInvite(inviter, invitee))
        assertTrue(repository.getInvites().isEmpty())
    }
}
