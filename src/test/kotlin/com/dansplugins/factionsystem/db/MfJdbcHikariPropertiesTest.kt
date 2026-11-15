package com.dansplugins.factionsystem.db

import com.zaxxer.hikari.HikariConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import java.util.logging.Logger

/**
 * Covers the optional `hikaricp.properties` file. See https://github.com/Dans-Plugins/Medieval-Factions/issues/1710.
 */
class MfJdbcHikariPropertiesTest {

    private val logger = Logger.getLogger(MfJdbcHikariPropertiesTest::class.java.name)
    private val url = "jdbc:h2:mem:hikari-test"

    @Test
    fun `without the file the pool keeps HikariCP's defaults`() {
        val built = MfJdbc.hikariConfig(url, "sa", "", null)
        val defaults = HikariConfig()
        assertEquals(url, built.jdbcUrl)
        assertEquals("sa", built.username)
        assertEquals("", built.password)
        assertEquals(defaults.maximumPoolSize, built.maximumPoolSize)
        assertEquals(defaults.connectionTimeout, built.connectionTimeout)
        assertEquals(defaults.maxLifetime, built.maxLifetime)
    }

    @Test
    fun `settings in the file are applied`() {
        val properties = Properties().apply {
            setProperty("maximumPoolSize", "4")
            setProperty("connectionTimeout", "5000")
            setProperty("poolName", "mf-pool")
            setProperty("dataSource.cachePrepStmts", "true")
        }
        val built = MfJdbc.hikariConfig(url, null, null, properties)
        assertEquals(4, built.maximumPoolSize)
        assertEquals(5000L, built.connectionTimeout)
        assertEquals("mf-pool", built.poolName)
        assertEquals("true", built.dataSourceProperties.getProperty("cachePrepStmts"))
    }

    @Test
    fun `config yml's url, username and password take precedence over the file`() {
        val properties = Properties().apply {
            setProperty("jdbcUrl", "jdbc:h2:mem:from-file")
            setProperty("username", "fileUser")
            setProperty("password", "filePassword")
        }
        val built = MfJdbc.hikariConfig(url, "configUser", "configPassword", properties)
        assertEquals(url, built.jdbcUrl)
        assertEquals("configUser", built.username)
        assertEquals("configPassword", built.password)
    }

    @Test
    fun `the file's username and password are kept when config yml sets none`() {
        val properties = Properties().apply {
            setProperty("username", "fileUser")
            setProperty("password", "filePassword")
        }
        val built = MfJdbc.hikariConfig(url, null, null, properties)
        assertEquals("fileUser", built.username)
        assertEquals("filePassword", built.password)
    }

    @Test
    fun `an unknown setting is refused rather than ignored`() {
        val properties = Properties().apply { setProperty("maximumPoolSizeTypo", "4") }
        assertThrows(RuntimeException::class.java) { MfJdbc.hikariConfig(url, null, null, properties) }
    }

    @Test
    fun `no file means no properties`(@TempDir dataFolder: File) {
        assertNull(MfJdbc.loadHikariProperties(dataFolder, logger))
    }

    @Test
    fun `the file is read from the data folder`(@TempDir dataFolder: File) {
        File(dataFolder, MfJdbc.HIKARI_PROPERTIES_FILE_NAME).writeText("maximumPoolSize=3\nminimumIdle=1\n")
        val properties = MfJdbc.loadHikariProperties(dataFolder, logger)
        assertNotNull(properties)
        assertEquals("3", properties!!.getProperty("maximumPoolSize"))
        assertEquals(3, MfJdbc.hikariConfig(url, null, null, properties).maximumPoolSize)
    }
}
