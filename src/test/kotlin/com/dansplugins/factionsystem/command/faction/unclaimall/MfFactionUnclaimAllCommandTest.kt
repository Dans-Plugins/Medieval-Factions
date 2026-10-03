package com.dansplugins.factionsystem.command.faction.unclaimall

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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

/**
 * Covers the admin form of `/f unclaimall`, which `mf.force.unclaim` enables: naming a faction unclaims all of its land
 * without disbanding it and without that faction's role check. See
 * https://github.com/Dans-Plugins/Medieval-Factions/issues/1987 and
 * https://github.com/Dans-Plugins/Medieval-Factions/issues/1733.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionUnclaimAllCommandTest {
    private val testUtils = TestUtils()
    private val ownFactionId = MfFactionId.generate()
    private val mfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()))

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var language: Language
    private lateinit var ownFaction: MfFaction
    private lateinit var uut: MfFactionUnclaimAllCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        mockConfig()
        mockScheduler()
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        uut = MfFactionUnclaimAllCommand(plugin)
    }

    @Test
    fun testOnCommand_forceUnclaimAllRemovesANamedFactionsLandWithoutMembership() {
        // prepare
        val player = fixture.player
        val rivals = stubNamedFaction("Rivals")
        stubSender(player, forceUnclaim = true, member = false)
        `when`(language["CommandFactionUnclaimAllSuccess"]).thenReturn("Unclaimed all")

        // execute
        val result = uut.onCommand(player, fixture.command, "label", arrayOf("Rivals"))

        // verify
        assertTrue(result)
        verify(claimService).deleteAllClaims(rivals.id)
        verify(player).sendMessage("${ChatColor.GREEN}Unclaimed all")
    }

    @Test
    fun testOnCommand_forceUnclaimAllAcceptsAQuotedMultiWordName() {
        // prepare
        val player = fixture.player
        val rivals = stubNamedFaction("The Rivals")
        stubSender(player, forceUnclaim = true, member = false)

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("\"The", "Rivals\""))

        // verify
        verify(claimService).deleteAllClaims(rivals.id)
    }

    @Test
    fun testOnCommand_namingAFactionWithoutForcePermissionOnlyUnclaimsTheSendersOwnFaction() {
        // prepare — without mf.force.unclaim the argument is ignored, as before
        val player = fixture.player
        val rivals = stubNamedFaction("Rivals")
        stubSender(player, forceUnclaim = false, member = true)

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Rivals"))

        // verify
        verify(claimService).deleteAllClaims(ownFactionId)
        verify(claimService, never()).deleteAllClaims(rivals.id)
    }

    @Test
    fun testOnCommand_namingAFactionWithoutForcePermissionStillRequiresMembership() {
        // prepare
        val player = fixture.player
        val rivals = stubNamedFaction("Rivals")
        stubSender(player, forceUnclaim = false, member = false)
        `when`(language["CommandFactionUnclaimAllMustBeInAFaction"]).thenReturn("Must be in a faction")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Rivals"))

        // verify
        verify(claimService, never()).deleteAllClaims(rivals.id)
        verify(player).sendMessage("${ChatColor.RED}Must be in a faction")
    }

    @Test
    fun testOnCommand_forceUnclaimAllRefusesAnUnknownFactionInsteadOfFallingBackToTheSendersOwn() {
        // prepare — a typo must never unclaim the admin's own faction
        val player = fixture.player
        stubSender(player, forceUnclaim = true, member = true)
        `when`(language["CommandFactionUnclaimAllInvalidFaction", "Nobody"]).thenReturn("No such faction")

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("Nobody"))

        // verify
        verify(claimService, never()).deleteAllClaims(ownFactionId)
        verify(player).sendMessage("${ChatColor.RED}No such faction")
    }

    @Test
    fun testOnCommand_forcePermissionHolderWithNoArgumentsUnclaimsTheirOwnFaction() {
        // prepare
        val player = fixture.player
        stubSender(player, forceUnclaim = true, member = true)

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf())

        // verify
        verify(claimService).deleteAllClaims(ownFactionId)
    }

    // Helper functions

    private fun stubNamedFaction(name: String): MfFaction {
        val namedFaction = mock(MfFaction::class.java)
        val namedFactionId = MfFactionId.generate()
        `when`(namedFaction.id).thenReturn(namedFactionId)
        `when`(namedFaction.name).thenReturn(name)
        `when`(factionService.getFaction(name)).thenReturn(namedFaction)
        `when`(claimService.deleteAllClaims(namedFactionId)).thenReturn(Success(Unit))
        return namedFaction
    }

    /**
     * Stubs the sender's node permissions and, when [member] is true, membership of their own faction with a role that
     * grants the faction-level unclaim permission.
     */
    private fun stubSender(player: Player, forceUnclaim: Boolean, member: Boolean) {
        `when`(player.hasPermission("mf.unclaimall")).thenReturn(true)
        `when`(player.hasPermission("mf.force.unclaim")).thenReturn(forceUnclaim)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        if (member) {
            val role = mock(MfFactionRole::class.java)
            val unclaim = MfFactionPermission("UNCLAIM", "Unclaim", false)
            val factionPermissions = mock(MfFactionPermissions::class.java)
            `when`(factionService.getFaction(mfPlayer.id)).thenReturn(ownFaction)
            `when`(ownFaction.getRole(mfPlayer.id)).thenReturn(role)
            `when`(plugin.factionPermissions).thenReturn(factionPermissions)
            `when`(factionPermissions.unclaim).thenReturn(unclaim)
            `when`(role.hasPermission(ownFaction, unclaim)).thenReturn(true)
        }
    }

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)

        ownFaction = mock(MfFaction::class.java)
        `when`(ownFaction.id).thenReturn(ownFactionId)
        `when`(claimService.deleteAllClaims(ownFactionId)).thenReturn(Success(Unit))
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
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
