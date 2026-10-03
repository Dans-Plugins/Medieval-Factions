package com.dansplugins.factionsystem.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetAddress
import java.net.URLClassLoader
import java.net.UnknownHostException
import java.nio.file.Path
import java.sql.Driver
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

/**
 * #1803: on a host whose own name does not resolve, H2 cannot start the auto-server that the
 * default `database.url` asks for, and fails with 90028. The pool is then opened on the same
 * database file without AUTO_SERVER.
 */
class MfJdbcAutoServerFallbackTest {

    @TempDir
    lateinit var tempDir: Path

    private val defaultSettings = ";AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false"

    @Test
    fun withoutAutoServer_removesOnlyTheAutoServerSetting() {
        assertEquals(
            "jdbc:h2:./medieval_factions_db;MODE=MYSQL;DATABASE_TO_UPPER=false",
            MfJdbc.withoutAutoServer("jdbc:h2:./medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false")
        )
        assertEquals("jdbc:h2:./db;MODE=MYSQL", MfJdbc.withoutAutoServer("jdbc:h2:./db;MODE=MYSQL;auto_server = TRUE"))
        assertEquals("jdbc:h2:./db", MfJdbc.withoutAutoServer("jdbc:h2:./db;AUTO_SERVER=TRUE;AUTO_SERVER=TRUE"))
        assertEquals("jdbc:h2:/srv/mc/plugins/MedievalFactions/db", MfJdbc.withoutAutoServer("jdbc:h2:/srv/mc/plugins/MedievalFactions/db"))
        assertEquals("jdbc:h2:./db;AUTO_SERVER=FALSE", MfJdbc.withoutAutoServer("jdbc:h2:./db;AUTO_SERVER=FALSE"))
    }

    @Test
    fun isAutoServerH2Url_onlyMatchesH2UrlsWithAutoServerOn() {
        assertTrue(MfJdbc.isAutoServerH2Url("jdbc:h2:./db$defaultSettings"))
        assertFalse(MfJdbc.isAutoServerH2Url("jdbc:h2:./db;AUTO_SERVER=FALSE"))
        assertFalse(MfJdbc.isAutoServerH2Url("jdbc:h2:./db;MODE=MYSQL"))
        assertFalse(MfJdbc.isAutoServerH2Url("jdbc:mariadb://localhost/db?x=;AUTO_SERVER=TRUE"))
    }

    @Test
    fun isUnresolvableHostFailure_requiresBoth90028AndAnUnknownHost() {
        val hostFailure = SQLException("IO Exception", "08000", 90028, UnknownHostException("a1b2c3: Name or service not known"))
        assertTrue(MfJdbc.isUnresolvableHostFailure(hostFailure))
        // Hikari wraps the driver's exception in a PoolInitializationException.
        assertTrue(MfJdbc.isUnresolvableHostFailure(RuntimeException("Failed to initialize pool", hostFailure)))

        assertFalse(MfJdbc.isUnresolvableHostFailure(SQLException("IO Exception: disk full", "08000", 90028)), "a genuine I/O error on the file")
        assertFalse(MfJdbc.isUnresolvableHostFailure(SQLException("Database may be already in use", "90020", 90020, UnknownHostException("x"))))
        assertFalse(MfJdbc.isUnresolvableHostFailure(RuntimeException(UnknownHostException("x"))))
    }

    @Test
    fun openDataSource_opensTheDefaultUrlWithAutoServerWhereTheHostResolves() {
        val dbPath = File(tempDir.toFile(), "medieval_factions_db").absolutePath
        val warnings = capturedWarnings()
        MfJdbc.openDataSource("jdbc:h2:$dbPath$defaultSettings", "sa", "", warnings.logger).use { dataSource ->
            assertTrue(dataSource.jdbcUrl.contains("AUTO_SERVER=true"), "the configured URL must be used as is")
            dataSource.connection.use { it.createStatement().execute("SELECT 1") }
        }
        assertEquals(emptyList<String>(), warnings.messages)
    }

