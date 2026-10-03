package com.dansplugins.factionsystem.command.faction.admin

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
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
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

/**
 * Covers `/f admin setleader` on a faction whose Owner role has been deleted, which previously could not be repaired.
 * See https://github.com/Dans-Plugins/Medieval-Factions/issues/1797.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionAdminSetLeaderCommandTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var logger: Logger
    private lateinit var permissions: MfFactionPermissions
    private lateinit var targetMfPlayer: MfPlayer
    private lateinit var uut: MfFactionAdminSetLeaderCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        mockLanguageSystem()
        mockConfig()
        mockScheduler()
        logger = mock(Logger::class.java)
        `when`(plugin.logger).thenReturn(logger)
        val flags = MfFlags(plugin)
        `when`(plugin.flags).thenReturn(flags)
        permissions = MfFactionPermissions(plugin)
        `when`(plugin.factionPermissions).thenReturn(permissions)
        mockServices()
        `when`(fixture.sender.hasPermission("mf.admin.setleader")).thenReturn(true)
        `when`(fixture.sender.name).thenReturn("Admin")
        uut = MfFactionAdminSetLeaderCommand(plugin)
    }

    @Test
    fun testOnCommand_recreatesTheOwnerRoleWhenTheFactionHasNone() {
        // prepare — the faction's members have deleted both the Owner and the Officer role
        val factionId = MfFactionId.generate()
        val member = MfFactionRole(plugin, name = "Member")
        val faction = MfFaction(plugin, id = factionId, name = "Leaderless", roles = MfFactionRoles(member.id, listOf(member)))
        `when`(factionService.getFaction("Leaderless")).thenReturn(faction)

        // execute
        val result = uut.onCommand(fixture.sender, fixture.command, "label", arrayOf("Target", "Leaderless"))

        // verify — the saved faction gained an Owner role and the player was assigned to it
        assertTrue(result)
        val saved = savedFaction()
        val owner = saved.roles.singleOrNull { it.name == "Owner" }
        assertNotNull(owner)
        owner!!
        assertEquals(listOf(member.id, owner.id), saved.roles.map { it.id })
        assertEquals(member.id, saved.roles.defaultRoleId)
        assertEquals(owner.id, saved.getRole(targetMfPlayer.id)?.id)

        // ...with the template's faction-wide permissions...
        assertTrue(owner.hasPermission(saved, permissions.disband))
        assertTrue(owner.hasPermission(saved, permissions.createRole))
        assertTrue(owner.hasPermission(saved, permissions.setRolePermission(permissions.disband)))
        // ...re-scoped onto the roles the faction actually has, itself included...
        // (checked as explicit grants: several role-scoped permissions default to true, which would hide a missing one)
        listOf(member.id, owner.id).forEach { roleId ->
            listOf(
                permissions.viewRole(roleId),
                permissions.modifyRole(roleId),
                permissions.setMemberRole(roleId),
                permissions.deleteRole(roleId),
                permissions.setRolePermission(permissions.setMemberRole(roleId))
            ).forEach { permission ->
                assertEquals(true, owner.permissionsByName[permission.name], "${permission.name} is not granted")
            }
        }
        // ...and nothing left pointing at the template's own Member and Officer roles, which this faction lacks
        val savedRoleIds = saved.roles.map { it.id.value }
        val roleScoped = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        owner.permissionsByName.keys.flatMap { name -> roleScoped.findAll(name).map { it.value }.toList() }
            .forEach { roleId -> assertTrue(roleId in savedRoleIds, "permission refers to unknown role $roleId") }

        // the sender is told and the repair is logged
        verify(fixture.sender).sendMessage("${ChatColor.GREEN}CommandFactionAdminSetLeaderRecreatedOwnerRole")
        verify(logger).info(anyString())
    }

    @Test
    fun testOnCommand_reusesAnExistingOwnerRole() {
        // prepare — a faction with the default roles, whose Owner has left
        val factionId = MfFactionId.generate()
        val roles = MfFactionRoles.defaults(plugin, factionId)
        val owner = roles.single { it.name == "Owner" }
        val faction = MfFaction(plugin, id = factionId, name = "Intact", roles = roles)
        `when`(factionService.getFaction("Intact")).thenReturn(faction)

        // execute
        uut.onCommand(fixture.sender, fixture.command, "label", arrayOf("Target", "Intact"))

        // verify — no role is added and the player gets the existing Owner role
        val saved = savedFaction()
        assertEquals(roles, saved.roles)
        assertEquals(owner.id, saved.getRole(targetMfPlayer.id)?.id)
        verify(fixture.sender, never()).sendMessage("${ChatColor.GREEN}CommandFactionAdminSetLeaderRecreatedOwnerRole")
        verify(logger, never()).info(anyString())
    }

    @Test
    fun testOnCommand_treatsAnOwnerRoleOfAnyCaseAsTheOwnerRole() {
        // prepare — a second "Owner" next to an "owner" would make the case-insensitive MfFactionRoles.getRole(name) lookup ambiguous
        val factionId = MfFactionId.generate()
        val member = MfFactionRole(plugin, name = "Member")
        val lowerOwner = MfFactionRole(plugin, name = "owner")
        val faction = MfFaction(plugin, id = factionId, name = "Lower", roles = MfFactionRoles(member.id, listOf(member, lowerOwner)))
        `when`(factionService.getFaction("Lower")).thenReturn(faction)

        // execute
        uut.onCommand(fixture.sender, fixture.command, "label", arrayOf("Target", "Lower"))

        // verify
        val saved = savedFaction()
        assertEquals(2, saved.roles.size)
        assertEquals(lowerOwner.id, saved.getRole(targetMfPlayer.id)?.id)
        assertFalse(saved.roles.any { it.name == "Owner" })
    }

    @Test
    fun testOnCommand_promotesAnExistingMemberIntoTheOwnerRole() {
        // prepare — a leaderless faction at its member limit whose only member holds the Member role
        `when`(plugin.config.getInt("factions.maxMembers")).thenReturn(1)
        val factionId = MfFactionId.generate()
        val roles = MfFactionRoles.defaults(plugin, factionId)
        val owner = roles.single { it.name == "Owner" }
        val member = roles.single { it.name == "Member" }
        val faction = MfFaction(
            plugin,
            id = factionId,
            name = "Leaderless",
            roles = roles,
            members = listOf(MfFactionMember(targetMfPlayer.id, member))
        )
        `when`(factionService.getFaction("Leaderless")).thenReturn(faction)
        `when`(factionService.getFaction(targetMfPlayer.id)).thenReturn(faction)

        // execute
        uut.onCommand(fixture.sender, fixture.command, "label", arrayOf("Target", "Leaderless"))

        // verify — the member is moved into the Owner role in place: not added twice, not refused as "in a faction"
        // and not refused as "full"
        val saved = savedFaction()
        assertEquals(1, saved.members.size)
        assertEquals(owner.id, saved.getRole(targetMfPlayer.id)?.id)
        assertEquals(roles, saved.roles)
        verify(fixture.sender).sendMessage("${ChatColor.GREEN}CommandFactionAdminSetLeaderSuccess")
        verify(fixture.sender, never()).sendMessage("${ChatColor.RED}CommandFactionAdminSetLeaderTargetPlayerAlreadyInFaction")
        verify(fixture.sender, never()).sendMessage("${ChatColor.RED}CommandFactionAdminSetLeaderTargetFactionFull")
    }

    @Test
    fun testOnCommand_recreatesTheOwnerRoleForAnExistingMember() {
        // prepare — the faction from #1797: Owner and Officer deleted, the remaining player holds Member
        val factionId = MfFactionId.generate()
        val member = MfFactionRole(plugin, name = "Member")
        val faction = MfFaction(
            plugin,
            id = factionId,
            name = "Leaderless",
            roles = MfFactionRoles(member.id, listOf(member)),
            members = listOf(MfFactionMember(targetMfPlayer.id, member))
        )
        `when`(factionService.getFaction("Leaderless")).thenReturn(faction)
        `when`(factionService.getFaction(targetMfPlayer.id)).thenReturn(faction)

        // execute
        uut.onCommand(fixture.sender, fixture.command, "label", arrayOf("Target", "Leaderless"))

        // verify
        val saved = savedFaction()
        val owner = saved.roles.single { it.name == "Owner" }
        assertEquals(1, saved.members.size)
        assertEquals(owner.id, saved.getRole(targetMfPlayer.id)?.id)
        verify(fixture.sender).sendMessage("${ChatColor.GREEN}CommandFactionAdminSetLeaderRecreatedOwnerRole")
    }

    @Test
    fun testOnCommand_refusesAPlayerInADifferentFaction() {
        // prepare
        val other = MfFaction(plugin, name = "Other")
        val target = MfFaction(plugin, name = "Leaderless")
        `when`(factionService.getFaction("Leaderless")).thenReturn(target)
        `when`(factionService.getFaction(targetMfPlayer.id)).thenReturn(other)

        // execute
        uut.onCommand(fixture.sender, fixture.command, "label", arrayOf("Target", "Leaderless"))

        // verify
        verify(fixture.sender).sendMessage("${ChatColor.RED}CommandFactionAdminSetLeaderTargetPlayerAlreadyInFaction")
        val placeholder = dummyFaction()
        verify(factionService, never()).save(any(MfFaction::class.java) ?: placeholder)
    }

    @Test
    fun testOnCommand_reportsAMemberWhoIsAlreadyTheOwner() {
        // prepare
        val factionId = MfFactionId.generate()
        val roles = MfFactionRoles.defaults(plugin, factionId)
        val owner = roles.single { it.name == "Owner" }
        val faction = MfFaction(
            plugin,
            id = factionId,
            name = "Led",
            roles = roles,
            members = listOf(MfFactionMember(targetMfPlayer.id, owner))
        )
        `when`(factionService.getFaction("Led")).thenReturn(faction)
        `when`(factionService.getFaction(targetMfPlayer.id)).thenReturn(faction)

        // execute
        uut.onCommand(fixture.sender, fixture.command, "label", arrayOf("Target", "Led"))

        // verify
        verify(fixture.sender).sendMessage("${ChatColor.RED}CommandFactionAdminSetLeaderAlreadyLeader")
        val placeholder = dummyFaction()
        verify(factionService, never()).save(any(MfFaction::class.java) ?: placeholder)
    }

    // Helper functions

    private fun savedFaction(): MfFaction {
        // Kotlin rejects the null a Mockito matcher returns, so a placeholder is built before the matcher is recorded.
        val placeholder = dummyFaction()
        val captor = ArgumentCaptor.forClass(MfFaction::class.java)
        verify(factionService).save(captor.capture() ?: placeholder)
        return captor.value
    }

    private fun dummyFaction() = MfFaction(plugin, name = "dummy")

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        val placeholder = dummyFaction()
        `when`(factionService.save(any(MfFaction::class.java) ?: placeholder)).thenReturn(Success(mock(MfFaction::class.java)))

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        val targetPlayer = mock(OfflinePlayer::class.java)
        `when`(targetPlayer.hasPlayedBefore()).thenReturn(true)
        `when`(plugin.server.getOfflinePlayer("Target")).thenReturn(targetPlayer)
        targetMfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()), name = "Target")
        `when`(playerService.getPlayer(targetPlayer)).thenReturn(targetMfPlayer)
    }

    /** Returns each language key unformatted, so the permission registry can be built from the real plugin classes. */
    private fun mockLanguageSystem() {
        val language = mock(Language::class.java) { invocation ->
            if (invocation.method.returnType == String::class.java) invocation.arguments[0] else RETURNS_DEFAULTS.answer(invocation)
        }
        `when`(plugin.language).thenReturn(language)
    }

    private fun mockConfig() {
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }
    }
}
