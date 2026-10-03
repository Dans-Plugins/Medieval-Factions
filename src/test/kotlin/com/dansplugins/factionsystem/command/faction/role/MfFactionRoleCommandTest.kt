package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

/**
 * Covers `mf.force.role`: `/f role <faction> <subcommand> ...` lets an admin manage another faction's roles, with the
 * force permission replacing that faction's own role-permission checks.
 * See https://github.com/Dans-Plugins/Medieval-Factions/issues/1797.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionRoleCommandTest {
    private val testUtils = TestUtils()

    private lateinit var plugin: MedievalFactions
    private lateinit var server: Server
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var permissions: MfFactionPermissions
    private lateinit var sender: Player
    private lateinit var command: Command
    private lateinit var senderMfPlayer: MfPlayer
    private lateinit var rivals: MfFaction
    private lateinit var home: MfFaction
    private lateinit var uut: MfFactionRoleCommand

    @BeforeEach
    fun setUp() {
        val fixture = testUtils.createCommandTestFixture()
        sender = fixture.player
        command = fixture.command
        plugin = mock(MedievalFactions::class.java)
        val logger = mock(Logger::class.java)
        `when`(plugin.logger).thenReturn(logger)
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        mockLanguageSystem()
        mockScheduler()
        val flags = MfFlags(plugin)
        `when`(plugin.flags).thenReturn(flags)
        permissions = MfFactionPermissions(plugin)
        `when`(plugin.factionPermissions).thenReturn(permissions)
        mockServices()
        mockFactions()
        listOf("view", "list", "setpermission", "set", "create", "delete", "rename", "setdefault").forEach {
            `when`(sender.hasPermission("mf.role.$it")).thenReturn(true)
        }
        uut = MfFactionRoleCommand(plugin)
    }

    // --- faction argument parsing ---

    @Test
    fun testOnCommand_forceSetPermissionOnANamedFactionWithoutMembership() {
        // prepare — the sender is in a different faction (Home) and has no role in Rivals
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)

        // execute
        uut.onCommand(sender, command, "role", arrayOf("Rivals", "setpermission", "Member", "INVITE", "deny"))

        // verify
        val saved = savedFaction()
        assertEquals(rivals.id, saved.id)
        assertEquals(false, saved.roles.single { it.name == "Member" }.permissionsByName[permissions.invite.name])
        verify(sender).sendMessage("${ChatColor.GREEN}CommandFactionRoleSetPermissionSuccess")
    }

    @Test
    fun testOnCommand_namedFactionWithoutForcePermissionShowsUsage() {
        // execute
        uut.onCommand(sender, command, "role", arrayOf("Rivals", "setpermission", "Member", "INVITE", "deny"))

        // verify
        verify(sender).sendMessage("${ChatColor.RED}CommandFactionRoleUsage")
        verifyNothingSaved()
    }

    @Test
    fun testOnCommand_unknownFactionIsRefusedWithoutFallingBackToOwnFaction() {
        // prepare — the sender's own faction would allow this, so a fallback would save Home
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)

        // execute
        uut.onCommand(sender, command, "role", arrayOf("Typo", "setpermission", "Member", "INVITE", "deny"))

        // verify
        verify(sender).sendMessage("${ChatColor.RED}CommandFactionRoleInvalidFaction")
        verifyNothingSaved()
    }

    @Test
    fun testOnCommand_factionCanBeGivenByIdOrAsAQuotedMultiWordName() {
        // prepare
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)
        val redRivals = MfFaction(plugin, name = "Red Rivals")
        stubFaction(redRivals)

        // execute
        uut.onCommand(sender, command, "role", arrayOf(rivals.id.value, "create", "Guard"))
        uut.onCommand(sender, command, "role", arrayOf("\"Red", "Rivals\"", "create", "Guard"))

        // verify
        val placeholder = dummyFaction()
        val captor = ArgumentCaptor.forClass(MfFaction::class.java)
        verify(factionService, atLeastOnce()).save(captor.capture() ?: placeholder)
        assertEquals(listOf(rivals.id, redRivals.id), captor.allValues.map { it.id })
        captor.allValues.forEach { faction -> assertTrue(faction.roles.any { it.name == "Guard" }) }
    }

    @Test
    fun testOnCommand_quotedFactionNamedLikeASubcommandIsAFaction() {
        // prepare
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)
        val listFaction = MfFaction(plugin, name = "list")
        stubFaction(listFaction)

        // execute
        uut.onCommand(sender, command, "role", arrayOf("\"list\"", "create", "Guard"))

        // verify
        assertEquals(listFaction.id, savedFaction().id)
    }

    @Test
    fun testOnCommand_withoutAFactionTheOwnRoleCheckStillApplies() {
        // prepare — the sender holds Member in Home, which cannot set role permissions; mf.force.role is not used
        // unless a faction is named
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)

        // execute
        uut.onCommand(sender, command, "role", arrayOf("setpermission", "Member", "INVITE", "deny"))

        // verify
        verify(sender).sendMessage("${ChatColor.RED}CommandFactionRoleSetPermissionNoFactionPermission")
        verifyNothingSaved()
    }

    // --- each subcommand with a named faction ---

    @Test
    fun testOnCommand_forceCreateGrantsAccessLikeTheOwnerRole() {
        // prepare
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)

        // execute
        uut.onCommand(sender, command, "role", arrayOf("Rivals", "create", "Guard"))

        // verify — the Owner role, which can modify every role, can modify the new one
        val saved = savedFaction()
        val guard = saved.roles.single { it.name == "Guard" }
        val owner = saved.roles.single { it.name == "Owner" }
        assertEquals(true, owner.permissionsByName[permissions.modifyRole(guard.id).name])
        assertEquals(true, owner.permissionsByName[permissions.deleteRole(guard.id).name])
    }

    @Test
    fun testOnCommand_forceDeleteRemovesTheRole() {
        // prepare
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)

        // execute
        uut.onCommand(sender, command, "role", arrayOf("Rivals", "delete", "Officer"))

        // verify
        assertFalse(savedFaction().roles.any { it.name == "Officer" })
    }

    @Test
    fun testOnCommand_forceRenameRenamesTheRole() {
        // prepare
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)

        // execute
        uut.onCommand(sender, command, "role", arrayOf("Rivals", "rename", "Officer", "Captain"))

        // verify
        val saved = savedFaction()
        assertNotNull(saved.roles.singleOrNull { it.name == "Captain" })
        assertFalse(saved.roles.any { it.name == "Officer" })
    }

    @Test
    fun testOnCommand_forceSetDefaultChangesTheDefaultRole() {
        // prepare
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)

        // execute
        uut.onCommand(sender, command, "role", arrayOf("Rivals", "setdefault", "Officer"))

        // verify
        val saved = savedFaction()
        assertEquals(saved.roles.single { it.name == "Officer" }.id, saved.roles.defaultRoleId)
    }

    @Test
    fun testOnCommand_forceSetPromotesAMemberOfTheNamedFaction() {
        // prepare
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)
        val rivalMember = stubPlayer("RivalMember")
        rivals = rivals.copy(members = listOf(MfFactionMember(rivalMember.id, rivals.roles.single { it.name == "Member" })))
        stubFaction(rivals)

        // execute
        uut.onCommand(sender, command, "role", arrayOf("Rivals", "set", "RivalMember", "Owner"))

        // verify
        val saved = savedFaction()
        assertEquals(saved.roles.single { it.name == "Owner" }.id, saved.getRole(rivalMember.id)?.id)
    }

    @Test
    fun testOnCommand_forceSetRefusesAPlayerOutsideTheNamedFaction() {
        // prepare — the target is in Home, not Rivals
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)
        val homeMember = stubPlayer("HomeMember")
        home = home.copy(members = home.members + MfFactionMember(homeMember.id, home.roles.single { it.name == "Member" }))
        stubFaction(home)

        // execute
        uut.onCommand(sender, command, "role", arrayOf("Rivals", "set", "HomeMember", "Owner"))

        // verify
        verify(sender).sendMessage("${ChatColor.RED}CommandFactionRoleSetTargetMustBeInFaction")
        verifyNothingSaved()
    }

    @Test
    fun testOnCommand_forceListButtonsKeepTheNamedFaction() {
        // prepare
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)
        val spigot = recordingSpigot()

        // execute
        uut.onCommand(sender, command, "role", arrayOf("Rivals", "list"))

        // verify — every role gets rename/delete buttons (the force permission replaces the role checks), and every
        // button targets Rivals by ID rather than the sender's own faction
        val commands = clickCommands(spigot)
        val officer = rivals.roles.single { it.name == "Officer" }
        assertTrue("/faction role ${rivals.id.value} rename ${officer.id.value} p=1" in commands, commands.toString())
        assertTrue("/faction role ${rivals.id.value} delete ${officer.id.value} p=1" in commands, commands.toString())
        assertTrue("/faction role ${rivals.id.value} create" in commands, commands.toString())
        assertTrue(commands.none { it.startsWith("/faction role ") && !it.startsWith("/faction role ${rivals.id.value} ") }, commands.toString())
    }

    @Test
    fun testOnCommand_forceViewButtonsKeepTheNamedFaction() {
        // prepare
        `when`(sender.hasPermission("mf.force.role")).thenReturn(true)
        val spigot = recordingSpigot()
        val member = rivals.roles.single { it.name == "Member" }

        // execute
        uut.onCommand(sender, command, "role", arrayOf("Rivals", "view", "Member"))

        // verify
        val commands = clickCommands(spigot)
        assertTrue(
            commands.any { it.startsWith("/faction role ${rivals.id.value} setpermission ${member.id.value} ") },
            commands.toString()
        )
        assertTrue(commands.none { it.startsWith("/faction role setpermission") }, commands.toString())
    }

    // Helper functions

    /** Records every component sent through `sender.spigot()`, however the varargs are passed. */
    private fun recordingSpigot(): MutableList<BaseComponent> {
        val sent = mutableListOf<BaseComponent>()
        val spigot = mock(Player.Spigot::class.java) { invocation ->
            invocation.arguments.forEach { argument ->
                when (argument) {
                    is BaseComponent -> sent += argument
                    is Array<*> -> sent += argument.filterIsInstance<BaseComponent>()
                }
            }
            null
        }
        `when`(sender.spigot()).thenReturn(spigot)
        return sent
    }

    private fun clickCommands(sent: List<BaseComponent>): List<String> = sent.mapNotNull { it.clickEvent?.value }

    private fun savedFaction(): MfFaction {
        val placeholder = dummyFaction()
        val captor = ArgumentCaptor.forClass(MfFaction::class.java)
        verify(factionService).save(captor.capture() ?: placeholder)
        return captor.value
    }

    private fun verifyNothingSaved() {
        val placeholder = dummyFaction()
        verify(factionService, never()).save(any(MfFaction::class.java) ?: placeholder)
    }

    private fun dummyFaction() = MfFaction(plugin, name = "dummy")

    private fun stubFaction(faction: MfFaction) {
        `when`(factionService.getFaction(faction.name)).thenReturn(faction)
        `when`(factionService.getFaction(faction.id)).thenReturn(faction)
        faction.members.forEach { `when`(factionService.getFaction(it.playerId)).thenReturn(faction) }
    }

    private fun stubPlayer(name: String): MfPlayer {
        val offlinePlayer = mock(OfflinePlayer::class.java)
        `when`(offlinePlayer.hasPlayedBefore()).thenReturn(true)
        `when`(offlinePlayer.name).thenReturn(name)
        `when`(server.getOfflinePlayer(name)).thenReturn(offlinePlayer)
        val mfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()), name = name)
        `when`(playerService.getPlayer(offlinePlayer)).thenReturn(mfPlayer)
        return mfPlayer
    }

    private fun mockFactions() {
        // Rivals: default roles, the sender is not a member
        val rivalsId = MfFactionId.generate()
        rivals = MfFaction(plugin, id = rivalsId, name = "Rivals", roles = MfFactionRoles.defaults(plugin, rivalsId))
        stubFaction(rivals)
        // Home: the sender's own faction, where the sender holds Member, which cannot manage roles
        val homeId = MfFactionId.generate()
        val homeRoles = MfFactionRoles.defaults(plugin, homeId)
        home = MfFaction(
            plugin,
            id = homeId,
            name = "Home",
            roles = homeRoles,
            members = listOf(MfFactionMember(senderMfPlayer.id, homeRoles.single { it.name == "Member" }))
        )
        stubFaction(home)
    }

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        val placeholder = dummyFaction()
        `when`(factionService.save(any(MfFaction::class.java) ?: placeholder)).thenAnswer { Success(it.arguments[0]) }

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
        senderMfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()), name = "Admin")
        `when`(playerService.getPlayer(sender)).thenReturn(senderMfPlayer)
    }

    /** Returns each language key unformatted, so the permission registry can be built from the real plugin classes. */
    private fun mockLanguageSystem() {
        val language = mock(Language::class.java) { invocation ->
            if (invocation.method.returnType == String::class.java) invocation.arguments[0] else RETURNS_DEFAULTS.answer(invocation)
        }
        `when`(plugin.language).thenReturn(language)
    }

    private fun mockScheduler() {
        server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }
        `when`(scheduler.runTask(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }
    }
}