    @Test
    fun openDataSource_withAutoServer_letsAnAddOnWithItsOwnH2ShareTheFile() {
        // This is why the default URL turns AUTO_SERVER on: an add-on bundling its own H2
        // (Currencies) opens medieval_factions_db while the plugin holds it, through the TCP
        // server the plugin's H2 starts.
        val dbPath = File(tempDir.toFile(), "medieval_factions_db").absolutePath
        val url = "jdbc:h2:$dbPath$defaultSettings"
        MfJdbc.openDataSource(url, "sa", "", capturedWarnings().logger).use { dataSource ->
            dataSource.connection.use { it.createStatement().execute("CREATE TABLE `probe` (`value` VARCHAR(64))") }
            assertEquals("OK", AddOnH2.tryOpen(url))
        }
    }

    @Test
    fun openDataSource_rethrowsOtherFailuresWithoutRetrying() {
        val warnings = capturedWarnings()
        // An H2 URL that fails for a reason other than the host name: an unknown setting.
        val url = "jdbc:h2:${File(tempDir.toFile(), "db").absolutePath};AUTO_SERVER=true;NOT_A_SETTING=1"
        val exception = assertThrows(Exception::class.java) { MfJdbc.openDataSource(url, "sa", "", warnings.logger) }
        assertFalse(MfJdbc.isUnresolvableHostFailure(exception))
        assertEquals(emptyList<String>(), warnings.messages)
        assertFalse(File(tempDir.toFile(), "db.mv.db").exists())
    }

    @Test
    fun openDataSource_onAHostWhoseNameDoesNotResolve_fallsBackToTheSameFileWithoutAutoServer() {
        val hostName = InetAddress.getLocalHost().hostName
        assumeFalse(hostName.equals("localhost", ignoreCase = true), "the host name must be one a hosts file can leave out")
        // A hosts file without this machine's name makes InetAddress.getLocalHost() fail in the
        // child JVM exactly as on the hosts in #1803. The setting is read once per JVM, hence the fork.
        val hostsFile = File(tempDir.toFile(), "hosts").apply { writeText("127.0.0.1 localhost\n") }
        val dbPath = File(tempDir.toFile(), "medieval_factions_db").absolutePath

        // An existing install: the database already holds data before the failing start.
        DriverManager.getConnection("jdbc:h2:$dbPath;MODE=MYSQL;DATABASE_TO_UPPER=false", "sa", "").use { connection ->
            connection.createStatement().execute("CREATE TABLE `probe` (`value` VARCHAR(64))")
            connection.createStatement().execute("INSERT INTO `probe` VALUES ('existing-before-start')")
        }

        val output = runProbe(hostsFile, dbPath)
        println(output)

        assertTrue(output.contains("WITHOUT_FALLBACK: 90028"), "the child JVM must reproduce 90028 first, output:\n$output")
        assertTrue(output.contains("FALLBACK_URL: jdbc:h2:$dbPath;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE"), output)
        assertTrue(output.contains("WARNING: H2 could not start its auto-server"), output)
        // Without the auto-server, an add-on with its own H2 (Currencies) cannot open the file while
        // the plugin holds it, whether its URL keeps AUTO_SERVER (90028 again) or not (90020,
        // "Database may be already in use"). This is the consequence the warning names.
        assertTrue(output.contains("ADDON_OWN_H2_DEFAULT_URL: "), output)
        assertFalse(output.contains("ADDON_OWN_H2_DEFAULT_URL: OK"), "output:\n$output")
        assertFalse(output.contains("ADDON_OWN_H2_EMBEDDED_URL: OK"), "output:\n$output")
        assertTrue(output.contains("PROBE: DONE"), output)

        // The pre-existing row survived the failed start, and the rows the child wrote are in the configured file.
        DriverManager.getConnection("jdbc:h2:$dbPath;MODE=MYSQL;DATABASE_TO_UPPER=false", "sa", "").use { connection ->
            connection.createStatement().executeQuery("SELECT `value` FROM `probe` ORDER BY `value` DESC").use { rs ->
                val values = generateSequence { if (rs.next()) rs.getString(1) else null }.toList()
                assertEquals(listOf("written-through-fallback", "written-after-addon-attempts", "existing-before-start"), values)
            }
        }
    }

