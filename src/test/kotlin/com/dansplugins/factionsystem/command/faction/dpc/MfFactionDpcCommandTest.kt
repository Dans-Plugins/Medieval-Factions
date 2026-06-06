package com.dansplugins.factionsystem.command.faction.dpc

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.service.Services
import net.md_5.bungee.api.ChatColor
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.logging.Logger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionDpcCommandTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var language: Language
    private lateinit var config: FileConfiguration
    private lateinit var uut: MfFactionDpcCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        mockLanguageSystem()
        mockScheduler()
        mockLogger()
        mockConfig()
        mockServices()
        uut = MfFactionDpcCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(false)
        `when`(language["CommandFactionDpcNoPermission"]).thenReturn("No permission")

        val result = uut.onCommand(sender, command, "label", arrayOf())

        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}No permission")
    }

    @Test
    fun testOnCommand_noArguments() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcUsage"]).thenReturn("Usage message")

        val result = uut.onCommand(sender, command, "label", arrayOf())

        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Usage message")
    }

    @Test
    fun testOnCommand_optIn() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcOptInSuccess"]).thenReturn("Opted in")

        val result = uut.onCommand(sender, command, "label", arrayOf("optin"))

        assertTrue(result)
        verify(config).set("dpc-api.enabled", true)
        verify(plugin).saveConfig()
        verify(sender).sendMessage("${ChatColor.GREEN}Opted in")
    }

    @Test
    fun testOnCommand_optOut() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcOptOutSuccess"]).thenReturn("Opted out")

        val result = uut.onCommand(sender, command, "label", arrayOf("optout"))

        assertTrue(result)
        verify(config).set("dpc-api.enabled", false)
        verify(plugin).saveConfig()
        verify(sender).sendMessage("${ChatColor.GREEN}Opted out")
    }

    @Test
    fun testOnCommand_reminderOn() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcReminderOnSuccess"]).thenReturn("Reminder on")

        val result = uut.onCommand(sender, command, "label", arrayOf("reminder", "on"))

        assertTrue(result)
        verify(config).set("dpc-api.login-reminder", true)
        verify(plugin).saveConfig()
        verify(sender).sendMessage("${ChatColor.GREEN}Reminder on")
    }

    @Test
    fun testOnCommand_reminderOff() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcReminderOffSuccess"]).thenReturn("Reminder off")

        val result = uut.onCommand(sender, command, "label", arrayOf("reminder", "off"))

        assertTrue(result)
        verify(config).set("dpc-api.login-reminder", false)
        verify(plugin).saveConfig()
        verify(sender).sendMessage("${ChatColor.GREEN}Reminder off")
    }

    @Test
    fun testOnCommand_shareIpOn() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcShareIpOnSuccess"]).thenReturn("Share IP on")

        val result = uut.onCommand(sender, command, "label", arrayOf("shareip", "on"))

        assertTrue(result)
        verify(config).set("dpc-api.share-server-ip", true)
        verify(plugin).saveConfig()
        verify(sender).sendMessage("${ChatColor.GREEN}Share IP on")
    }

    @Test
    fun testOnCommand_shareIpOff() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcShareIpOffSuccess"]).thenReturn("Share IP off")

        val result = uut.onCommand(sender, command, "label", arrayOf("shareip", "off"))

        assertTrue(result)
        verify(config).set("dpc-api.share-server-ip", false)
        verify(plugin).saveConfig()
        verify(sender).sendMessage("${ChatColor.GREEN}Share IP off")
    }

    @Test
    fun testOnCommand_discordSetLink() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcDiscordSetSuccess", "https://discord.gg/test"]).thenReturn("Discord set")

        val result = uut.onCommand(sender, command, "label", arrayOf("discord", "https://discord.gg/test"))

        assertTrue(result)
        verify(config).set("dpc-api.discord-link", "https://discord.gg/test")
        verify(plugin).saveConfig()
        verify(sender).sendMessage("${ChatColor.GREEN}Discord set")
    }

    @Test
    fun testOnCommand_discordClear() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcDiscordClearSuccess"]).thenReturn("Discord cleared")

        val result = uut.onCommand(sender, command, "label", arrayOf("discord", "clear"))

        assertTrue(result)
        verify(config).set("dpc-api.discord-link", "")
        verify(plugin).saveConfig()
        verify(sender).sendMessage("${ChatColor.GREEN}Discord cleared")
    }

    @Test
    fun testOnCommand_discordInvalidLinkRejected() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcDiscordInvalidLink"]).thenReturn("Invalid link")

        val result = uut.onCommand(sender, command, "label", arrayOf("discord", "https://example.com/not-discord"))

        assertTrue(result)
        verify(config, never()).set(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())
        verify(plugin, never()).saveConfig()
        verify(sender).sendMessage("${ChatColor.RED}Invalid link")
    }

    @Test
    fun testOnCommand_discordComLinkAccepted() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcDiscordSetSuccess", "https://discord.com/invite/test"]).thenReturn("Discord set")

        val result = uut.onCommand(sender, command, "label", arrayOf("discord", "https://discord.com/invite/test"))

        assertTrue(result)
        verify(config).set("dpc-api.discord-link", "https://discord.com/invite/test")
        verify(plugin).saveConfig()
        verify(sender).sendMessage("${ChatColor.GREEN}Discord set")
    }

    @Test
    fun testOnCommand_reminderNoSubarg() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcReminderUsage"]).thenReturn("Reminder usage")

        val result = uut.onCommand(sender, command, "label", arrayOf("reminder"))

        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Reminder usage")
    }

    @Test
    fun testOnCommand_unknownSubcommand() {
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.dpc")).thenReturn(true)
        `when`(language["CommandFactionDpcUsage"]).thenReturn("Usage message")

        val result = uut.onCommand(sender, command, "label", arrayOf("invalid"))

        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Usage message")
    }

    // Helper functions

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
    }

    private fun mockLogger() {
        val logger = mock(Logger::class.java)
        `when`(plugin.logger).thenReturn(logger)
    }

    private fun mockConfig() {
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
    }
}
