package com.dansplugins.factionsystem.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.SQLDialect
import java.net.UnknownHostException
import java.sql.SQLException
import java.util.logging.Logger

/**
 * Shared handling of the `database.url` / `database.dialect` config values, used wherever the
 * plugin opens a datasource (startup and both legs of `/faction migrate`).
 */
object MfJdbc {

    private const val H2_PREFIX = "jdbc:h2:"
    private const val CLOSE_ON_EXIT_SETTING = "DB_CLOSE_ON_EXIT"
    private val AUTO_SERVER_ON = Regex(";\\s*AUTO_SERVER\\s*=\\s*TRUE\\s*(;|$)", RegexOption.IGNORE_CASE)

    /** H2's generic I/O error code, which is what a failed auto-server start surfaces as. */
    private const val H2_IO_EXCEPTION = 90028

    /**
     * Opens the connection pool for a configured `database.url`, after [hardenUrl] and
     * [preloadDriver].
     *
     * An H2 URL in `AUTO_SERVER=TRUE` mode makes H2 start a TCP server when it opens the
     * database, and to do that it resolves the host's own name (`InetAddress.getLocalHost()`).
     * On hosts whose name does not resolve — common for containers and game-server panels,
     * where the host name is a random ID — the open fails with error 90028 caused by an
     * `UnknownHostException`, and the plugin cannot enable at all (#1803).
     *
     * Only in exactly that case the pool is opened again with `AUTO_SERVER=TRUE` removed from
     * the URL. The database path and every other setting are kept as written, so the same
     * database file is opened, only without the TCP server. That server is what lets anything
     * else reach the file while the plugin holds it: an add-on bundling its own H2 (Currencies
     * opens `medieval_factions_db`) runs in its own classloader with its own H2 engine, so it
     * cannot open the file in this mode, and neither can outside tools.
     * The configured value is not changed, and a warning says what happened. Any other
     * failure is rethrown unchanged.
     */
    fun openDataSource(configuredUrl: String, username: String?, password: String?, logger: Logger): HikariDataSource {
        val jdbcUrl = hardenUrl(configuredUrl)
        try {
            return newDataSource(jdbcUrl, username, password)
        } catch (exception: Exception) {
            if (!isAutoServerH2Url(jdbcUrl) || !isUnresolvableHostFailure(exception)) throw exception
            val fallbackUrl = hardenUrl(withoutAutoServer(configuredUrl))
            logger.warning(
                "H2 could not start its auto-server because this host's name does not resolve " +
                    "(${rootMessage(exception)}). Opening the same database file without AUTO_SERVER instead. " +
                    "While this server runs, nothing else can open that file: add-on plugins that share it " +
                    "(such as Currencies, which uses medieval_factions_db by default) will fail to connect, " +
                    "and so will external tools such as the H2 console. To fix this, make the host name " +
                    "resolve (e.g. add it to /etc/hosts); if nothing else needs the file, removing " +
                    "AUTO_SERVER=true from database.url in config.yml also stops this warning."
            )
            return newDataSource(fallbackUrl, username, password)
        }
    }

    private fun newDataSource(jdbcUrl: String, username: String?, password: String?): HikariDataSource {
        preloadDriver(jdbcUrl)
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = jdbcUrl
        if (username != null) {
            hikariConfig.username = username
        }
        if (password != null) {
            hikariConfig.password = password
        }
        return HikariDataSource(hikariConfig)
    }

    fun isAutoServerH2Url(url: String): Boolean =
        url.startsWith(H2_PREFIX, ignoreCase = true) && AUTO_SERVER_ON.containsMatchIn(url)

    /**
     * Returns [url] with its `AUTO_SERVER=TRUE` setting removed. The database path (everything
     * before the first `;`) and all other settings are left exactly as written.
     */
    fun withoutAutoServer(url: String): String {
        var result = url
        while (AUTO_SERVER_ON.containsMatchIn(result)) {
            result = AUTO_SERVER_ON.replace(result) { match -> match.groupValues[1] }
        }
        check(result.substringBefore(';') == url.substringBefore(';')) {
            "Removing AUTO_SERVER changed the database path of $url"
        }
        return result
    }