    private fun runProbe(hostsFile: File, dbPath: String): String {
        val java = File(System.getProperty("java.home"), "bin/java").absolutePath
        val process = ProcessBuilder(
            java,
            "-Djdk.net.hosts.file=${hostsFile.absolutePath}",
            "-cp",
            System.getProperty("java.class.path"),
            AutoServerFallbackProbe::class.java.name,
            dbPath
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertTrue(process.waitFor(60, TimeUnit.SECONDS), "probe timed out:\n$output")
        assertEquals(0, process.exitValue(), "probe failed:\n$output")
        return output
    }

    private class CapturedWarnings(val logger: Logger, val messages: MutableList<String>)

    private fun capturedWarnings(): CapturedWarnings {
        val messages = mutableListOf<String>()
        val logger = Logger.getAnonymousLogger().apply {
            useParentHandlers = false
            addHandler(object : Handler() {
                override fun publish(record: LogRecord) { messages += record.message }
                override fun flush() {}
                override fun close() {}
            })
        }
        return CapturedWarnings(logger, messages)
    }
}

/** Runs in a child JVM whose own host name does not resolve. */
object AutoServerFallbackProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        val dbPath = args[0]
        val defaultUrl = "jdbc:h2:$dbPath;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false"

        try {
            DriverManager.getConnection(defaultUrl, "sa", "").close()
            println("WITHOUT_FALLBACK: OK")
        } catch (e: SQLException) {
            println("WITHOUT_FALLBACK: ${e.errorCode}")
        }

        val logger = Logger.getAnonymousLogger().apply {
            useParentHandlers = false
            addHandler(object : Handler() {
                override fun publish(record: LogRecord) { println("${record.level}: ${record.message}") }
                override fun flush() {}
                override fun close() {}
            })
        }
        MfJdbc.openDataSource(defaultUrl, "sa", "", logger).use { dataSource ->
            println("FALLBACK_URL: ${dataSource.jdbcUrl}")
            dataSource.connection.use { connection ->
                connection.createStatement().execute("CREATE TABLE IF NOT EXISTS `probe` (`value` VARCHAR(64))")
                connection.createStatement().execute("INSERT INTO `probe` VALUES ('written-through-fallback')")
            }
            // An add-on such as Currencies opens the same file with its own copy of H2: Bukkit gives
            // each plugin its own classloader, so it does not share this H2 engine.
            println("ADDON_OWN_H2_DEFAULT_URL: ${AddOnH2.tryOpen(defaultUrl)}")
            println("ADDON_OWN_H2_EMBEDDED_URL: ${AddOnH2.tryOpen(MfJdbc.withoutAutoServer(defaultUrl))}")
            // The add-on's failed attempts must leave the plugin's own pool working.
            dataSource.connection.use { connection ->
                connection.createStatement().execute("INSERT INTO `probe` VALUES ('written-after-addon-attempts')")
            }
        }
        println("PROBE: DONE")
    }
}

/**
 * Opens [url] through a copy of the H2 driver loaded in a separate classloader, the way an add-on
 * plugin that bundles its own H2 (Currencies) reaches `medieval_factions_db`. Returns "OK" or the
 * H2 error code.
 */
object AddOnH2 {
    fun tryOpen(url: String): String {
        val h2Jar = System.getProperty("java.class.path").split(File.pathSeparator)
            .single { File(it).name.startsWith("h2-") && it.endsWith(".jar") }
        URLClassLoader(arrayOf(File(h2Jar).toURI().toURL()), ClassLoader.getPlatformClassLoader()).use { loader ->
            val driver = Class.forName("org.h2.Driver", true, loader).getDeclaredConstructor().newInstance() as Driver
            check(driver.javaClass.classLoader === loader)
            val properties = Properties().apply {
                setProperty("user", "sa")
                setProperty("password", "")
            }
            return try {
                driver.connect(url, properties)!!.use { connection ->
                    connection.createStatement().executeQuery("SELECT COUNT(*) FROM `probe`").use { it.next() }
                }
                "OK"
            } catch (e: SQLException) {
                "${e.errorCode}"
            }
        }
    }
}
