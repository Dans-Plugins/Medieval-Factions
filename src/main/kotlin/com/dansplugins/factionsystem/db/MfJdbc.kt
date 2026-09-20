package com.dansplugins.factionsystem.db

import org.jooq.SQLDialect

/**
 * Shared handling of the `database.url` / `database.dialect` config values, used wherever the
 * plugin opens a datasource (startup and both legs of `/faction migrate`).
 */
object MfJdbc {

    private const val H2_PREFIX = "jdbc:h2:"
    private const val CLOSE_ON_EXIT_SETTING = "DB_CLOSE_ON_EXIT"

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
     * Every other URL is returned unchanged.
     */
    fun hardenUrl(url: String): String {
        if (!url.startsWith(H2_PREFIX, ignoreCase = true)) return url
        if (url.contains(CLOSE_ON_EXIT_SETTING, ignoreCase = true)) return url
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
