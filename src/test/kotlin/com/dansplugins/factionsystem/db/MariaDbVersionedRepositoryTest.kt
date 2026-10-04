package com.dansplugins.factionsystem.db

import org.jooq.SQLDialect
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.mariadb.jdbc.MariaDbDataSource
import org.testcontainers.DockerClientFactory
import org.testcontainers.mariadb.MariaDBContainer
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * The optimistic-lock contract on a real MariaDB server, started with Testcontainers and migrated
 * with the plugin's own Flyway scripts (#2076). Connections use the MariaDB driver's defaults, as
 * the plugin's do, so a matched but unchanged row counts as one affected row.
 *
 * Every test is skipped, not failed, where no Docker daemon is available. GitHub Actions'
 * ubuntu runners have one, so this runs in CI; the build prints each test's outcome so that the
 * log shows whether it ran.
 */
class MariaDbVersionedRepositoryTest : VersionedRepositoryContract() {
    override val dialect = SQLDialect.MARIADB

    private var container: MariaDBContainer? = null

    override fun openDataSource(): DataSource {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable, "Docker is not available, so the MariaDB tests are skipped")
        val started = MariaDBContainer(IMAGE).also { it.start() }
        container = started
        Logger.getLogger(javaClass.name).info("Running the optimistic-lock contract against $IMAGE at ${started.jdbcUrl}")
        return MariaDbDataSource(started.jdbcUrl).apply {
            setUser(started.username)
            setPassword(started.password)
        }
    }

    @AfterAll
    fun stopContainer() {
        container?.stop()
    }

    companion object {
        // The long-term-support series that the plugin's MariaDB users are most likely to run.
        private const val IMAGE = "mariadb:10.11"
    }
}
