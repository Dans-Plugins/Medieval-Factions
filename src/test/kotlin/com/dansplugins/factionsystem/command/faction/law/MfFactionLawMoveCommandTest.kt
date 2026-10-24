package com.dansplugins.factionsystem.command.faction.law

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.law.MfLaw
import com.dansplugins.factionsystem.law.MfLawId
import com.dansplugins.factionsystem.law.MfLawService
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginManager
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

/**
 * Covers how `/f law move` validates the new number. See https://github.com/Dans-Plugins/Medieval-Factions/issues/2129.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionLawMoveCommandTest {
    private val testUtils = TestUtils()
    private val factionId = MfFactionId.generate()
    private val mfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()))
    private val moveLawPermission = MfFactionPermission("MOVE_LAW", "Move law", false)

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var lawService: MfLawService
    private lateinit var language: Language
    private lateinit var faction: MfFaction
    private lateinit var laws: List<MfLaw>
    private lateinit var uut: MfFactionLawMoveCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        mockServer()
        mockLanguageSystem()
        mockServices()
        uut = MfFactionLawMoveCommand(plugin)
    }

    @Test
    fun testOnCommand_nonNumericTargetIsRefused() {
        // prepare
        val player = stubPlayer()

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("2", "first"))

        // verify
        verify(lawService, never()).move(anyLaw(), anyInt())
        verify(player).sendMessage("${ChatColor.RED}Invalid number")
    }

    @Test
    fun testOnCommand_targetAboveTheLawCountIsRefused() {
        // prepare
        val player = stubPlayer()

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("1", "4"))

        // verify
        verify(lawService, never()).move(anyLaw(), anyInt())
        verify(player).sendMessage("${ChatColor.RED}Invalid number")
    }

    @Test
    fun testOnCommand_targetBelowOneIsRefused() {
        // prepare
        val player = stubPlayer()

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("3", "0"))

        // verify
        verify(lawService, never()).move(anyLaw(), anyInt())
        verify(player).sendMessage("${ChatColor.RED}Invalid number")
    }

    @Test
    fun testOnCommand_targetWithinRangeMovesTheLaw() {
        // prepare
        val player = stubPlayer()

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf("3", "1"))

        // verify
        verify(lawService).move(laws[2], 1)
        verify(player).sendMessage("${ChatColor.GREEN}Law moved")
    }

    @Test
    fun testOnCommand_anotherFactionsLawIsRefusedWithAKeyedMessage() {
        // prepare
        val player = stubPlayer()
        val otherLaw = MfLaw(MfLawId.generate(), 1, MfFactionId.generate(), "other", 1)
        `when`(lawService.getLaw(otherLaw.id)).thenReturn(otherLaw)

        // execute
        uut.onCommand(player, fixture.command, "label", arrayOf(otherLaw.id.value, "1"))

        // verify
        verify(lawService, never()).move(anyLaw(), anyInt())
        verify(player).sendMessage("${ChatColor.RED}Not your faction")
    }

    // Helper functions

    /**
     * Mockito's [ArgumentMatchers.any] returns null, which trips Kotlin's null-check on the non-null parameter, so the
     * matcher is registered and an unchecked null handed back instead.
     */
    private fun <T> anyLaw(): T {
        ArgumentMatchers.any<MfLaw>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    private fun stubPlayer(): Player {
        val player = fixture.player
        `when`(player.hasPermission("mf.law.move")).thenReturn(true)
        return player
    }

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        val playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
        `when`(playerService.getPlayer(fixture.player)).thenReturn(mfPlayer)

        faction = mock(MfFaction::class.java)
        `when`(faction.id).thenReturn(factionId)
        val role = mock(MfFactionRole::class.java)
        `when`(faction.getRole(mfPlayer.id)).thenReturn(role)
        `when`(role.hasPermission(faction, moveLawPermission)).thenReturn(true)
        val factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)

        val factionPermissions = mock(MfFactionPermissions::class.java)
        `when`(plugin.factionPermissions).thenReturn(factionPermissions)
        `when`(factionPermissions.moveLaw).thenReturn(moveLawPermission)

        laws = (1..3).map { MfLaw(MfLawId.generate(), 1, factionId, "law $it", it) }
        lawService = mock(MfLawService::class.java)
        `when`(services.lawService).thenReturn(lawService)
        `when`(lawService.getLaws(factionId)).thenReturn(laws)
        laws.forEach { law ->
            `when`(lawService.getLaw(factionId, law.number)).thenReturn(law)
            `when`(lawService.move(law, 1)).thenReturn(Success(Unit))
        }
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language["EscapeSequence"]).thenReturn("cancel")
        `when`(language["CommandFactionLawMoveInvalidNumber", "3"]).thenReturn("Invalid number")
        `when`(language["CommandFactionLawMoveNotYourFaction"]).thenReturn("Not your faction")
        `when`(language["CommandFactionLawMoveSuccess"]).thenReturn("Law moved")
    }

    private fun mockServer() {
        val server = mock(Server::class.java)
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
