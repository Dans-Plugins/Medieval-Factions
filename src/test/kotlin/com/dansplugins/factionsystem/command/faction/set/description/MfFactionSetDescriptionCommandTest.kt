package com.dansplugins.factionsystem.command.faction.set.description

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
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
import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
 * Covers `mf.force.desc` (#1898): `/f set description <faction> -- <text>` sets another faction's description, and
 * every description without the `--` form keeps meaning the sender's own faction's.
 */
class MfFactionSetDescriptionCommandTest {
    private val senderMfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()))
    private val changeDescriptionPermission = MfFactionPermission("CHANGE_DESCRIPTION", "Change description", false)

    private lateinit var plugin: MedievalFactions
    private lateinit var player: Player
    private lateinit var factionService: MfFactionService
    private lateinit var ownFaction: MfFaction
    private lateinit var rivals: MfFaction
    private lateinit var uut: MfFactionSetDescriptionCommand

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        val language = mock(Language::class.java) { invocation -> invocation.arguments.joinToString("|") }
        `when`(plugin.language).thenReturn(language)
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        val playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
        val factionPermissions = mock(MfFactionPermissions::class.java)
        `when`(plugin.factionPermissions).thenReturn(factionPermissions)
        `when`(factionPermissions.changeDescription).thenReturn(changeDescriptionPermission)

        player = TestUtils().createCommandTestFixture().player
        `when`(player.hasPermission("mf.desc")).thenReturn(true)
        `when`(playerService.getPlayer(player)).thenReturn(senderMfPlayer)

        ownFaction = faction("Own")
        val role = mock(MfFactionRole::class.java)
        `when`(role.hasPermission(ownFaction, changeDescriptionPermission)).thenReturn(true)
        `when`(ownFaction.getRole(senderMfPlayer.id)).thenReturn(role)
        `when`(factionService.getFaction(senderMfPlayer.id)).thenReturn(ownFaction)
        rivals = faction("Rivals")
        `when`(factionService.save(anyFaction())).thenReturn(Success(rivals))
        uut = MfFactionSetDescriptionCommand(plugin)
    }

    @Test
    fun forcedFormSetsTheNamedFactionsDescriptionWithoutItsRoleCheck() {
        `when`(player.hasPermission("mf.force.desc")).thenReturn(true)
        val updated = mock(MfFaction::class.java)
        `when`(rivals.copy(description = "Feared by all")).thenReturn(updated)

        uut.onCommand(player, mock(Command::class.java), "label", arrayOf("Rivals", "--", "Feared", "by", "all"))

        verify(factionService).save(updated)
        verify(rivals, never()).getRole(senderMfPlayer.id)
        verify(player).sendMessage("${ChatColor.GREEN}CommandFactionForceSetDescriptionSuccess|Rivals|Feared by all")
    }

    @Test
    fun forcedFormNamingAnUnknownFactionIsRefused() {
        `when`(player.hasPermission("mf.force.desc")).thenReturn(true)

        uut.onCommand(player, mock(Command::class.java), "label", arrayOf("\"No", "Such\"", "--", "text"))

        verify(factionService, never()).save(anyFaction())
        verify(player).sendMessage("${ChatColor.RED}CommandFactionForceInvalidFaction|No Such")
    }

    @Test
    fun aDescriptionStartingWithAFactionNameStillSetsTheSendersOwnDescription() {
        `when`(player.hasPermission("mf.force.desc")).thenReturn(true)
        val updated = mock(MfFaction::class.java)
        `when`(ownFaction.copy(description = "Rivals beware")).thenReturn(updated)

        uut.onCommand(player, mock(Command::class.java), "label", arrayOf("Rivals", "beware"))

        verify(factionService).save(updated)
    }

    @Test
    fun withoutTheForcePermissionTheSeparatorIsPartOfTheOwnDescription() {
        `when`(player.hasPermission("mf.force.desc")).thenReturn(false)
        val updated = mock(MfFaction::class.java)
        `when`(ownFaction.copy(description = "Rivals -- beware")).thenReturn(updated)

        uut.onCommand(player, mock(Command::class.java), "label", arrayOf("Rivals", "--", "beware"))

        verify(factionService).save(updated)
    }

    private fun faction(name: String): MfFaction {
        val faction = mock(MfFaction::class.java)
        `when`(faction.id).thenReturn(MfFactionId.generate())
        `when`(faction.name).thenReturn(name)
        `when`(factionService.getFaction(name)).thenReturn(faction)
        return faction
    }

    private fun <T> anyFaction(): T {
        ArgumentMatchers.any<MfFaction>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}
