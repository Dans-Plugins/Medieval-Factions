package com.dansplugins.factionsystem.trace

import org.bukkit.configuration.Configuration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Decides whether `usage-reporting.enabled` allows reporting, and keeps an operator's opt-out
 * from ever being turned back on by the plugin.
 *
 * Fails closed: only a value that clearly means "on" ([TraceClient.allowsReporting]) turns
 * reporting on, and `config.yml` as it is on disk right now is read directly, so neither a stale
 * copy of the config held in memory nor Bukkit's fallback to the bundled defaults can override
 * an `enabled: false` the operator wrote.
 */
object UsageReportingSetting {
    const val ENABLED_KEY = "usage-reporting.enabled"

    /**
     * The value of `usage-reporting.enabled` written in [configFile], with no defaults applied;
     * null when the file or the key is missing. A file that exists but cannot be parsed reads as
     * false: Bukkit loads such a file as empty, which would fall back to the bundled `true`, but
     * what the operator wrote in it cannot be known.
     */
    fun readFromDisk(configFile: File): Any? {
        if (!configFile.isFile) return null
        val onDisk = YamlConfiguration()
        return try {
            onDisk.load(configFile)
            onDisk.get(ENABLED_KEY) ?: if (hasBlankEnabledLine(configFile)) false else null
        } catch (exception: Exception) {
            false
        }
    }

    /**
     * Whether [configFile] has an `enabled:` line with no value (or only a comment) inside its
     * top-level `usage-reporting:` block. Bukkit drops a key with no value when it loads the file,
     * so the bundled `true` would win; but an operator who blanked the switch did not ask for
     * reporting, so it counts as off.
     */
    internal fun hasBlankEnabledLine(configFile: File): Boolean {
        val lines = try {
            configFile.readLines(Charsets.ISO_8859_1)
        } catch (exception: Exception) {
            return false
        }
        var inBlock = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            if (!line[0].isWhitespace()) {
                inBlock = trimmed.matches(Regex("usage-reporting\\s*:\\s*(#.*)?"))
                continue
            }
            if (inBlock && trimmed.matches(Regex("enabled\\s*:\\s*(#.*)?"))) return true
        }
        return false
    }

    /** Whether [configFile] exists but cannot be parsed as YAML. */
    fun isUnparseable(configFile: File): Boolean {
        if (!configFile.isFile) return false
        return try {
            YamlConfiguration().load(configFile)
            false
        } catch (exception: Exception) {
            true
        }
    }

    /** Whether [configFile] on disk turns reporting off. */
    fun turnedOffOnDisk(configFile: File): Boolean {
        val onDisk = readFromDisk(configFile) ?: return false
        return !TraceClient.allowsReporting(onDisk)
    }

    /**
     * Whether reporting is on: both the loaded [config] (which falls through to the bundled
     * defaults when the file has no block) and [configFile] on disk must allow it.
     */
    fun isEnabled(config: Configuration, configFile: File): Boolean =
        !turnedOffOnDisk(configFile) && TraceClient.allowsReporting(config.get(ENABLED_KEY))

    /**
     * Carries an opt-out written in [configFile] into [config], so that saving [config] over the
     * file cannot turn reporting back on. [config] may be older than the file: an operator can
     * set `enabled: false` by hand while the server is running.
     */
    fun keepOptOutFromDisk(config: Configuration, configFile: File) {
        if (turnedOffOnDisk(configFile)) {
            config.set(ENABLED_KEY, false)
        }
    }
}
