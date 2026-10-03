package com.dansplugins.factionsystem.command.faction.addons

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.addon.MfAddonDetector
import com.dansplugins.factionsystem.lang.Language
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.PluginDescriptionFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class MfFactionAddonsCommandTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var sender: CommandSender
    private val command = mock(Command::class.java)
    private var enabled = mapOf<String, String>()

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        // Renders language["Key", a, b] as "Key|a|b".
        val language = mock(Language::class.java) { invocation ->
            invocation.arguments.flatMap { arg -> if (arg is Array<*>) arg.toList() else listOf(arg) }.joinToString("|")
        }
        `when`(plugin.language).thenReturn(language)
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("addons.suggestions")).thenReturn(true)
        `when`(plugin.description).thenReturn(PluginDescriptionFile("MedievalFactions", "7.0.1", "x.Main"))
        sender = mock(CommandSender::class.java)
        `when`(sender.hasPermission("mf.addons")).thenReturn(true)
        enabled = emptyMap()
    }

    private fun run(): List<String> {
        val uut = MfFactionAddonsCommand(plugin) { MfAddonDetector({ enabled[it] }) }
        assertTrue(uut.onCommand(sender, command, "mf", arrayOf()))
        val captor = ArgumentCaptor.forClass(String::class.java)
        verify(sender, atLeastOnce()).sendMessage(captor.capture())
        return captor.allValues
    }

    @Test
    fun `sender without permission sees only the refusal`() {
        `when`(sender.hasPermission("mf.addons")).thenReturn(false)
        assertEquals(listOf("${ChatColor.RED}CommandFactionAddonsNoPermission"), run())
    }

    @Test
    fun `lists every entry with link and verified version when nothing is installed`() {
        val lines = run()
        assertTrue(lines.contains("${ChatColor.GRAY}CommandFactionAddonsNotInstalledVerified|Currencies|7.0.0|3.0.0"), lines.toString())
        assertTrue(lines.contains("${ChatColor.GRAY}CommandFactionAddonsNotInstalled|Mailboxes"), lines.toString())
        assertTrue(lines.any { it.contains("https://dansplugins.com/resources/bluemap-medieval-factions") }, lines.toString())
        assertEquals(7, lines.count { it.contains("CommandFactionAddonsDetail") })
    }

    @Test
    fun `a matching version is reported as verified and a different one is not`() {
        enabled = mapOf("Currencies" to "3.0.0", "Democracy" to "0.2.0", "MedievalEconomy" to "1.4")
        val lines = run()
        assertTrue(lines.contains("${ChatColor.GREEN}CommandFactionAddonsInstalledVerified|Currencies|3.0.0|7.0.0"), lines.toString())
        assertTrue(lines.contains("${ChatColor.YELLOW}CommandFactionAddonsInstalledNotVerified|Democracy|0.2.0|7.0.0|0.3.0"), lines.toString())
        assertTrue(lines.contains("${ChatColor.WHITE}CommandFactionAddonsInstalledNoData|Medieval Economy|1.4"), lines.toString())
    }

    @Test
    fun `with suggestions off only installed entries are listed`() {
        `when`(config.getBoolean("addons.suggestions")).thenReturn(false)
        enabled = mapOf("Fiefs" to "0.12.1")
        val lines = run()
        assertEquals(1, lines.count { it.contains("CommandFactionAddonsDetail") }, lines.toString())
        assertTrue(lines.any { it.contains("Fiefs") })
        assertTrue(lines.none { it.contains("Currencies") })
        assertTrue(lines.contains("${ChatColor.GRAY}CommandFactionAddonsSuggestionsOff"))
    }
}
