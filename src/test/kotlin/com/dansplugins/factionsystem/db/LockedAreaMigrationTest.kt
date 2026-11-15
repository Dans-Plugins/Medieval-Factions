package com.dansplugins.factionsystem.db

import com.dansplugins.factionsystem.lockedarea.JooqMfLockedAreaRepository
import com.dansplugins.factionsystem.locks.JooqMfLockRepository
import com.dansplugins.factionsystem.locks.MfLockedBlockId
import com.dansplugins.factionsystem.player.MfPlayerId
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

/**
 * V14 (#1393) only adds the locked area tables: data stored before it, such as a block lock with an
 * accessor, is untouched, and there are no locked areas until one is made.
 */
class LockedAreaMigrationTest {

    @Test
    fun `data stored before V14 survives it and no locked areas appear`() {
        val dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:mf_${UUID.randomUUID()};MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1")
        }
        migrate(dataSource, target = "9")

        val worldId = UUID.randomUUID().toString()
        val ownerId = UUID.randomUUID().toString()
        val accessorId = UUID.randomUUID().toString()
        val factionId = UUID.randomUUID().toString()
        val lockId = MfLockedBlockId.generate().value
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                listOf(ownerId, accessorId).forEach { id ->
                    statement.executeUpdate(
                        "insert into `mf_player`(`id`, `version`, `power`, `bypass_enabled`, `power_at_logout`) values ('$id', 1, 5, false, 5)"
                    )
                }
                statement.executeUpdate(
                    "insert into `mf_faction`(`id`, `version`, `name`, `description`, `flags`, `bonus_power`, `autoclaim`, `roles`, `default_role_id`, `default_permissions`) " +
                        "values ('$factionId', 1, 'Lockers', '', '{}', 0, false, '[]', '${UUID.randomUUID()}', '{}')"
                )
                statement.executeUpdate("insert into `mf_claimed_chunk`(`world_id`, `x`, `z`, `faction_id`) values ('$worldId', 0, 0, '$factionId')")
                statement.executeUpdate(
                    "insert into `mf_locked_block`(`id`, `version`, `world_id`, `x`, `y`, `z`, `chunk_x`, `chunk_z`, `player_id`) " +
                        "values ('$lockId', 3, '$worldId', 1, 64, 1, 0, 0, '$ownerId')"
                )
                statement.executeUpdate("insert into `mf_locked_block_accessor`(`locked_block_id`, `player_id`) values ('$lockId', '$accessorId')")
            }
        }

        migrate(dataSource, target = "14")

        val locks = JooqMfLockRepository(DSL.using(dataSource, SQLDialect.H2, Settings().withRenderSchema(false)))
        val lock = locks.getLockedBlocks().single()
        assertEquals(lockId, lock.id.value)
        assertEquals(3, lock.version)
        assertEquals(MfPlayerId(ownerId), lock.playerId)
        assertEquals(listOf(MfPlayerId(accessorId)), lock.accessors)
        assertTrue(JooqMfLockedAreaRepository(DSL.using(dataSource, SQLDialect.H2, Settings().withRenderSchema(false))).getLockedAreas().isEmpty())
    }

    private fun migrate(dataSource: DataSource, target: String) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:com/dansplugins/factionsystem/db/migration")
            .table("mf_schema_history")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .validateOnMigrate(false)
            .target(target)
            .load()
            .migrate()
    }
}
