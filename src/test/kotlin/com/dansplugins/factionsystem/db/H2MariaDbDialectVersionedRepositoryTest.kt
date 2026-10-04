package com.dansplugins.factionsystem.db

import org.h2.jdbcx.JdbcDataSource
import org.jooq.SQLDialect
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.UUID
import javax.sql.DataSource

/**
 * The optimistic-lock contract with jOOQ's MariaDB dialect, run on H2 so that it needs no
 * database server and fails locally if #2076 comes back. [MariaDbVersionedRepositoryTest] runs
 * the same contract on a real MariaDB server in CI.
 *
 * H2 runs the SQL that jOOQ renders for MariaDB (in MySQL mode, the mode of the default
 * `database.url`, chosen when Flyway 9.4 did not recognise H2's MariaDB mode). H2 alone would hide
 * the bug, though: it reports 0 rows for an `INSERT … ON DUPLICATE KEY UPDATE` that matched a
 * row and left it unchanged, whereas the MariaDB driver, in its default found-rows mode, reports
 * 1. The data source below reports such statements the way the MariaDB driver does.
 */
class H2MariaDbDialectVersionedRepositoryTest : VersionedRepositoryContract() {
    override val dialect = SQLDialect.MARIADB

    // jOOQ binds JSON values as text for MariaDB, which H2 then stores as a JSON string rather
    // than the array or object it holds, so faction rows written with this dialect do not read
    // back on H2. Parent factions are written with the H2 dialect instead, and the faction
    // repository's own tests are left to H2VersionedRepositoryTest and the real MariaDB server.
    override val fixtureDialect = SQLDialect.H2
    override val factionRowsRoundTrip = false

    override fun openDataSource(): DataSource = withMariaDbFoundRows(
        JdbcDataSource().apply {
            setURL("jdbc:h2:mem:mf_${UUID.randomUUID()};MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1")
        }
    )

    private fun withMariaDbFoundRows(dataSource: DataSource): DataSource =
        proxy(dataSource) { name, _, result ->
            if (name == "getConnection") withMariaDbFoundRows(result as Connection) else result
        }

    private fun withMariaDbFoundRows(connection: Connection): Connection =
        proxy(connection) { name, args, result ->
            if (name == "prepareStatement" && result is PreparedStatement) {
                withMariaDbFoundRows(result, args?.firstOrNull() as? String ?: "")
            } else {
                result
            }
        }

    private fun withMariaDbFoundRows(statement: PreparedStatement, sql: String): PreparedStatement {
        val onDuplicateKeyUpdate = sql.contains("on duplicate key update", ignoreCase = true)
        return proxy(statement) { name, _, result ->
            when {
                !onDuplicateKeyUpdate -> result
                // A matched row is found even when the update leaves it unchanged.
                name in setOf("executeUpdate", "getUpdateCount") && result == 0 -> 1
                name == "executeLargeUpdate" && result == 0L -> 1L
                else -> result
            }
        }
    }

    private inline fun <reified T : Any> proxy(target: T, crossinline map: (String, Array<out Any?>?, Any?) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            val result = try {
                method.invoke(target, *(args ?: emptyArray()))
            } catch (exception: InvocationTargetException) {
                throw exception.targetException
            }
            map(method.name, args, result)
        } as T
}
