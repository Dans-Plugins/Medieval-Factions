package com.dansplugins.factionsystem.trace

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * An operator who sets `usage-reporting.enabled: false` must find reporting off, every time,
 * whatever the plugin does with config.yml afterwards.
 */
class UsageReportingSettingTest {

    @TempDir
    lateinit var folder: File

    private fun configFile(contents: String): File =
        File(folder, "config.yml").apply { writeText(contents) }

    /** What JavaPlugin.getConfig() loads: the file, with the jar's config.yml as defaults. */
    private fun loadLikeBukkit(file: File): YamlConfiguration {
        // Bukkit's loadConfiguration() logs a file it cannot parse and returns it empty.
        val config = YamlConfiguration()
        try {
            config.load(file)
        } catch (exception: Exception) {
            // left empty, as Bukkit does
        }
        javaClass.getResourceAsStream("/config.yml")!!.use { stream ->
            config.setDefaults(YamlConfiguration.loadConfiguration(InputStreamReader(stream, StandardCharsets.UTF_8)))
        }
        return config
    }

    /** What onEnable() does on every start: copy the defaults in, stamp the version, save. */
    private fun startupSave(file: File) {
        val config = loadLikeBukkit(file)
        config.options().copyDefaults(true)
        config.set("version", "9.9.9")
        if (UsageReportingSetting.isUnparseable(file)) return
        UsageReportingSetting.keepOptOutFromDisk(config, file)
        config.save(file)
    }

    @Test
    fun anUpgradeKeepsReportingOff() {
        val file = configFile("version: 5.0.0\nusage-reporting:\n  enabled: false\n")

        startupSave(file)

        assertEquals(false, UsageReportingSetting.readFromDisk(file))
        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file))
    }

    @Test
    fun anOptOutWrittenUnderANearMissNameIsAnOptOut() {
        val nearMisses = listOf(
            "usage_reporting:\n  enabled: false\n",
            "usageReporting:\n  enabled: off\n",
            "Usage-Reporting:\n  enabled: \"false\"\n",
            "usage-reporting: false\n",
            "usage_reporting: no\n",
            "usage-reporting:\n  enable: false\n",
            "usage-reporting:\n  Enabled: false\n",
            "usage-reporting:\n  disabled: true\n",
            "usage-reporting:\n  enabled: true\n  disable: yes\n"
        )
        for (nearMiss in nearMisses) {
            val file = configFile("version: 5.0.0\n$nearMiss")
            assertTrue(UsageReportingSetting.turnedOffOnDisk(file), nearMiss)
            assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file), nearMiss)
        }
    }

    @Test
    fun nearMissNamesThatSayOnAreNotAnOptOut() {
        val on = listOf(
            "usage-reporting:\n  enabled: true\n",
            "usage_reporting:\n  enabled: true\n",
            "usage-reporting: true\n",
            "usage-reporting:\n  enabled: true\n  disabled: false\n",
            "usage-reporting:\n  enabled: true\n  endpoint: https://example.org\n"
        )
        for (config in on) {
            val file = configFile(config)
            assertFalse(UsageReportingSetting.turnedOffOnDisk(file), config)
        }
    }

    @Test
    fun anUpgradeOverANearMissOptOutWritesTheRealSwitchOff() {
        // What an upgrade did to such a file: copyDefaults(true) adds the bundled block,
        // enabled: true, beside the operator's usage_reporting block.
        val file = configFile("version: 5.0.0\nusage_reporting:\n  enabled: false\n")

        startupSave(file)

        assertEquals(false, YamlConfiguration.loadConfiguration(file).get(UsageReportingSetting.ENABLED_KEY))
        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file))
    }

    @Test
    fun aFileWithoutTheBlockReportsByDefault() {
        val file = configFile("version: 5.0.0\n")

        assertTrue(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file))
    }

    @Test
    fun aQuotedFalseTurnsReportingOff() {
        val file = configFile("usage-reporting:\n  enabled: \"false\"\n")
        // Bukkit's getBoolean() returns the bundled default, true, for the string "false".
        assertTrue(loadLikeBukkit(file).getBoolean(UsageReportingSetting.ENABLED_KEY))

        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file))
    }

    @Test
    fun anUnrecognisedValueTurnsReportingOff() {
        val file = configFile("usage-reporting:\n  enabled: disabled\n")

        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file))
    }

    @Test
    fun aStaleLoadedConfigCannotOverwriteAnOptOutMadeWhileRunning() {
        val file = configFile("usage-reporting:\n  enabled: true\n")
        val loaded = loadLikeBukkit(file)
        // The operator turns reporting off by hand while the server runs.
        file.writeText("usage-reporting:\n  enabled: false\n")

        // Something later saves the loaded, stale config (e.g. /mf dpc on).
        loaded.set("dpc-api.enabled", true)
        UsageReportingSetting.keepOptOutFromDisk(loaded, file)
        loaded.save(file)

        assertEquals(false, UsageReportingSetting.readFromDisk(file))
    }

    @Test
    fun aFileBukkitCannotParseIsNeverOverwrittenAndReportingStaysOff() {
        val broken = "usage-reporting:\n  enabled: false\n\tfactions: [\n"
        val file = configFile(broken)
        // Bukkit loads it as empty, so every getter falls through to the bundled defaults.
        assertTrue(loadLikeBukkit(file).getBoolean(UsageReportingSetting.ENABLED_KEY))

        assertTrue(UsageReportingSetting.isUnparseable(file))
        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file))
        startupSave(file)
        assertEquals(broken, file.readText(), "the operator's file must be left as it is")
    }

    @Test
    fun aBlankEnabledValueIsAnOptOut() {
        val file = configFile("usage-reporting:\n  enabled:   # off for now\n  key: k\n")
        // Bukkit drops the key, so the bundled default would win.
        assertTrue(loadLikeBukkit(file).getBoolean(UsageReportingSetting.ENABLED_KEY))

        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file))
    }

    @Test
    fun anEnabledKeyOutsideTheBlockIsNotTheSwitch() {
        val file = configFile("other:\n  enabled:\nusage-reporting:\n  enabled: true\n")

        assertFalse(UsageReportingSetting.turnedOffOnDisk(file))
    }

    @Test
    fun aMissingFileIsNotAnOptOut() {
        val file = File(folder, "absent.yml")

        assertNull(UsageReportingSetting.readFromDisk(file))
        assertFalse(UsageReportingSetting.turnedOffOnDisk(file))
        assertFalse(UsageReportingSetting.isUnparseable(file))
    }
}
