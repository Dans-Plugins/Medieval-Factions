package com.dansplugins.factionsystem.db

import org.h2.jdbcx.JdbcDataSource
import org.jooq.SQLDialect
import java.util.UUID
import javax.sql.DataSource

/** The optimistic-lock contract on H2 in MySQL mode with jOOQ's H2 dialect: the default `database` settings. */
class H2VersionedRepositoryTest : VersionedRepositoryContract() {
    override val dialect = SQLDialect.H2

    override fun openDataSource(): DataSource = JdbcDataSource().apply {
        setURL("jdbc:h2:mem:mf_${UUID.randomUUID()};MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1")
    }
}
