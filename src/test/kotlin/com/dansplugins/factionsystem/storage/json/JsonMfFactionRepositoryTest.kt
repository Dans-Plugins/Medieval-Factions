package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionApplication
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionInvite
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.UnreadableJsonFileException
import com.dansplugins.factionsystem.player.MfPlayerId
import com.google.gson.Gson
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

/**
 * Round-trip tests for [JsonMfFactionRepository].
 *
 * These deliberately serve the real `schemas/factions.json` resource through the mocked plugin, so that
 * schema validation actually runs. Mocking `getResource` away leaves validation disabled and lets shape
 * mismatches between the schema and the serialized DTO go unnoticed.
 */
class JsonMfFactionRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var repository: JsonMfFactionRepository

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        `when`(plugin.getResource("schemas/factions.json"))
            .thenAnswer { File("src/main/resources/schemas/factions.json").inputStream() }
        repository = JsonMfFactionRepository(plugin, JsonStorageManager(plugin, tempDir.toString()), Gson())
    }

    private fun role(name: String, permissions: Map<String, Boolean?> = emptyMap()) =
        MfFactionRole(plugin, MfFactionRoleId.generate(), name, permissions)

    private fun faction(
        id: MfFactionId = MfFactionId.generate(),
        version: Int = 0,
        name: String = "TestFaction",
        members: List<MfFactionMember> = emptyList(),
        roles: MfFactionRoles,
        prefix: String? = null,
        home: MfPosition? = null,
        invites: List<MfFactionInvite> = emptyList(),
        applications: List<MfFactionApplication> = emptyList(),
        flags: Map<String, Any?> = emptyMap()
    ) = MfFaction(
        plugin = plugin,
        id = id,
        version = version,
        name = name,
        description = "a test faction",
        members = members,
        invites = invites,
        flags = MfFlagValues(plugin, flags),
        prefix = prefix,
        home = home,
        bonusPower = 3.5,
        autoclaim = true,
        roles = roles,
        defaultPermissionsByName = mapOf("disband" to false, "claim" to true),
        applications = applications
    )

    @Test
    fun `upsert writes a schema-valid factions file`() {
        val owner = role("Owner", mapOf("disband" to true))
        val saved = repository.upsert(faction(roles = MfFactionRoles(owner.id, listOf(owner))))

        assertEquals(1, saved.version)
        val onDisk = File(tempDir.toFile(), "factions.json")
        assertTrue(onDisk.exists(), "factions.json should have been written")
        // The plugin reference must never reach the file.
        assertTrue(!onDisk.readText().contains("\"plugin\""), "serialized faction must not contain a plugin field")
    }

    @Test
    fun `round trips a fully populated faction`() {
        val owner = role("Owner", mapOf("disband" to true, "claim" to null))
        val member = role("Member")
        val roles = MfFactionRoles(member.id, listOf(owner, member))
        val ownerPlayerId = MfPlayerId(UUID.randomUUID().toString())
        val memberPlayerId = MfPlayerId(UUID.randomUUID().toString())
        val invitedPlayerId = MfPlayerId(UUID.randomUUID().toString())
        val applicantId = MfPlayerId(UUID.randomUUID().toString())
        val factionId = MfFactionId.generate()
        val worldId = UUID.randomUUID()

        repository.upsert(
            faction(
                id = factionId,
                name = "Round Trip",
                members = listOf(MfFactionMember(ownerPlayerId, owner), MfFactionMember(memberPlayerId, member)),
                roles = roles,
                prefix = "[RT]",
                home = MfPosition(worldId, 1.5, 64.0, -2.5, 90.0f, 45.0f),
                invites = listOf(MfFactionInvite(invitedPlayerId)),
                applications = listOf(MfFactionApplication(factionId, applicantId)),
                flags = mapOf("isPeaceful" to true)
            )
        )

        val read = repository.getFaction(factionId)
        assertNotNull(read)
        requireNotNull(read)

        assertEquals("Round Trip", read.name)
        assertEquals("a test faction", read.description)
        assertEquals(1, read.version)
        assertEquals("[RT]", read.prefix)
        assertEquals(3.5, read.bonusPower)
        assertTrue(read.autoclaim)

        assertEquals(MfPosition(worldId, 1.5, 64.0, -2.5, 90.0f, 45.0f), read.home)

        assertEquals(2, read.members.size)
        assertEquals(owner.id, read.getRole(ownerPlayerId)?.id)
        assertEquals("Owner", read.getRole(ownerPlayerId)?.name)
        assertEquals(mapOf("disband" to true), read.getRole(ownerPlayerId)?.permissionsByName)
        assertEquals(member.id, read.getRole(memberPlayerId)?.id)

        assertEquals(listOf(invitedPlayerId), read.invites.map { it.playerId })
        assertEquals(listOf(applicantId), read.applications.map { it.applicantId })
        assertEquals(factionId, read.applications.single().factionId)

        assertEquals(member.id, read.roles.defaultRoleId)
        assertEquals(setOf(owner.id, member.id), read.roles.roles.map { it.id }.toSet())
        assertEquals(mapOf("isPeaceful" to true), read.flags.valuesByName)
        assertEquals(mapOf("disband" to false, "claim" to true), read.defaultPermissionsByName)

        // The plugin reference must be re-attached on read, or every derived property NPEs.
        assertNotNull(read.roles.roles.first().plugin)
    }

    @Test
    fun `round trips a faction with no home and no prefix`() {
        val owner = role("Owner")
        val factionId = MfFactionId.generate()
        repository.upsert(faction(id = factionId, roles = MfFactionRoles(owner.id, listOf(owner))))

        val read = repository.getFaction(factionId)
        assertNotNull(read)
        assertNull(read?.home)
        assertNull(read?.prefix)
    }

    @Test
    fun `finds factions by name and by member`() {
        val owner = role("Owner")
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        val factionId = MfFactionId.generate()
        repository.upsert(
            faction(
                id = factionId,
                name = "Findable",
                members = listOf(MfFactionMember(playerId, owner)),
                roles = MfFactionRoles(owner.id, listOf(owner))
            )
        )

        assertEquals(factionId, repository.getFaction("Findable")?.id)
        assertEquals(factionId, repository.getFaction(playerId)?.id)
        assertNull(repository.getFaction("Nonexistent"))
        assertNull(repository.getFaction(MfPlayerId(UUID.randomUUID().toString())))
    }

    @Test
    fun `updates an existing faction and increments its version`() {
        val owner = role("Owner")
        val roles = MfFactionRoles(owner.id, listOf(owner))
        val factionId = MfFactionId.generate()

        val first = repository.upsert(faction(id = factionId, name = "Before", roles = roles))
        assertEquals(1, first.version)

        val second = repository.upsert(first.copy(name = "After"))
        assertEquals(2, second.version)

        assertEquals(1, repository.getFactions().size)
        assertEquals("After", repository.getFaction(factionId)?.name)
    }

    @Test
    fun `rejects a stale version`() {
        val owner = role("Owner")
        val roles = MfFactionRoles(owner.id, listOf(owner))
        val factionId = MfFactionId.generate()
        repository.upsert(faction(id = factionId, roles = roles))

        assertThrows<OptimisticLockingFailureException> {
            repository.upsert(faction(id = factionId, version = 0, roles = roles))
        }
    }

    @Test
    fun `deletes a faction`() {
        val owner = role("Owner")
        val factionId = MfFactionId.generate()
        repository.upsert(faction(id = factionId, roles = MfFactionRoles(owner.id, listOf(owner))))

        repository.delete(factionId)

        assertNull(repository.getFaction(factionId))
        assertTrue(repository.getFactions().isEmpty())
    }

    @Test
    fun `refuses to overwrite a corrupted factions file, and backs it up`() {
        val factionsFile = File(tempDir.toFile(), "factions.json")
        factionsFile.writeText("{ this is not json")

        assertTrue(repository.getFactions().isEmpty())
        assertTrue(
            File(tempDir.toFile(), "factions.json.corrupted.backup").exists(),
            "a corrupted file should be backed up rather than silently discarded"
        )

        val owner = role("Owner")
        assertThrows<UnreadableJsonFileException> {
            repository.upsert(faction(roles = MfFactionRoles(owner.id, listOf(owner))))
        }
        assertEquals(
            "{ this is not json",
            factionsFile.readText(),
            "a file that could not be read must not be replaced by what was loaded in its place"
        )
    }

    @Test
    fun `resumes writing once a corrupted factions file is repaired`() {
        val factionsFile = File(tempDir.toFile(), "factions.json")
        factionsFile.writeText("{ this is not json")
        assertTrue(repository.getFactions().isEmpty())

        factionsFile.writeText("""{"factions":[]}""")

        val owner = role("Owner")
        val saved = repository.upsert(faction(name = "Repaired", roles = MfFactionRoles(owner.id, listOf(owner))))

        assertEquals("Repaired", repository.getFaction(saved.id)?.name)
    }
}
