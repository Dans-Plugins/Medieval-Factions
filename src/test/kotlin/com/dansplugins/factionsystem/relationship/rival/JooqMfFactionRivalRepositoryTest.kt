package com.dansplugins.factionsystem.relationship.rival

import com.dansplugins.factionsystem.faction.MfFactionId
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Runs every migration, including the one adding `mf_faction_rival` (#1896), on H2 in MySQL mode (the default
 * `database` settings), and checks the rival table alongside existing data.
 */
class JooqMfFactionRivalRepositoryTest {

    private lateinit var dsl: DSLContext
    private lateinit var repository: JooqMfFactionRivalRepository
    private val north = MfFactionId.generate()
    private val south = MfFactionId.generate()

    @BeforeEach
    fun setUp() {
        val dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:mf_${UUID.randomUUID()};MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1")
        }
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:com/dansplugins/factionsystem/db/migration")
            .table("mf_schema_history")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .validateOnMigrate(false)
        // Data from before rivals existed: migrate up to the last migration without them, add factions, then
        // migrate the rest. The existing factions must survive and start with no rivals.
        flyway.target("9").load().migrate()
        dsl = DSL.using(dataSource, SQLDialect.H2, Settings().withRenderSchema(false))
        insertFaction(north, "North")
        insertFaction(south, "South")
        flyway.target("latest").load().migrate()
        repository = JooqMfFactionRivalRepository(dsl)
    }

    @Test
    fun `existing factions survive the migration and start with no rivals`() {
        assertEquals(2, dsl.fetchCount(DSL.table("mf_faction")))
        assertEquals(emptyList<MfFactionRival>(), repository.getRivals())
    }

    @Test
    fun `a rival round trips and naming it again keeps the original`() {
        val createdAt = Instant.parse("2026-11-15T12:00:00Z")
        repository.insert(MfFactionRival(north, south, createdAt))
        val again = repository.insert(MfFactionRival(north, south, createdAt.plusSeconds(60)))

        assertEquals(MfFactionRival(north, south, createdAt), again)
        assertEquals(listOf(MfFactionRival(north, south, createdAt)), repository.getRivals())
    }

    @Test
    fun `delete removes one rivalry and leaves the reverse one`() {
        repository.insert(MfFactionRival(north, south))
        repository.insert(MfFactionRival(south, north))

        repository.delete(north, south)

        assertEquals(listOf(south to north), repository.getRivals().map { it.factionId to it.targetId })
    }

    @Test
    fun `deleting a faction removes the rivalries it is part of`() {
        repository.insert(MfFactionRival(north, south))
        repository.insert(MfFactionRival(south, north))

        dsl.deleteFrom(DSL.table("mf_faction")).where(DSL.field("id").eq(south.value)).execute()

        assertEquals(emptyList<MfFactionRival>(), repository.getRivals())
    }

    private fun insertFaction(id: MfFactionId, name: String) {
        dsl.execute(
            "insert into `mf_faction`(`id`, `version`, `name`, `description`, `flags`, `bonus_power`, `autoclaim`, " +
                "`roles`, `default_role_id`, `default_permissions`) values (?, 1, ?, '', '{}', 0, false, '[]', ?, '{}')",
            id.value,
            name,
            UUID.randomUUID().toString()
        )
    }
}
