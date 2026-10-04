package com.dansplugins.factionsystem.db

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.claim.JooqMfClaimedChunkRepository
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.duel.JooqMfDuelRepository
import com.dansplugins.factionsystem.duel.MfDuel
import com.dansplugins.factionsystem.duel.MfDuelId
import com.dansplugins.factionsystem.faction.JooqMfFactionRepository
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionInvite
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.gate.JooqMfGateCreationContextRepository
import com.dansplugins.factionsystem.gate.JooqMfGateRepository
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateCreationContext
import com.dansplugins.factionsystem.gate.MfGateId
import com.dansplugins.factionsystem.gate.MfGateStatus
import com.dansplugins.factionsystem.law.JooqMfLawRepository
import com.dansplugins.factionsystem.law.MfLaw
import com.dansplugins.factionsystem.law.MfLawId
import com.dansplugins.factionsystem.locks.JooqMfLockRepository
import com.dansplugins.factionsystem.locks.MfLockedBlock
import com.dansplugins.factionsystem.player.JooqMfPlayerRepository
import com.dansplugins.factionsystem.player.JooqMfPlayerRepositoryTest
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.plugin.PluginManager
import org.flywaydb.core.Flyway
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

/**
 * The optimistic-lock contract every versioned jOOQ repository must keep on every supported
 * database (#2076): a write carrying the stored version is applied and bumps the version, a new
 * row is inserted at version 1, and a write carrying any other version is rejected with an
 * [OptimisticLockingFailureException] and leaves the stored row (and its child rows) untouched.
 *
 * On MariaDB and MySQL, the former single guarded upsert reported a skipped stale write as one
 * affected row, so the stale write was dropped and reported as a success. Each subclass runs this
 * contract against one database: H2 as configured by default, H2 driven through jOOQ's MariaDB
 * dialect, and a real MariaDB server.
 *
 * Every test works on rows with fresh random ids, so the tests can share one migrated database.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class VersionedRepositoryContract {

    /** A data source for an empty database, which is migrated once for the whole class. */
    protected abstract fun openDataSource(): DataSource

    protected abstract val dialect: SQLDialect

    /**
     * The dialect used to create the parent factions that other rows refer to. It differs from
     * [dialect] only where the database cannot store faction rows written with [dialect].
     */
    protected open val fixtureDialect: SQLDialect get() = dialect

    /** Whether faction rows written with [dialect] read back correctly on this database. */
    protected open val factionRowsRoundTrip: Boolean = true

    private lateinit var dsl: DSLContext
    private lateinit var fixtureDsl: DSLContext
    private lateinit var plugin: MedievalFactions
    private lateinit var bukkit: MockedStatic<Bukkit>

    private lateinit var players: JooqMfPlayerRepository
    private lateinit var factions: JooqMfFactionRepository
    private lateinit var parentFactions: JooqMfFactionRepository
    private lateinit var claims: JooqMfClaimedChunkRepository
    private lateinit var gates: JooqMfGateRepository
    private lateinit var gateCreationContexts: JooqMfGateCreationContextRepository
    private lateinit var locks: JooqMfLockRepository
    private lateinit var laws: JooqMfLawRepository
    private lateinit var duels: JooqMfDuelRepository

    @BeforeAll
    fun migrate() {
        val dataSource = openDataSource()
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:com/dansplugins/factionsystem/db/migration")
            .table("mf_schema_history")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .validateOnMigrate(false)
            .load()
            .migrate()
        dsl = DSL.using(dataSource, dialect, Settings().withRenderSchema(false))
        fixtureDsl = DSL.using(dataSource, fixtureDialect, Settings().withRenderSchema(false))
    }

    @BeforeEach
    fun createRepositories() {
        plugin = JooqMfPlayerRepositoryTest.powerConfiguredPlugin()
        // Faction roles are deserialized through the static Bukkit plugin manager.
        val pluginManager = mock(PluginManager::class.java)
        `when`(pluginManager.getPlugin("MedievalFactions")).thenReturn(plugin)
        bukkit = mockStatic(Bukkit::class.java)
        bukkit.`when`<PluginManager> { Bukkit.getPluginManager() }.thenReturn(pluginManager)

        players = JooqMfPlayerRepository(plugin, dsl)
        factions = JooqMfFactionRepository(plugin, dsl, Gson())
        parentFactions = JooqMfFactionRepository(plugin, fixtureDsl, Gson())
        claims = JooqMfClaimedChunkRepository(dsl)
        gates = JooqMfGateRepository(plugin, dsl)
        gateCreationContexts = JooqMfGateCreationContextRepository(dsl)
        locks = JooqMfLockRepository(dsl)
        laws = JooqMfLawRepository(dsl)
        duels = JooqMfDuelRepository(dsl)
    }

    @AfterEach
    fun closeStaticMocks() {
        bukkit.close()
    }

    // --- players (fixed by #2069, kept here so every backend is covered) ---

    @Test
    fun `player - a stale save is rejected and the newer row survives`() {
        val original = players.upsert(newPlayer())
        players.upsert(original.copy(power = 9.0))

        assertThrows(OptimisticLockingFailureException::class.java) {
            players.upsert(original.copy(isBypassEnabled = true))
        }

        val stored = players.getPlayer(original.id)!!
        assertEquals(9.0, stored.power)
        assertFalse(stored.isBypassEnabled)
        assertEquals(2, stored.version)
    }

    @Test
    fun `player - a create racing an existing row is rejected`() {
        val existing = players.upsert(newPlayer(power = 12.0))

        assertThrows(OptimisticLockingFailureException::class.java) {
            players.upsert(MfPlayer(existing.id, power = 5.0, powerAtLogout = 5.0))
        }
        assertEquals(12.0, players.getPlayer(existing.id)!!.power)
    }

    // --- factions ---

    @Test
    fun `faction - a new faction is inserted at version 1 and a current save increments the version`() {
        assumeTrue(factionRowsRoundTrip, "faction rows written with $dialect do not read back on this database")
        val saved = factions.upsert(newFaction(name = "Inserted"))
        assertEquals(1, saved.version)

        val updated = factions.upsert(saved.copy(description = "updated"))
        assertEquals(2, updated.version)
        assertEquals("updated", factions.getFaction(saved.id)!!.description)
    }

    @Test
    fun `faction - a stale save is rejected and leaves the faction and its members untouched`() {
        assumeTrue(factionRowsRoundTrip, "faction rows written with $dialect do not read back on this database")
        val member = players.upsert(newPlayer())
        val original = factions.upsert(newFaction(name = "Stale"))
        val newer = factions.upsert(
            original.copy(prefix = "NEW", members = listOf(MfFactionMember(member.id, original.roles.default)))
        )

        assertThrows(OptimisticLockingFailureException::class.java) {
            factions.upsert(original.copy(description = "stale", members = emptyList(), invites = listOf(MfFactionInvite(member.id))))
        }

        val stored = factions.getFaction(original.id)!!
        assertEquals(newer.version, stored.version)
        assertEquals("NEW", stored.prefix)
        assertEquals("", stored.description)
        assertEquals(listOf(member.id), stored.members.map { it.playerId }, "the stale save must not replace the members")
        assertTrue(stored.invites.isEmpty(), "the stale save must not write invites")
    }

    @Test
    fun `faction - a create racing an existing faction is rejected`() {
        assumeTrue(factionRowsRoundTrip, "faction rows written with $dialect do not read back on this database")
        val existing = factions.upsert(newFaction(name = "First"))

        assertThrows(OptimisticLockingFailureException::class.java) {
            factions.upsert(newFaction(id = existing.id, name = "Second"))
        }
        assertEquals(existing.name, factions.getFaction(existing.id)!!.name)
    }

    // --- claims (no version column: a claim save is last-writer-wins on every backend) ---

    @Test
    fun `claim - a save inserts and a later save reassigns the chunk`() {
        val first = parentFactions.upsert(newFaction(name = "ClaimFirst"))
        val second = parentFactions.upsert(newFaction(name = "ClaimSecond"))
        val claim = MfClaimedChunk(UUID.randomUUID(), 3, -4, first.id)

        assertEquals(first.id, claims.upsert(claim).factionId)
        assertEquals(second.id, claims.upsert(claim.copy(factionId = second.id)).factionId)
        assertEquals(second.id, claims.getClaim(claim.worldId, 3, -4)!!.factionId)
    }

    // --- gates ---

    @Test
    fun `gate - a new gate is inserted at version 1 and a current save increments the version`() {
        val saved = gates.upsert(newGate(parentFactions.upsert(newFaction(name = "GateInsert")).id))
        assertEquals(1, saved.version)

        val updated = gates.upsert(saved.copy(status = MfGateStatus.OPEN))
        assertEquals(2, updated.version)
        assertEquals(MfGateStatus.OPEN, gates.getGate(saved.id)!!.status)
    }

    @Test
    fun `gate - a stale save is rejected and the newer row survives`() {
        val original = gates.upsert(newGate(parentFactions.upsert(newFaction(name = "GateStale")).id))
        gates.upsert(original.copy(status = MfGateStatus.OPENING))

        assertThrows(OptimisticLockingFailureException::class.java) {
            gates.upsert(original.copy(status = MfGateStatus.CLOSING))
        }

        val stored = gates.getGate(original.id)!!
        assertEquals(MfGateStatus.OPENING, stored.status)
        assertEquals(2, stored.version)
    }

    @Test
    fun `gate - a create racing an existing gate is rejected`() {
        val faction = parentFactions.upsert(newFaction(name = "GateRace"))
        val existing = gates.upsert(newGate(faction.id, material = Material.OAK_PLANKS))

        assertThrows(OptimisticLockingFailureException::class.java) {
            gates.upsert(newGate(faction.id, id = existing.id, material = Material.IRON_BARS))
        }
        assertEquals(Material.OAK_PLANKS, gates.getGate(existing.id)!!.material)
    }

    @Test
    fun `gate - an insert that fails for another reason is rethrown, not reported as a conflict`() {
        // No faction row exists for this id, so the insert breaks the faction foreign key.
        assertThrows(DataAccessException::class.java) {
            gates.upsert(newGate(MfFactionId.generate()))
        }
    }

    // --- gate creation contexts ---

    @Test
    fun `gate creation context - a stale save is rejected and the newer row survives`() {
        val player = players.upsert(newPlayer())
        val world = UUID.randomUUID()
        val original = gateCreationContexts.upsert(MfGateCreationContext(player.id))
        assertEquals(1, original.version)
        gateCreationContexts.upsert(original.copy(position1 = MfBlockPosition(world, 1, 2, 3)))

        assertThrows(OptimisticLockingFailureException::class.java) {
            gateCreationContexts.upsert(original.copy(position1 = MfBlockPosition(world, 9, 9, 9)))
        }

        val stored = gateCreationContexts.getContext(player.id)!!
        assertEquals(MfBlockPosition(world, 1, 2, 3), stored.position1)
        assertEquals(2, stored.version)
    }

    @Test
    fun `gate creation context - a create racing an existing context is rejected`() {
        val player = players.upsert(newPlayer())
        val world = UUID.randomUUID()
        gateCreationContexts.upsert(MfGateCreationContext(player.id, position1 = MfBlockPosition(world, 1, 2, 3)))

        assertThrows(OptimisticLockingFailureException::class.java) {
            gateCreationContexts.upsert(MfGateCreationContext(player.id))
        }
        assertEquals(MfBlockPosition(world, 1, 2, 3), gateCreationContexts.getContext(player.id)!!.position1)
    }

    // --- locks ---

    @Test
    fun `lock - a stale save is rejected and leaves the accessors untouched`() {
        val owner = players.upsert(newPlayer())
        val accessor = players.upsert(newPlayer())
        val other = players.upsert(newPlayer())
        val original = locks.upsert(newLockedBlock(owner.id))
        assertEquals(1, original.version)
        val newer = locks.upsert(original.copy(accessors = listOf(accessor.id)))
        assertEquals(2, newer.version)

        assertThrows(OptimisticLockingFailureException::class.java) {
            locks.upsert(original.copy(accessors = listOf(other.id)))
        }

        val stored = locks.getLockedBlock(original.id)!!
        assertEquals(2, stored.version)
        assertEquals(listOf(accessor.id), stored.accessors, "the stale save must not replace the accessors")
    }

    @Test
    fun `lock - a create racing an existing lock is rejected`() {
        val owner = players.upsert(newPlayer())
        val accessor = players.upsert(newPlayer())
        val existing = locks.upsert(newLockedBlock(owner.id).copy(accessors = listOf(accessor.id)))

        assertThrows(OptimisticLockingFailureException::class.java) {
            locks.upsert(existing.copy(version = 0, accessors = emptyList()))
        }
        assertEquals(listOf(accessor.id), locks.getLockedBlock(existing.id)!!.accessors)
    }

    // --- laws ---

    @Test
    fun `law - a new law is numbered after the existing ones and an update keeps its number`() {
        val faction = parentFactions.upsert(newFaction(name = "LawNumbers"))
        val first = laws.upsert(MfLaw(faction, "first"))
        val second = laws.upsert(MfLaw(faction, "second"))
        assertEquals(1, first.number)
        assertEquals(2, second.number)

        val edited = laws.upsert(first.copy(text = "first, edited"))
        assertEquals(first.version + 1, edited.version)
        assertEquals(1, edited.number)
        assertEquals("first, edited", edited.text)
    }

    @Test
    fun `law - a stale save is rejected and the newer row survives`() {
        val faction = parentFactions.upsert(newFaction(name = "LawStale"))
        val original = laws.upsert(MfLaw(faction, "original"))
        laws.upsert(original.copy(text = "newer"))

        assertThrows(OptimisticLockingFailureException::class.java) {
            laws.upsert(original.copy(text = "stale"))
        }

        val stored = laws.getLaw(original.id)!!
        assertEquals("newer", stored.text)
        assertEquals(original.version + 1, stored.version)
    }

    @Test
    fun `law - a create racing an existing law is rejected`() {
        val faction = parentFactions.upsert(newFaction(name = "LawRace"))
        val existing = laws.upsert(MfLaw(MfLawId.generate(), 0, faction.id, "existing", null))

        assertThrows(OptimisticLockingFailureException::class.java) {
            laws.upsert(MfLaw(existing.id, 0, faction.id, "racing", null))
        }
        assertEquals("existing", laws.getLaw(existing.id)!!.text)
    }

    // --- duels ---

    @Test
    fun `duel - a stale save is rejected and the newer row survives`() {
        val original = duels.upsert(newDuel())
        assertEquals(1, original.version)
        duels.upsert(original.copy(challengerHealth = 4.0))

        assertThrows(OptimisticLockingFailureException::class.java) {
            duels.upsert(original.copy(challengedHealth = 1.0))
        }

        val stored = duels.getDuels().single { it.id == original.id }
        assertEquals(4.0, stored.challengerHealth)
        assertEquals(20.0, stored.challengedHealth)
        assertEquals(2, stored.version)
    }

    @Test
    fun `duel - a create racing an existing duel is rejected`() {
        val existing = duels.upsert(newDuel())

        assertThrows(OptimisticLockingFailureException::class.java) {
            duels.upsert(existing.copy(version = 0, challengerHealth = 1.0))
        }
        assertEquals(20.0, duels.getDuels().single { it.id == existing.id }.challengerHealth)
    }

    @Test
    fun `duel - a save of a deleted duel inserts it again at version 1, as before`() {
        val existing = duels.upsert(newDuel())
        duels.delete(existing.id)

        // The former upsert inserted a row whose id no longer existed, whatever version the
        // write carried. That is kept: only a row that exists at another version is a conflict.
        val reinserted = duels.upsert(existing.copy(challengerHealth = 2.0))
        assertEquals(1, reinserted.version)
        assertEquals(2.0, reinserted.challengerHealth)
    }

    // --- builders ---

    private fun newPlayer(power: Double = 5.0) =
        MfPlayer(MfPlayerId(UUID.randomUUID().toString()), name = "Steve", power = power, powerAtLogout = power)

    private fun newFaction(id: MfFactionId = MfFactionId.generate(), name: String): MfFaction {
        val member = MfFactionRole(plugin, MfFactionRoleId.generate(), "Member", mapOf("claim" to true))
        return MfFaction(
            plugin = plugin,
            id = id,
            // Faction names are not unique in the schema, but keep them distinct per run.
            name = "$name-${UUID.randomUUID().toString().take(8)}",
            flags = MfFlagValues(plugin, emptyMap()),
            roles = MfFactionRoles(member.id, listOf(member)),
            defaultPermissionsByName = emptyMap()
        )
    }

    private fun newGate(factionId: MfFactionId, id: MfGateId = MfGateId.generate(), material: Material = Material.OAK_PLANKS): MfGate {
        val world = UUID.randomUUID()
        return MfGate(
            plugin,
            id,
            0,
            factionId,
            MfCuboidArea(MfBlockPosition(world, 0, 60, 0), MfBlockPosition(world, 2, 62, 0)),
            MfBlockPosition(world, 3, 60, 0),
            material,
            MfGateStatus.CLOSED
        )
    }

    private fun newLockedBlock(ownerId: MfPlayerId): MfLockedBlock {
        val world = UUID.randomUUID()
        val faction = parentFactions.upsert(newFaction(name = "LockOwner"))
        claims.upsert(MfClaimedChunk(world, 0, 0, faction.id))
        return MfLockedBlock(
            block = MfBlockPosition(world, 1, 64, 1),
            chunkX = 0,
            chunkZ = 0,
            playerId = ownerId,
            accessors = emptyList()
        )
    }

    private fun newDuel(): MfDuel {
        val challenger = players.upsert(newPlayer())
        val challenged = players.upsert(newPlayer())
        val world = UUID.randomUUID()
        return MfDuel(
            id = MfDuelId.generate(),
            challengerId = challenger.id,
            challengedId = challenged.id,
            challengerHealth = 20.0,
            challengedHealth = 20.0,
            endTime = Instant.now().plus(2, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS),
            challengerLocation = MfPosition(world, 1.0, 64.0, 1.0, 0f, 0f),
            challengedLocation = null
        )
    }
}
