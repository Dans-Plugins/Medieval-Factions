package com.dansplugins.factionsystem.command.faction.kick

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginManager
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

/**
 * Covers how `mf.force.kick` is honoured: when a faction is named, the force permission replaces that faction's own
 * kick role check, matching `mf.force.flag`. See https://github.com/Dans-Plugins/Medieval-Factions/issues/1988.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionKickCommandTest {
    private val testUtils = TestUtils()
    private val rivalsId = MfFactionId.generate()
    private val senderMfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()))
    private val targetMfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()))
    private val leaderPlayerId = MfPlayerId(UUID.randomUUID().toString())
    private val targetRoleId = MfFactionRoleId("member-role")
    private val kickPermission = MfFactionPermission("KICK", "Kick", false)

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var server: Server
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var language: Language
    private lateinit var rivals: MfFaction
    private lateinit var uut: MfFactionKickCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        mockServer()
        mockServices()
        mockLanguageSystem()
        mockRivals()
        uut = MfFactionKickCommand(plugin)
    }

    @Test
    fun testOnCommand_forceKickFromANamedFactionSucceedsWithoutMembership() {
        // prepare — the sender is in no faction, so the old behaviour (role == null) refused this
        val player = fixture.player
        stubSender(player, forceKick = true)
        `when`(language["CommandFactionKickSuccess", "Target", "Rivals"]).thenReturn("Kicked")

        // execute
        val result = uut.onCommand(player, fixture.command, "label", arrayOf("Rivals", "Target"))

        // verify
        assertTrue(result)
        verify(factionService).save(anyFaction())
        verify(player).sendMessage("${ChatColor.GREEN}Kicked")
    }

    @Test
    fun testOnCommand_forceKickFromANamedFactionIgnoresTheSendersRoleInIt() {
        // prepare — the sender is a member of the named faction whose role lacks the kick permission
        val player = fixture.player
        stubSender(player, forceKick = true)
        stubSenderRoleInRivals(canKick = false)
        `when`(language["CommandFactionKickSuccess", "Target", "Rivals"]).thenReturn("Kicked")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Rivals", "Target"))

        // verify
        verify(factionService).save(anyFaction())
        verify(player).sendMessage("${ChatColor.GREEN}Kicked")
    }

    @Test
    fun testOnCommand_withoutForcePermissionTheFactionRoleCheckStillApplies() {
        // prepare — same sender and role as above, but no mf.force.kick
        val player = fixture.player
        stubSender(player, forceKick = false)
        stubSenderRoleInRivals(canKick = false)
        `when`(language["CommandFactionKickNoFactionPermission"]).thenReturn("No faction permission")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Rivals", "Target"))

        // verify
        verify(factionService, never()).save(anyFaction())
        verify(player).sendMessage("${ChatColor.RED}No faction permission")
    }

    @Test
    fun testOnCommand_forcePermissionHolderNotNamingAFactionStillNeedsTheRolePermission() {
        // prepare — `/f kick Target` acts on the sender's own faction, so its role check applies as before
        val player = fixture.player
        stubSender(player, forceKick = true)
        stubSenderRoleInRivals(canKick = false)
        `when`(language["CommandFactionKickNoFactionPermission"]).thenReturn("No faction permission")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Target"))

        // verify
        verify(factionService, never()).save(anyFaction())
        verify(player).sendMessage("${ChatColor.RED}No faction permission")
    }

    @Test
    fun testOnCommand_withoutForcePermissionANonMemberIsRefused() {
        // prepare
        val player = fixture.player
        stubSender(player, forceKick = false)
        `when`(language["CommandFactionKickMustBeInAFaction"]).thenReturn("Must be in a faction")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Rivals", "Target"))

        // verify
        verify(factionService, never()).save(anyFaction())
        verify(player).sendMessage("${ChatColor.RED}Must be in a faction")
    }

    // Helper functions

    /**
     * Mockito's [ArgumentMatchers.any] returns null, which trips Kotlin's null-check on the non-null parameter, so the
     * matcher is registered and an unchecked null handed back instead.
     */
    private fun <T> anyFaction(): T {
        ArgumentMatchers.any<MfFaction>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    private fun stubSender(player: Player, forceKick: Boolean) {
        `when`(player.hasPermission("mf.kick")).thenReturn(true)
        `when`(player.hasPermission("mf.force.kick")).thenReturn(forceKick)
        `when`(playerService.getPlayer(player)).thenReturn(senderMfPlayer)
    }

    private fun stubSenderRoleInRivals(canKick: Boolean) {
        val senderRole = mock(MfFactionRole::class.java)
        `when`(senderRole.hasPermission(rivals, kickPermission)).thenReturn(canKick)
        `when`(factionService.getFaction(senderMfPlayer.id)).thenReturn(rivals)
        `when`(rivals.getRole(senderMfPlayer.id)).thenReturn(senderRole)
    }

    /**
     * Stubs a faction "Rivals" holding the kick target and a leader who can set the target's role, so the
     * "no one can set their role" safeguard passes and only the sender's permission decides the outcome.
     */
    private fun mockRivals() {
        rivals = mock(MfFaction::class.java)
        `when`(rivals.id).thenReturn(rivalsId)
        `when`(rivals.name).thenReturn("Rivals")
        `when`(factionService.getFaction("Rivals")).thenReturn(rivals)

        val targetRole = mock(MfFactionRole::class.java)
        `when`(targetRole.id).thenReturn(targetRoleId)
        val leaderRole = mock(MfFactionRole::class.java)
        val setTargetRole = MfFactionPermission("SET_MEMBER_ROLE(member-role)", "Set member role", false)
        `when`(leaderRole.hasPermission(rivals, setTargetRole)).thenReturn(true)
        `when`(rivals.members).thenReturn(
            listOf(MfFactionMember(targetMfPlayer.id, targetRole), MfFactionMember(leaderPlayerId, leaderRole))
        )
        `when`(rivals.getRole(targetMfPlayer.id)).thenReturn(targetRole)
        `when`(rivals.getRole(leaderPlayerId)).thenReturn(leaderRole)

        val factionPermissions = mock(MfFactionPermissions::class.java)
        `when`(plugin.factionPermissions).thenReturn(factionPermissions)
        `when`(factionPermissions.kick).thenReturn(kickPermission)
        `when`(factionPermissions.setMemberRole(targetRoleId)).thenReturn(setTargetRole)

        val target = mock(OfflinePlayer::class.java)
        `when`(target.isOnline).thenReturn(true)
        `when`(target.name).thenReturn("Target")
        `when`(server.getOfflinePlayer("Target")).thenReturn(target)
        `when`(playerService.getPlayer(target)).thenReturn(targetMfPlayer)

        `when`(factionService.save(anyFaction())).thenReturn(Success(rivals))
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

    private fun mockServer() {
        server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        `when`(server.pluginManager).thenReturn(mock(PluginManager::class.java))

        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }
    }
}
