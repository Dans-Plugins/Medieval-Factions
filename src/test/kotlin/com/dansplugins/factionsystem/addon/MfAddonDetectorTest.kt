package com.dansplugins.factionsystem.addon

import com.dansplugins.factionsystem.lang.Language
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class MfAddonDetectorTest {

    private fun detectorWith(vararg enabled: Pair<String, String>): MfAddonDetector {
        val versions = mapOf(*enabled)
        return MfAddonDetector({ name -> versions[name] })
    }

    private fun recordingLanguage(): Language = mock(Language::class.java) { invocation ->
        // Renders language["Key", a, b] as "Key|a|b" so assertions can see both key and arguments.
        invocation.arguments.flatMap { arg -> if (arg is Array<*>) arg.toList() else listOf(arg) }.joinToString("|")
    }

    @Test
    fun `registry uses the exact plugin yml names`() {
        assertEquals(
            listOf("Currencies", "Fiefs", "Democracy", "MF_Bluemap", "MedievalRoleplayEngine", "Mailboxes", "MedievalEconomy"),
            MfKnownAddons.all.map { it.pluginName }
        )
    }

    @Test
    fun `every expansion has a verified version and no related plugin claims one`() {
        MfKnownAddons.all.forEach { addon ->
            when (addon.kind) {
                MfAddonKind.EXPANSION -> assertNotNull(addon.verifiedVersion, addon.pluginName)
                MfAddonKind.RELATED -> assertNull(addon.verifiedVersion, addon.pluginName)
            }
            assertTrue(addon.url.startsWith("https://dansplugins.com/resources/"), addon.url)
        }
        assertEquals(
            mapOf("Currencies" to "3.0.0", "Fiefs" to "0.12.1", "Democracy" to "0.3.0", "MF_Bluemap" to "1.0.0"),
            MfKnownAddons.all.filter { it.kind == MfAddonKind.EXPANSION }.associate { it.pluginName to it.verifiedVersion }
        )
    }

    @Test
    fun `nothing installed is detected as not installed`() {
        val detected = detectorWith().detect()
        assertEquals(MfKnownAddons.all.size, detected.size)
        assertTrue(detected.all { it.status == MfAddonStatus.NOT_INSTALLED })
        assertTrue(detectorWith().installed().isEmpty())
    }

    @Test
    fun `installed versions are detected by plugin name`() {
        val installed = detectorWith("Fiefs" to "0.12.1", "MF_Bluemap" to "1.0.0", "Unrelated" to "1.0").installed()
        assertEquals(listOf("Fiefs", "Bluemap_MedievalFactions"), installed.map { it.addon.displayName })
    }

    @Test
    fun `status is verified only when the installed version equals the verified one`() {
        val byName = detectorWith(
            "Currencies" to "3.0.0",
            "Fiefs" to "v0.12.1",
            "Democracy" to "0.2.0",
            "Mailboxes" to "1.2.0"
        ).detect().associateBy { it.addon.pluginName }
        assertEquals(MfAddonStatus.INSTALLED_VERIFIED, byName.getValue("Currencies").status)
        assertEquals(MfAddonStatus.INSTALLED_VERIFIED, byName.getValue("Fiefs").status)
        assertEquals(MfAddonStatus.INSTALLED_NOT_VERIFIED, byName.getValue("Democracy").status)
        assertEquals(MfAddonStatus.INSTALLED_NO_DATA, byName.getValue("Mailboxes").status)
        assertEquals(MfAddonStatus.NOT_INSTALLED, byName.getValue("MF_Bluemap").status)
    }

    @Test
    fun `a newer or snapshot version is not claimed as verified`() {
        val currencies = detectorWith("Currencies" to "3.0.1-SNAPSHOT").detect().first { it.addon.pluginName == "Currencies" }
        assertEquals(MfAddonStatus.INSTALLED_NOT_VERIFIED, currencies.status)
    }

    @Test
    fun `chart data is none when nothing is installed`() {
        assertEquals(mapOf("none" to 1), detectorWith().installedAddonsChartData())
    }

    @Test
    fun `chart data has one key per installed entry with value 1`() {
        assertEquals(
            mapOf("Currencies" to 1, "Medieval Roleplay Engine" to 1),
            detectorWith("Currencies" to "2.1.0", "MedievalRoleplayEngine" to "2.0.0").installedAddonsChartData()
        )
    }

    @Test
    fun `plugin manager lookup ignores disabled plugins`() {
        val pluginManager = mock(PluginManager::class.java)
        val enabled = mock(Plugin::class.java)
        `when`(enabled.isEnabled).thenReturn(true)
        `when`(enabled.description).thenReturn(PluginDescriptionFile("Currencies", "3.0.0", "x.Main"))
        val disabled = mock(Plugin::class.java)
        `when`(disabled.isEnabled).thenReturn(false)
        `when`(disabled.description).thenReturn(PluginDescriptionFile("Fiefs", "0.12.1", "x.Main"))
        `when`(pluginManager.getPlugin("Currencies")).thenReturn(enabled)
        `when`(pluginManager.getPlugin("Fiefs")).thenReturn(disabled)

        val installed = MfAddonDetector.forPluginManager(pluginManager).installed()

        assertEquals(listOf("Currencies" to "3.0.0"), installed.map { it.addon.pluginName to it.installedVersion })
    }

    @Test
    fun `startup line points at the command when nothing is installed`() {
        assertEquals("AddonsStartupNone", startupAddonsLine(detectorWith().detect(), recordingLanguage()))
    }

    @Test
    fun `startup line names the detected add-ons with their versions`() {
        val line = startupAddonsLine(detectorWith("Democracy" to "0.3.0", "Mailboxes" to "v1.2").detect(), recordingLanguage())
        assertEquals("AddonsStartupDetected|Democracy v0.3.0, Mailboxes v1.2", line)
        assertFalse(line.contains("Currencies"))
    }
}
