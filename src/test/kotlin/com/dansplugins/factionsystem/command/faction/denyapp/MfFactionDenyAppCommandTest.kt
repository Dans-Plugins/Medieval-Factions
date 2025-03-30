package com.dansplugins.factionsystem.command.faction.denyapp

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.command.faction.denyapp.tasks.DenyApplicationTask
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import net.md_5.bungee.api.ChatColor
import org.bukkit.Server
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.logging.Logger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionDenyAppCommandTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var language: Language
    private lateinit var uut: MfFactionDenyAppCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        mockScheduler()
        mockLogger()
        uut = MfFactionDenyAppCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.denyapp")).thenReturn(false)
        `when`(language["CommandFactionDenyAppNoPermission"]).thenReturn("No permission")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}No permission")
    }

    @Test
    fun testOnCommand_SenderNotAPlayer() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.denyapp")).thenReturn(true)
        `when`(language["CommandFactionDenyAppNotAPlayer"]).thenReturn("Not a player")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Not a player")
    }

    @Test
    fun testOnCommand_NoArgumentsProvided() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        `when`(player.hasPermission("mf.denyapp")).thenReturn(true)
        `when`(language["CommandFactionDenyAppUsage"]).thenReturn("Usage: /faction apply <faction>")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.RED}Usage: /faction apply <faction>")
    }

    @Test
    fun testOnCommand_InitiateDenyApplicationTask() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        `when`(player.hasPermission("mf.denyapp")).thenReturn(true)
        `when`(language["CommandFactionDenyAppUsage"]).thenReturn("Usage: /faction apply <faction>")
        `when`(player.name).thenReturn("playerName")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("targetPlayerName"))

        // verify
        assertTrue(result)
        verify(plugin.logger).info("Player playerName is denying application for player targetPlayerName")
        verify(plugin.server.scheduler).runTaskAsynchronously(
            eq(plugin),
            any(DenyApplicationTask::class.java)
        )
    }

    // Helper functions

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
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
}
