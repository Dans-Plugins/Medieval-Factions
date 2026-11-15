package com.dansplugins.factionsystem.command.faction.invoke

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfTruceService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor.RED
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.UUID

class MfFactionInvokeCommandTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var player: Player
    private lateinit var faction: MfFaction
    private lateinit var ally: MfFaction
    private lateinit var enemy: MfFaction
    private lateinit var uut: MfFactionInvokeCommand

    private fun <T> anyRelationship(): T {
        ArgumentMatchers.any<MfFactionRelationship>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    private fun mockFaction(name: String): MfFaction {
        val mockFaction = mock(MfFaction::class.java)
        `when`(mockFaction.id).thenReturn(MfFactionId.generate())
        `when`(mockFaction.name).thenReturn(name)
        return mockFaction
    }

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        val server = mock(Server::class.java)
        val scheduler = mock(BukkitScheduler::class.java)
        `when`(plugin.server).thenReturn(server)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }

        val services = mock(Services::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val factionService = mock(MfFactionService::class.java)
        relationshipService = mock(MfFactionRelationshipService::class.java)
        `when`(plugin.services).thenReturn(services)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.factionRelationshipService).thenReturn(relationshipService)

        val language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language["CommandFactionInvokeAllyTruceActive", "Ally", "Enemy", "60"]).thenReturn("truce refusal")

        player = mock(Player::class.java)
        `when`(player.hasPermission("mf.invoke")).thenReturn(true)
        val mfPlayer = mock(MfPlayer::class.java)
        val playerId = MfPlayerId(UUID.randomUUID().toString())
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)

        faction = mockFaction("Own")
        ally = mockFaction("Ally")
        enemy = mockFaction("Enemy")
        `when`(factionService.getFaction(playerId)).thenReturn(faction)
        `when`(factionService.getFaction("Ally")).thenReturn(ally)
        `when`(factionService.getFaction("Enemy")).thenReturn(enemy)

        val role = mock(MfFactionRole::class.java)
        val invoke = MfFactionPermission("INVOKE", "Invoke", false)
        val factionPermissions = mock(MfFactionPermissions::class.java)
        `when`(plugin.factionPermissions).thenReturn(factionPermissions)
        `when`(factionPermissions.invoke).thenReturn(invoke)
        `when`(faction.getRole(playerId)).thenReturn(role)
        `when`(role.hasPermission(faction, invoke)).thenReturn(true)

        val flags = mock(MfFlags::class.java)

        @Suppress("UNCHECKED_CAST")
        val neutral = mock(MfFlag::class.java) as MfFlag<Boolean>
        val allyFlags = mock(MfFlagValues::class.java)
        `when`(plugin.flags).thenReturn(flags)
        `when`(flags.isNeutral).thenReturn(neutral)
        `when`(ally.flags).thenReturn(allyFlags)
        `when`(allyFlags[neutral]).thenReturn(false)

        `when`(relationshipService.getRelationships(faction.id, ally.id))
            .thenReturn(listOf(MfFactionRelationship(factionId = faction.id, targetId = ally.id, type = ALLY)))
        `when`(relationshipService.getRelationships(ally.id, faction.id))
            .thenReturn(listOf(MfFactionRelationship(factionId = ally.id, targetId = faction.id, type = ALLY)))
        `when`(relationshipService.getRelationships(faction.id, enemy.id))
            .thenReturn(listOf(MfFactionRelationship(factionId = faction.id, targetId = enemy.id, type = AT_WAR)))
        `when`(relationshipService.getRelationships(enemy.id, faction.id))
            .thenReturn(listOf(MfFactionRelationship(factionId = enemy.id, targetId = faction.id, type = AT_WAR)))

        val saved = MfFactionRelationship(factionId = ally.id, targetId = enemy.id, type = AT_WAR)
        doReturn(Success(saved)).`when`(relationshipService).save(anyRelationship())

        uut = MfFactionInvokeCommand(plugin)
    }

    @Test
    fun `an ally under truce with the enemy cannot be called into the war`() {
        val now = Instant.parse("2026-11-15T12:00:00Z")
        val truceService = MfTruceService({ 60L }, null, { now })
        truceService.startTruce(ally.id, enemy.id)
        `when`(plugin.truceService).thenReturn(truceService)

        uut.onCommand(player, mock(Command::class.java), "invoke", arrayOf("Ally", "Enemy"))

        verify(player).sendMessage("${RED}truce refusal")
        verify(relationshipService, never()).save(anyRelationship())
    }
}
