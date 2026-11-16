package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfFactionChatChannel
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import org.bukkit.configuration.file.FileConfiguration
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Runs the jOOQ player repository against a real H2 database migrated with the plugin's own
 * Flyway scripts, in the same MySQL compatibility mode as the default `database.url`.
 */
class JooqMfPlayerRepositoryTest {

    private lateinit var dsl: DSLContext
    private lateinit var repository: JooqMfPlayerRepository

    @BeforeEach
    fun setUp() {
        dsl = migratedH2Dsl()
        repository = JooqMfPlayerRepository(powerConfiguredPlugin(), dsl)
    }

    @Test
    fun `upsert inserts a new player at version 1`() {
        val saved = repository.upsert(newPlayer(power = 5.0))

        assertEquals(1, saved.version)
        assertEquals(5.0, saved.power)
    }

    @Test
    fun `upsert with the current version updates the row and increments the version`() {
        val saved = repository.upsert(newPlayer(power = 5.0))

        val updated = repository.upsert(saved.copy(power = 7.0, chatChannel = MfFactionChatChannel.ALLIES))

        assertEquals(2, updated.version)
        assertEquals(7.0, updated.power)
        assertEquals(MfFactionChatChannel.ALLIES, updated.chatChannel)
    }

    @Test
    fun `a stale write is rejected instead of overwriting a newer row`() {
        val original = repository.upsert(newPlayer(power = 5.0))
        // Two writers start from the same snapshot; the first one wins.
        repository.upsert(original.copy(power = 9.0))

        assertThrows(OptimisticLockingFailureException::class.java) {
            repository.upsert(original.copy(isBypassEnabled = true))
        }

        val stored = repository.getPlayer(original.id)!!
        assertEquals(9.0, stored.power, "the first writer's power change must survive the stale write")
        assertEquals(false, stored.isBypassEnabled, "the stale write must not have been applied")
        assertEquals(2, stored.version)
    }

    @Test
    fun `a write from a snapshot taken before the scheduled power task is rejected`() {
        val snapshot = repository.upsert(newPlayer(power = 5.0))
        // The scheduled power task raises power and bumps the version in SQL, outside the entity.
        repository.increaseOnlinePlayerPower(listOf(snapshot.id))
        val afterTask = repository.getPlayer(snapshot.id)!!
        assertEquals(snapshot.version + 1, afterTask.version)

        assertThrows(OptimisticLockingFailureException::class.java) {
            repository.upsert(snapshot.copy(powerAtLogout = snapshot.power))
        }
        assertEquals(afterTask.power, repository.getPlayer(snapshot.id)!!.power, "the power increase must not be rolled back")
    }

    @Test
    fun `a create racing an existing row is rejected rather than resetting it`() {
        val id = MfPlayerId(UUID.randomUUID().toString())
        repository.upsert(MfPlayer(id, power = 12.0, powerAtLogout = 12.0))

        assertThrows(OptimisticLockingFailureException::class.java) {
            repository.upsert(MfPlayer(id, power = 5.0, powerAtLogout = 5.0))
        }
        assertEquals(12.0, repository.getPlayer(id)!!.power)
    }

    @Test
    fun `a player stored before fixed power existed loads with no fixed power after the migration`() {
        // A database at V14, the last schema before #1682, holding a player row.
        val dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:mf_${UUID.randomUUID()};MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1")
        }
        flyway(dataSource, target = "14").migrate()
        val id = UUID.randomUUID().toString()
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    "insert into `mf_player` (`id`, `version`, `name`, `power`, `power_at_logout`, `bypass_enabled`, `chat_channel`) " +
                        "values ('$id', 4, 'OldTimer', 7.5, 6.0, false, null)"
                )
            }
        }

        flyway(dataSource, target = null).migrate()
        val upgraded = JooqMfPlayerRepository(powerConfiguredPlugin(), DSL.using(dataSource, SQLDialect.H2, Settings().withRenderSchema(false)))
        val loaded = upgraded.getPlayer(MfPlayerId(id))!!

        assertNull(loaded.fixedPower)
        assertFalse(loaded.isPowerFixed)
        assertEquals(7.5, loaded.power)
        assertEquals(6.0, loaded.powerAtLogout)
        assertEquals(4, loaded.version)
        assertEquals("OldTimer", loaded.name)
    }

    @Test
    fun `fixed power round trips and can be cleared`() {
        val saved = repository.upsert(newPlayer(power = 5.0).copy(fixedPower = 5.0))

        assertEquals(5.0, repository.getPlayer(saved.id)!!.fixedPower)

        val cleared = repository.upsert(saved.copy(fixedPower = null))
        assertNull(cleared.fixedPower)
        assertNull(repository.getPlayer(saved.id)!!.fixedPower)
    }

    @Test
    fun `the power cycle skips players whose power is fixed`() {
        val fixedOnline = repository.upsert(newPlayer(power = 5.0).copy(fixedPower = 5.0))
        val normalOnline = repository.upsert(newPlayer(power = 5.0))
        val fixedOffline = repository.upsert(newPlayer(power = 15.0).copy(fixedPower = 15.0))
        val normalOffline = repository.upsert(newPlayer(power = 15.0))
        val online = listOf(fixedOnline.id, normalOnline.id)

        repository.increaseOnlinePlayerPower(online)
        repository.decreaseOfflinePlayerPower(online)

        assertEquals(fixedOnline, repository.getPlayer(fixedOnline.id), "a fixed player must not gain power or be touched")
        assertEquals(fixedOffline, repository.getPlayer(fixedOffline.id), "a fixed player must not lose power or be touched")
        assertTrue(repository.getPlayer(normalOnline.id)!!.power > 5.0, "an unfixed online player still gains power")
        assertTrue(repository.getPlayer(normalOffline.id)!!.power < 15.0, "an unfixed offline player still loses power")
    }

    private fun newPlayer(power: Double) =
        MfPlayer(MfPlayerId(UUID.randomUUID().toString()), name = "Steve", power = power, powerAtLogout = power)

    companion object {
        fun powerConfiguredPlugin(initialPower: Double = 5.0): MedievalFactions {
            val config = mock(FileConfiguration::class.java)
            `when`(config.getDouble("players.minPower")).thenReturn(0.0)
            `when`(config.getDouble("players.maxPower")).thenReturn(20.0)
            `when`(config.getDouble("players.initialPower")).thenReturn(initialPower)
            `when`(config.getDouble("players.hoursToReachMaxPower")).thenReturn(12.0)
            `when`(config.getDouble("players.hoursToReachMinPower")).thenReturn(72.0)
            val plugin = mock(MedievalFactions::class.java)
            `when`(plugin.config).thenReturn(config)
            `when`(plugin.logger).thenReturn(Logger.getLogger("JooqMfPlayerRepositoryTest"))
            return plugin
        }

        fun migratedH2Dsl(): DSLContext {
            val dataSource = JdbcDataSource().apply {
                setURL("jdbc:h2:mem:mf_${UUID.randomUUID()};MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1")
            }
            flyway(dataSource, target = null).migrate()
            return DSL.using(dataSource, SQLDialect.H2, Settings().withRenderSchema(false))
        }

        /** The plugin's Flyway setup; [target] stops at that version, null runs every migration. */
        private fun flyway(dataSource: DataSource, target: String?): Flyway =
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:com/dansplugins/factionsystem/db/migration")
                .table("mf_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false)
                .apply { if (target != null) target(target) }
                .load()
    }
}