    /**
     * True when [exception] is H2's I/O error 90028 caused by the host's own name not resolving,
     * which is how a failed auto-server start surfaces. Nothing else qualifies, so genuine I/O
     * errors on the database file are never retried.
     */
    fun isUnresolvableHostFailure(exception: Throwable): Boolean {
        val chain = generateSequence(exception) { it.cause.takeUnless { cause -> cause === it } }.take(20).toList()
        return chain.any { it is SQLException && it.errorCode == H2_IO_EXCEPTION } &&
            chain.any { it is UnknownHostException }
    }

    private fun rootMessage(exception: Throwable): String =
        generateSequence(exception) { it.cause.takeUnless { cause -> cause === it } }.take(20).last().toString()

    /**
     * Returns the URL the datasource should actually be opened with.
     *
     * For embedded H2 this appends `;DB_CLOSE_ON_EXIT=FALSE` unless the operator has set the
     * setting themselves. By default H2 registers a JVM shutdown hook that closes any database
     * still open at exit. Bukkit unloads the plugin's classloader long before that hook runs,
     * so if anything (another plugin sharing the file, a leaked connection) keeps the database
     * open past `onDisable`, the hook fails inside an unloaded classloader and leaves the store
     * unflushed with a `*.trace.db` beside it. The plugin closes its pool explicitly in
     * `onDisable`, which closes the database while the classloader is still alive; the hook adds
     * nothing on the happy path and is the thing that breaks on the unhappy one.
     *
     * H2 refuses the combination `AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE` ("Feature not
     * supported", 50100) — and the default URL runs in auto-server mode so that other
     * plugins can share the file. Such a URL is therefore returned unchanged: the exit hook
     * stays, and the explicit close in `onDisable` is what keeps it from ever having work
     * to do. Every other URL is returned unchanged too.
     */
    fun hardenUrl(url: String): String {
        if (!url.startsWith(H2_PREFIX, ignoreCase = true)) return url
        if (url.contains(CLOSE_ON_EXIT_SETTING, ignoreCase = true)) return url
        if (AUTO_SERVER_ON.containsMatchIn(url)) return url
        return "$url;$CLOSE_ON_EXIT_SETTING=FALSE"
    }

    /**
     * Resolves `database.dialect` to a jOOQ dialect, case-insensitively, accepting the spellings
     * the documentation has used (`MySQL`, `MariaDB`, `PostgreSQL`) as well as the enum names.
     * A null or blank value returns null so jOOQ falls back to its default dialect, as before.
     *
     * @throws IllegalArgumentException naming the accepted values when the value is not one of them
     */
    fun parseDialect(name: String?): SQLDialect? {
        val trimmed = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val canonical = when (trimmed.uppercase()) {
            "POSTGRESQL" -> "POSTGRES"
            else -> trimmed.uppercase()
        }
        return try {
            SQLDialect.valueOf(canonical)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Unknown database.dialect '$trimmed'. Accepted values: H2, MYSQL, MARIADB, POSTGRES."
            )
        }
    }

    /**
     * Loads the JDBC driver class for a URL whose driver is known to be bundled, so the plugin's
     * relocated drivers are registered even where JDBC's service loading does not see them.
     * URLs for drivers that are not bundled are left to JDBC's own driver discovery.
     */
    fun preloadDriver(url: String) {
        val lowerUrl = url.lowercase()
        when {
            lowerUrl.startsWith(H2_PREFIX) -> Class.forName("org.h2.Driver")
            lowerUrl.startsWith("jdbc:mariadb:") -> Class.forName("org.mariadb.jdbc.Driver")
            // For other JDBC URLs, rely on JDBC 4.0+ auto-loading via SPI
        }
    }
}
