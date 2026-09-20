package com.dansplugins.factionsystem.db

import org.jooq.SQLDialect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.sql.DriverManager

class MfJdbcTest {

    private val defaultUrl = "jdbc:h2:./medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false"
    private val embeddedUrl = "jdbc:h2:./medieval_factions_db;MODE=MYSQL;DATABASE_TO_UPPER=false"

    @Test
    fun hardenUrl_leavesTheDefaultAutoServerUrlAlone() {
        // H2 2.1.214 refuses AUTO_SERVER=TRUE together with DB_CLOSE_ON_EXIT=FALSE (50100), and
        // the release gate caught exactly that on the default config (run 35481312415).
        assertEquals(defaultUrl, MfJdbc.hardenUrl(defaultUrl))
        assertEquals("jdbc:h2:./db;auto_server = true", MfJdbc.hardenUrl("jdbc:h2:./db;auto_server = true"))
        val explicitOff = "jdbc:h2:./db;AUTO_SERVER=FALSE"
        assertEquals("$explicitOff;DB_CLOSE_ON_EXIT=FALSE", MfJdbc.hardenUrl(explicitOff))
    }

    @Test
    fun hardenUrl_appendsCloseOnExitFalse_toAnEmbeddedH2Url() {
        assertEquals("$embeddedUrl;DB_CLOSE_ON_EXIT=FALSE", MfJdbc.hardenUrl(embeddedUrl))
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

    @Test
    fun hardenUrl_producesAUrlH2Accepts_forTheDefaultAndAnEmbeddedStore() {
        // The bundled H2 opens both what the default config produces (auto-server, no
        // setting appended) and an embedded URL with the setting appended. On file stores in
        // a temp dir, not mem:, because AUTO_SERVER is what the rule turns on.
        val dir = java.nio.file.Files.createTempDirectory("mfjdbc").toFile()
        try {
            val urls = listOf(
                MfJdbc.hardenUrl("jdbc:h2:$dir/auto;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false"),
                MfJdbc.hardenUrl("jdbc:h2:$dir/embedded;MODE=MYSQL;DATABASE_TO_UPPER=false")
            )
            for (url in urls) {
                DriverManager.getConnection(url, "sa", "").use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT 1").use { rs ->
                            rs.next()
                            assertEquals(1, rs.getInt(1))
                        }
                    }
                }
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun hardenUrl_producesAUrlH2Accepts_alongsideTheDefaultSettings() {
        val url = MfJdbc.hardenUrl("jdbc:h2:mem:mfjdbc;MODE=MYSQL;DATABASE_TO_UPPER=false")
        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT 1").use { rs ->
                    rs.next()
                    assertEquals(1, rs.getInt(1))
                }
            }
        }
    }
}
