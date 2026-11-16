package com.dansplugins.factionsystem.storage.migration

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfChatChannelMessageRepository
import com.dansplugins.factionsystem.claim.MfClaimedChunkRepository
import com.dansplugins.factionsystem.duel.MfDuelInviteRepository
import com.dansplugins.factionsystem.duel.MfDuelRepository
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionRepository
import com.dansplugins.factionsystem.gate.MfGateCreationContextRepository
import com.dansplugins.factionsystem.gate.MfGateRepository
import com.dansplugins.factionsystem.interaction.MfInteractionStatusRepository
import com.dansplugins.factionsystem.law.MfLawRepository
import com.dansplugins.factionsystem.locks.MfLockRepository
import com.dansplugins.factionsystem.player.JooqMfPlayerRepository
import com.dansplugins.factionsystem.player.JooqMfPlayerRepositoryTest
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerRepository
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipRepository
import com.dansplugins.factionsystem.storage.json.JsonMfPlayerRepository
import com.dansplugins.factionsystem.storage.json.JsonStorageManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

/**
 * Mirror of [DatabaseToJsonMigratorTest] for the other direction — both migrators must refuse to run
 * into a target that already holds data.
 */
class JsonToDatabaseMigratorTest {

    private lateinit var plugin: MedievalFactions

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
    }

    private fun migratorWith(
        targetFactionRepo: MfFactionRepository,
        sourcePlayerRepo: MfPlayerRepository = mock(MfPlayerRepository::class.java),
        targetPlayerRepo: MfPlayerRepository = mock(MfPlayerRepository::class.java).also { `when`(it.getPlayers()).thenReturn(emptyList()) }
    ): JsonToDatabaseMigrator {
        val emptyRelationshipRepo = mock(MfFactionRelationshipRepository::class.java)
            .also { `when`(it.getFactionRelationships()).thenReturn(emptyList()) }
        val emptyClaimRepo = mock(MfClaimedChunkRepository::class.java).also { `when`(it.getClaims()).thenReturn(emptyList()) }
        val emptyLockRepo = mock(MfLockRepository::class.java).also { `when`(it.getLockedBlocks()).thenReturn(emptyList()) }
        val emptyGateRepo = mock(MfGateRepository::class.java).also { `when`(it.getGates()).thenReturn(emptyList()) }
        val emptyDuelRepo = mock(MfDuelRepository::class.java).also { `when`(it.getDuels()).thenReturn(emptyList()) }
        return JsonToDatabaseMigrator(
            plugin,
            sourcePlayerRepo, mock(MfFactionRepository::class.java),
            mock(MfLawRepository::class.java), mock(MfFactionRelationshipRepository::class.java),
            mock(MfClaimedChunkRepository::class.java), mock(MfLockRepository::class.java),
            mock(MfInteractionStatusRepository::class.java), mock(MfGateRepository::class.java),
            mock(MfGateCreationContextRepository::class.java), mock(MfChatChannelMessageRepository::class.java),
            mock(MfDuelRepository::class.java), mock(MfDuelInviteRepository::class.java),
            targetPlayerRepo, targetFactionRepo,
            mock(MfLawRepository::class.java), emptyRelationshipRepo,
            emptyClaimRepo, emptyLockRepo,
            mock(MfInteractionStatusRepository::class.java), emptyGateRepo,
            mock(MfGateCreationContextRepository::class.java), mock(MfChatChannelMessageRepository::class.java),
            emptyDuelRepo, mock(MfDuelInviteRepository::class.java)
        )
    }

    @Test
    fun `refuses to migrate into a target that already holds data`() {
        val occupiedFactionRepo = mock(MfFactionRepository::class.java)
            .also { `when`(it.getFactions()).thenReturn(listOf(mock(MfFaction::class.java))) }

        val result = migratorWith(occupiedFactionRepo).migrate()

        assertFalse(result.success, "migration into a non-empty target must not be attempted")
        assertEquals(0, result.itemsMigrated)
        assertTrue(
            result.message.contains("1 factions"),
            "the refusal should name what is already there, was: ${result.message}"
        )
        assertTrue(
            result.message.contains("database"),
            "the refusal should tell the operator how to clear a database target, was: ${result.message}"
        )
    }

    @Test
    fun `proceeds when the target is empty`() {
        val emptyFactionRepo = mock(MfFactionRepository::class.java)
            .also { `when`(it.getFactions()).thenReturn(emptyList()) }

        val result = migratorWith(emptyFactionRepo).migrate()

        assertTrue(result.success, "an empty target must not trip the guard, was: ${result.message}")
    }

    @Test
    fun `carries a player's fixed power, and its absence, from JSON to the database`(@TempDir tempDir: Path) {
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        `when`(plugin.getResource("schemas/players.json"))
            .thenAnswer { java.io.File("src/main/resources/schemas/players.json").inputStream() }
        val sourcePlayerRepo = JsonMfPlayerRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
        val fixed = sourcePlayerRepo.upsert(MfPlayer(MfPlayerId(UUID.randomUUID().toString()), power = 12.0, powerAtLogout = 12.0, fixedPower = 12.0))
        val normal = sourcePlayerRepo.upsert(MfPlayer(MfPlayerId(UUID.randomUUID().toString()), power = 4.0, powerAtLogout = 4.0))
        val targetPlayerRepo = JooqMfPlayerRepository(JooqMfPlayerRepositoryTest.powerConfiguredPlugin(), JooqMfPlayerRepositoryTest.migratedH2Dsl())
        val emptyFactionRepo = mock(MfFactionRepository::class.java).also { `when`(it.getFactions()).thenReturn(emptyList()) }

        val result = migratorWith(emptyFactionRepo, sourcePlayerRepo, targetPlayerRepo).migrate()

        assertTrue(result.success, result.message)
        assertEquals(12.0, targetPlayerRepo.getPlayer(fixed.id)!!.fixedPower)
        assertNull(targetPlayerRepo.getPlayer(normal.id)!!.fixedPower)
    }
}
