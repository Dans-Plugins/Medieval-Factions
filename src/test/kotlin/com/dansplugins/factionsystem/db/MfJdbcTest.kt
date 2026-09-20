package com.dansplugins.factionsystem.db

import org.jooq.SQLDialect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MfJdbcTest {

    private val defaultUrl = "jdbc:h2:./medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false"

    @Test
    fun hardenUrl_appendsCloseOnExitFalse_toTheDefaultH2Url() {
        assertEquals("$defaultUrl;DB_CLOSE_ON_EXIT=FALSE", MfJdbc.hardenUrl(defaultUrl))
    }

    @Test
    fun hardenUrl_matchesTheH2PrefixCaseInsensitively() {
        assertEquals("JDBC:H2:./db;DB_CLOSE_ON_EXIT=FALSE", MfJdbc.hardenUrl("JDBC:H2:./db"))
    }

    @Test
    fun hardenUrl_leavesAnOperatorSettingAlone() {
        val explicitTrue = "$defaultUrl;DB_CLOSE_ON_EXIT=TRUE"
        assertEquals(explicitTrue, MfJdbc.hardenUrl(explicitTrue))
        val explicitFalse = "$defaultUrl;db_close_on_exit=false"
        assertEquals(explicitFalse, MfJdbc.hardenUrl(explicitFalse))
    }

    @Test
    fun hardenUrl_leavesOtherDatabasesUnchanged() {
        val mariadb = "jdbc:mariadb://localhost:3306/medievalfactions"
        assertEquals(mariadb, MfJdbc.hardenUrl(mariadb))
        val postgres = "jdbc:postgresql://localhost:5432/medievalfactions"
        assertEquals(postgres, MfJdbc.hardenUrl(postgres))
        assertEquals("", MfJdbc.hardenUrl(""))
    }

    @Test
    fun parseDialect_acceptsEnumNames() {
        assertEquals(SQLDialect.H2, MfJdbc.parseDialect("H2"))
        assertEquals(SQLDialect.MYSQL, MfJdbc.parseDialect("MYSQL"))
        assertEquals(SQLDialect.MARIADB, MfJdbc.parseDialect("MARIADB"))
        assertEquals(SQLDialect.POSTGRES, MfJdbc.parseDialect("POSTGRES"))
    }

    @Test
    fun parseDialect_acceptsTheDocumentedSpellings() {
        assertEquals(SQLDialect.MYSQL, MfJdbc.parseDialect("MySQL"))
        assertEquals(SQLDialect.MARIADB, MfJdbc.parseDialect("MariaDB"))
        assertEquals(SQLDialect.POSTGRES, MfJdbc.parseDialect("PostgreSQL"))
        assertEquals(SQLDialect.H2, MfJdbc.parseDialect(" h2 "))
    }

    @Test
    fun parseDialect_nullOrBlank_returnsNull() {
        assertNull(MfJdbc.parseDialect(null))
        assertNull(MfJdbc.parseDialect(""))
        assertNull(MfJdbc.parseDialect("   "))
    }

    @Test
    fun parseDialect_unknownValue_namesTheAcceptedOnes() {
        val e = assertThrows(IllegalArgumentException::class.java) { MfJdbc.parseDialect("NotADatabase") }
        assertEquals("Unknown database.dialect 'NotADatabase'. Accepted values: H2, MYSQL, MARIADB, POSTGRES.", e.message)
    }
}
