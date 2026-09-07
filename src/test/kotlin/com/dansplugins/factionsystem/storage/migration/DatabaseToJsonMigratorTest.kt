package com.dansplugins.factionsystem.storage.migration

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfChatChannelMessageRepository
import com.dansplugins.factionsystem.claim.MfClaimedChunkRepository
import com.dansplugins.factionsystem.duel.MfDuelInviteRepository
import com.dansplugins.factionsystem.duel.MfDuelRepository
import com.dansplugins.factionsystem.faction.MfFactionRepository
import com.dansplugins.factionsystem.gate.MfGateCreationContextRepository
import com.dansplugins.factionsystem.gate.MfGateRepository
import com.dansplugins.factionsystem.interaction.MfInteractionStatusRepository
import com.dansplugins.factionsystem.law.MfLawRepository
import com.dansplugins.factionsystem.locks.MfLockRepository
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerRepository
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipRepository
import com.dansplugins.factionsystem.storage.json.JsonMfPlayerRepository
import com.dansplugins.factionsystem.storage.json.JsonStorageManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger

/**
 * The migrators must refuse to run into a target that already holds data.
 *
 * Entity versions in the two backends advance independently, so upserting an entity that already
 * exists at a different version throws and aborts the migration partway — leaving the target half
 * written with no indication of how far it got.
 */
class DatabaseToJsonMigratorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var storageManager: JsonStorageManager

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        `when`(plugin.getResource("schemas/players.json"))
            .thenAnswer { java.io.File("src/main/resources/schemas/players.json").inputStream() }
        storageManager = JsonStorageManager(plugin, tempDir.toString())
    }

    /** Builds a migrator whose targets are real JSON repositories and whose sources are inert mocks. */
    private fun migratorWith(targetPlayerRepo: MfPlayerRepository): DatabaseToJsonMigrator {
        val emptyFactionRepo = mock(MfFactionRepository::class.java).also { `when`(it.getFactions()).thenReturn(emptyList()) }
        val emptyRelationshipRepo = mock(MfFactionRelationshipRepository::class.java)
            .also { `when`(it.getFactionRelationships()).thenReturn(emptyList()) }
        val emptyClaimRepo = mock(MfClaimedChunkRepository::class.java).also { `when`(it.getClaims()).thenReturn(emptyList()) }
        val emptyLockRepo = mock(MfLockRepository::class.java).also { `when`(it.getLockedBlocks()).thenReturn(emptyList()) }
        val emptyGateRepo = mock(MfGateRepository::class.java).also { `when`(it.getGates()).thenReturn(emptyList()) }
        val emptyDuelRepo = mock(MfDuelRepository::class.java).also { `when`(it.getDuels()).thenReturn(emptyList()) }
        return DatabaseToJsonMigrator(
            plugin,
            mock(MfPlayerRepository::class.java), mock(MfFactionRepository::class.java),
            mock(MfLawRepository::class.java), mock(MfFactionRelationshipRepository::class.java),
            mock(MfClaimedChunkRepository::class.java), mock(MfLockRepository::class.java),
            mock(MfInteractionStatusRepository::class.java), mock(MfGateRepository::class.java),
            mock(MfGateCreationContextRepository::class.java), mock(MfChatChannelMessageRepository::class.java),
            mock(MfDuelRepository::class.java), mock(MfDuelInviteRepository::class.java),
            targetPlayerRepo, emptyFactionRepo,
            mock(MfLawRepository::class.java), emptyRelationshipRepo,
            emptyClaimRepo, emptyLockRepo,
            mock(MfInteractionStatusRepository::class.java), emptyGateRepo,
            mock(MfGateCreationContextRepository::class.java), mock(MfChatChannelMessageRepository::class.java),
            emptyDuelRepo, mock(MfDuelInviteRepository::class.java)
        )
    }

    @Test
    fun `refuses to migrate into a target that already holds data`() {
        val targetPlayerRepo = JsonMfPlayerRepository(plugin, storageManager)
        targetPlayerRepo.upsert(MfPlayer(MfPlayerId(UUID.randomUUID().toString()), 0, "Existing", 5.0, 5.0, false, null))

        val result = migratorWith(targetPlayerRepo).migrate()

        assertFalse(result.success, "migration into a non-empty target must not be attempted")
        assertEquals(0, result.itemsMigrated)
        assertTrue(
            result.message.contains("1 players"),
            "the refusal should name what is already there, was: ${result.message}"
        )
        assertTrue(
            result.message.contains("not supported"),
            "the refusal should say what to do about it, was: ${result.message}"
        )
    }

    @Test
    fun `proceeds when the target is empty`() {
        val targetPlayerRepo = mock(MfPlayerRepository::class.java)
            .also { `when`(it.getPlayers()).thenReturn(emptyList()) }

        val result = migratorWith(targetPlayerRepo).migrate()

        // The sources are inert mocks returning empty lists, so this migrates nothing but must not be
        // blocked by the pre-flight guard.
        assertTrue(result.success, "an empty target must not trip the guard, was: ${result.message}")
    }
}
