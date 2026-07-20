package com.dansplugins.factionsystem.command.faction.addmember

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import net.md_5.bungee.api.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionAddMemberCommandTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var language: Language
    private lateinit var config: FileConfiguration
    private lateinit var server: Server
    private lateinit var uut: MfFactionAddMemberCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        mockConfig()
        mockServer()
        uut = MfFactionAddMemberCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.force.addmember")).thenReturn(false)
        `when`(sender.hasPermission("mf.force.join")).thenReturn(false)
        `when`(language["CommandFactionAddMemberNoPermission"]).thenReturn("No permission")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}No permission")
    }

    @Test
    fun testOnCommand_senderNotAPlayer() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.force.addmember")).thenReturn(true)
        `when`(language["CommandFactionAddMemberNotAPlayer"]).thenReturn("Not a player")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Not a player")
    }

    @Test
    fun testOnCommand_noArgumentsProvided() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        `when`(player.hasPermission("mf.force.addmember")).thenReturn(true)
        `when`(language["CommandFactionAddMemberUsage"]).thenReturn("Usage")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.RED}Usage")
    }

    @Test
    fun testOnCommand_invalidTargetFaction() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        `when`(player.hasPermission("mf.force.addmember")).thenReturn(true)
        `when`(language["CommandFactionAddMemberInvalidTargetFaction"]).thenReturn("No such faction")

        val targetPlayer = mockOfflinePlayer("targetPlayerName")
        `when`(playerService.getPlayer(targetPlayer)).thenReturn(MfPlayer(testUtils.createPlayerId(), name = "targetPlayerName"))
        `when`(factionService.getFaction("NonExistentFaction")).thenReturn(null)

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("targetPlayerName", "NonExistentFaction"))

        // verify
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.RED}No such faction")
    }

    @Test
    fun testOnCommand_targetFactionFull_playerHasNoCurrentFaction() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        `when`(player.hasPermission("mf.force.addmember")).thenReturn(true)
        `when`(language["CommandFactionAddMemberTargetFactionFull"]).thenReturn("Faction full")
        `when`(config.getInt("factions.maxMembers")).thenReturn(1)

        val targetPlayerId = testUtils.createPlayerId()
        val targetPlayer = mockOfflinePlayer("targetPlayerName")
        `when`(playerService.getPlayer(targetPlayer)).thenReturn(MfPlayer(targetPlayerId, name = "targetPlayerName"))

        val targetFaction = mockFaction("TargetFaction", memberCount = 1)
        `when`(factionService.getFaction("TargetFaction")).thenReturn(targetFaction)
        `when`(factionService.getFaction(targetPlayerId)).thenReturn(null)

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("targetPlayerName", "TargetFaction"))

        // verify
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.RED}Faction full")
        verify(factionService, never()).save(anyFaction())
    }

    /**
     * Regression test for the target-faction-full check running after the player was already
     * removed from their current faction. If the full check is not performed before the removal,
     * this test fails because factionService.save is invoked to remove the player from currentFaction.
     */
    @Test
    fun testOnCommand_targetFactionFull_doesNotRemovePlayerFromCurrentFaction() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        `when`(player.hasPermission("mf.force.addmember")).thenReturn(true)
        `when`(language["CommandFactionAddMemberTargetFactionFull"]).thenReturn("Faction full")
        `when`(config.getInt("factions.maxMembers")).thenReturn(1)

        val targetPlayerId = testUtils.createPlayerId()
        val targetPlayer = mockOfflinePlayer("targetPlayerName")
        `when`(playerService.getPlayer(targetPlayer)).thenReturn(MfPlayer(targetPlayerId, name = "targetPlayerName"))

        val targetFaction = mockFaction("TargetFaction", memberCount = 1)
        `when`(factionService.getFaction("TargetFaction")).thenReturn(targetFaction)

        val currentFaction = mockFaction("CurrentFaction", memberCount = 1)
        `when`(factionService.getFaction(targetPlayerId)).thenReturn(currentFaction)

        // execute: force flag so the removal branch would otherwise be reached directly
        val result = uut.onCommand(player, command, "label", arrayOf("targetPlayerName", "TargetFaction", "-f"))

        // verify
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.RED}Faction full")
        verify(factionService, never()).save(anyFaction())
    }

    // Helper functions

    /**
     * Mockito's [ArgumentMatchers.any] returns null, which trips Kotlin's null-check on the
     * non-nullable [MfFaction] parameter of [MfFactionService.save] before the matcher is
     * registered, corrupting Mockito's matcher stack for subsequent tests. This generic
     * indirection avoids the compiler inserting that check.
     */
    private fun <T> anyFaction(): T {
        ArgumentMatchers.any<MfFaction>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    private fun mockOfflinePlayer(name: String): OfflinePlayer {
        val offlinePlayer = mock(OfflinePlayer::class.java)
        `when`(offlinePlayer.hasPlayedBefore()).thenReturn(true)
        `when`(server.getOfflinePlayer(name)).thenReturn(offlinePlayer)
        return offlinePlayer
    }

    private fun mockFaction(name: String, memberCount: Int): MfFaction {
        val faction = mock(MfFaction::class.java)
        `when`(faction.name).thenReturn(name)
        val members = (1..memberCount).map {
            MfFactionMember(testUtils.createPlayerId(), mock(MfFactionRole::class.java))
        }
        `when`(faction.members).thenReturn(members)
        val roles = mock(MfFactionRoles::class.java)
        `when`(roles.default).thenReturn(mock(MfFactionRole::class.java))
        `when`(faction.roles).thenReturn(roles)
        return faction
    }

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

    private fun mockConfig() {
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
    }

    private fun mockServer() {
        server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
    }
}
