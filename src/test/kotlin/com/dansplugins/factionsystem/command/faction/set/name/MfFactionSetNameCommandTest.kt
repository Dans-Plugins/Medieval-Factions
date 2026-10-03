package com.dansplugins.factionsystem.command.faction.set.name

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
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
 * Covers how `mf.force.rename` is honoured: when a faction is named, the force permission replaces that faction's own
 * change-name role check, matching `mf.force.flag`. See https://github.com/Dans-Plugins/Medieval-Factions/issues/1988.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionSetNameCommandTest {
    private val testUtils = TestUtils()
    private val rivalsId = MfFactionId.generate()
    private val senderMfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()))
    private val changeNamePermission = MfFactionPermission("CHANGE_NAME", "Change name", false)

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var language: Language
    private lateinit var rivals: MfFaction
    private lateinit var adminCo: MfFaction
    private lateinit var uut: MfFactionSetNameCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        mockServer()
        mockServices()
        mockLanguageSystem()
        mockConfig()
        mockRivals()
        mockAdminCo()
        uut = MfFactionSetNameCommand(plugin)
    }

    @Test
    fun testOnCommand_forceRenameOfANamedFactionSucceedsWithoutMembership() {
        // prepare — the sender is in no faction, so the old behaviour (role == null) refused this
        val player = fixture.player
        stubSender(player, forceRename = true)
        `when`(language["CommandFactionSetNameSuccess", "NewName"]).thenReturn("Renamed")

        // execute
        val result = uut.onCommand(player, fixture.command, "label", arrayOf("Rivals", "NewName"))

        // verify
        assertTrue(result)
        verify(factionService).save(anyFaction())
        verify(player).sendMessage("${ChatColor.GREEN}Renamed")
    }

    @Test
    fun testOnCommand_forceRenameOfANamedFactionIgnoresTheSendersRoleInIt() {
        // prepare — the sender is a member of the named faction whose role lacks the change-name permission
        val player = fixture.player
        stubSender(player, forceRename = true)
        stubSenderRoleInRivals(canRename = false)
        `when`(language["CommandFactionSetNameSuccess", "NewName"]).thenReturn("Renamed")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Rivals", "NewName"))

        // verify
        verify(factionService).save(anyFaction())
        verify(player).sendMessage("${ChatColor.GREEN}Renamed")
    }

    @Test
    fun testOnCommand_withoutForcePermissionTheFactionRoleCheckStillApplies() {
        // prepare — same sender and role as above, but no mf.force.rename
        val player = fixture.player
        stubSender(player, forceRename = false)
        stubSenderRoleInRivals(canRename = false)
        `when`(language["CommandFactionSetNameNoFactionPermission"]).thenReturn("No faction permission")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Rivals", "NewName"))

        // verify
        verify(factionService, never()).save(anyFaction())
        verify(player).sendMessage("${ChatColor.RED}No faction permission")
    }

    @Test
    fun testOnCommand_forcePermissionHolderNotNamingAFactionStillNeedsTheRolePermission() {
        // prepare — `/f set name NewName` acts on the sender's own faction, so its role check applies as before
        val player = fixture.player
        stubSender(player, forceRename = true)
        stubSenderRoleInRivals(canRename = false)
        `when`(language["CommandFactionSetNameNoFactionPermission"]).thenReturn("No faction permission")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("NewName"))

        // verify
        verify(factionService, never()).save(anyFaction())
        verify(player).sendMessage("${ChatColor.RED}No faction permission")
    }

    // #2070: with mf.force.rename, two or more arguments always name a target faction, and an unknown one is refused
    // instead of falling back to renaming the sender's own faction to the whole argument string.

    @Test
    fun testOnCommand_forceRenameNamingAnUnknownFactionIsRefusedAndDoesNotRenameTheSendersOwnFaction() {
        // prepare — the exact repro from #2070: an op who leads AdminCo runs /f set name Xlnd NewName, no faction Xlnd
        val player = fixture.player
        stubSender(player, forceRename = true)
        stubSenderLeadsAdminCo()
        `when`(language["CommandFactionSetNameInvalidFaction", "Xlnd", "Xlnd NewName"]).thenReturn("No faction Xlnd")

        // execute
        val result = uut.onCommand(player, fixture.command, "label", arrayOf("Xlnd", "NewName"))

        // verify
        assertTrue(result)
        verify(factionService, never()).save(anyFaction())
        verify(adminCo, never()).getRole(senderMfPlayer.id)
        verify(player).sendMessage("${ChatColor.RED}No faction Xlnd")
    }

    @Test
    fun testOnCommand_forceRenameNamingAnUnknownQuotedFactionIsRefused() {
        // prepare
        val player = fixture.player
        stubSender(player, forceRename = true)
        stubSenderLeadsAdminCo()
        `when`(language["CommandFactionSetNameInvalidFaction", "No Such", "No Such NewName"]).thenReturn("No faction No Such")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("\"No", "Such\"", "NewName"))

        // verify
        verify(factionService, never()).save(anyFaction())
        verify(player).sendMessage("${ChatColor.RED}No faction No Such")
    }

    @Test
    fun testOnCommand_forceRenameCanNameTheTargetFactionByItsId() {
        // prepare
        val player = fixture.player
        stubSender(player, forceRename = true)
        stubSenderLeadsAdminCo()
        `when`(factionService.getFaction(rivalsId)).thenReturn(rivals)
        `when`(language["CommandFactionSetNameSuccess", "New Name"]).thenReturn("Renamed")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf(rivalsId.value, "New", "Name"))

        // verify — the named faction is renamed under the force permission; the sender's own is not consulted
        verify(factionService).save(anyFaction())
        verify(adminCo, never()).getRole(senderMfPlayer.id)
        verify(player).sendMessage("${ChatColor.GREEN}Renamed")
    }

    @Test
    fun testOnCommand_forcePermissionHolderRenamesTheirOwnFactionToAQuotedMultiWordName() {
        // prepare — /f set name "Xlnd NewName" is the force holder's way to give their own faction a name with spaces
        val player = fixture.player
        stubSender(player, forceRename = true)
        stubSenderLeadsAdminCo()
        `when`(language["CommandFactionSetNameSuccess", "Xlnd NewName"]).thenReturn("Renamed")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("\"Xlnd", "NewName\""))

        // verify — the own-faction path, with its role check, is taken
        verify(adminCo).getRole(senderMfPlayer.id)
        verify(factionService).save(anyFaction())
        verify(player).sendMessage("${ChatColor.GREEN}Renamed")
    }

    @Test
    fun testOnCommand_withoutForcePermissionAPlainMultiWordRenameOfTheOwnFactionStillWorks() {
        // prepare — the same words as the #2070 repro, from a member without mf.force.rename: the whole text is the
        // new name of their own faction, as before
        val player = fixture.player
        stubSender(player, forceRename = false)
        stubSenderLeadsAdminCo()
        `when`(language["CommandFactionSetNameSuccess", "Xlnd NewName"]).thenReturn("Renamed")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Xlnd", "NewName"))

        // verify
        verify(adminCo).getRole(senderMfPlayer.id)
        verify(factionService).save(anyFaction())
        verify(player).sendMessage("${ChatColor.GREEN}Renamed")
    }

    @Test
    fun testOnCommand_withoutForcePermissionAnExistingFactionNameIsPartOfTheNewNameNotATarget() {
        // prepare — "Rivals Reborn" from a member of AdminCo without mf.force.rename never touches Rivals
        val player = fixture.player
        stubSender(player, forceRename = false)
        stubSenderLeadsAdminCo()
        `when`(language["CommandFactionSetNameSuccess", "Rivals Reborn"]).thenReturn("Renamed")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Rivals", "Reborn"))

        // verify
        verify(adminCo).getRole(senderMfPlayer.id)
        verify(player).sendMessage("${ChatColor.GREEN}Renamed")
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

    private fun stubSender(player: Player, forceRename: Boolean) {
        `when`(player.hasPermission("mf.rename")).thenReturn(true)
        `when`(player.hasPermission("mf.force.rename")).thenReturn(forceRename)
        `when`(playerService.getPlayer(player)).thenReturn(senderMfPlayer)
    }

    private fun stubSenderRoleInRivals(canRename: Boolean) {
        val senderRole = mock(MfFactionRole::class.java)
        `when`(senderRole.hasPermission(rivals, changeNamePermission)).thenReturn(canRename)
        `when`(factionService.getFaction(senderMfPlayer.id)).thenReturn(rivals)
        `when`(rivals.getRole(senderMfPlayer.id)).thenReturn(senderRole)
    }

    private fun stubSenderLeadsAdminCo() {
        val senderRole = mock(MfFactionRole::class.java)
        `when`(senderRole.hasPermission(adminCo, changeNamePermission)).thenReturn(true)
        `when`(factionService.getFaction(senderMfPlayer.id)).thenReturn(adminCo)
        `when`(adminCo.getRole(senderMfPlayer.id)).thenReturn(senderRole)
    }

    private fun mockAdminCo() {
        adminCo = mock(MfFaction::class.java)
        `when`(adminCo.id).thenReturn(MfFactionId.generate())
        `when`(adminCo.name).thenReturn("AdminCo")
        `when`(factionService.getFaction("AdminCo")).thenReturn(adminCo)
    }

    private fun mockRivals() {
        rivals = mock(MfFaction::class.java)
        `when`(rivals.id).thenReturn(rivalsId)
        `when`(rivals.name).thenReturn("Rivals")
        `when`(factionService.getFaction("Rivals")).thenReturn(rivals)

        val factionPermissions = mock(MfFactionPermissions::class.java)
        `when`(plugin.factionPermissions).thenReturn(factionPermissions)
        `when`(factionPermissions.changeName).thenReturn(changeNamePermission)

        `when`(factionService.save(anyFaction())).thenReturn(Success(rivals))
    }

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        `when`(services.claimService).thenReturn(mock(MfClaimService::class.java))
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language["EscapeSequence"]).thenReturn("cancel")
    }

    private fun mockConfig() {
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getInt("factions.maxNameLength")).thenReturn(32)
    }

    private fun mockServer() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        `when`(server.onlinePlayers).thenReturn(emptyList())

        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }
    }
}
