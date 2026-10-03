package com.dansplugins.factionsystem.update

import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * An existing install's config.yml lacks update-check until the plugin writes
 * the defaults back. The one-argument getter the notifier uses must fall
 * through to the jar's bundled config.yml, which ships the check on.
 */
class MfUpdateCheckConfigTest {

    private fun bundledDefaults(): YamlConfiguration {
        val stream = javaClass.classLoader.getResourceAsStream("config.yml") ?: error("config.yml not on the classpath")
        return InputStreamReader(stream, Charsets.UTF_8).use { YamlConfiguration.loadConfiguration(it) }
    }

    @Test
    fun testBundledConfigShipsCheckOn() {
        assertTrue(bundledDefaults().getBoolean(MfUpdateCheck.CONFIG_KEY))
    }

    @Test
    fun testMissingKeyFallsThroughToBundledDefault() {
        val onDisk = YamlConfiguration.loadConfiguration(java.io.StringReader("language: en-US\n"))
        onDisk.setDefaults(bundledDefaults())
        assertTrue(onDisk.getBoolean(MfUpdateCheck.CONFIG_KEY))
    }

    @Test
    fun testOperatorOptOutIsRead() {
        val onDisk = YamlConfiguration.loadConfiguration(java.io.StringReader("update-check:\n  enabled: false\n"))
        onDisk.setDefaults(bundledDefaults())
        assertFalse(onDisk.getBoolean(MfUpdateCheck.CONFIG_KEY))
    }
}
