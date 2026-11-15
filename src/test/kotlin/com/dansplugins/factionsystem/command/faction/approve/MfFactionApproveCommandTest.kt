package com.dansplugins.factionsystem.command.faction.approve

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.approval.MfApprovalRequest
import com.dansplugins.factionsystem.approval.MfApprovalRequestService
import com.dansplugins.factionsystem.approval.MfApprovalRequestType
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfTruceService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor.RED
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertNotNull
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

class MfFactionApproveCommandTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var approvalService: MfApprovalRequestService
    private lateinit var sender: CommandSender
    private lateinit var faction: MfFaction
    private lateinit var target: MfFaction
    private lateinit var uut: MfFactionApproveCommand

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
        val factionService = mock(MfFactionService::class.java)
        relationshipService = mock(MfFactionRelationshipService::class.java)
        approvalService = MfApprovalRequestService()
        `when`(plugin.services).thenReturn(services)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.factionRelationshipService).thenReturn(relationshipService)
        `when`(services.approvalRequestService).thenReturn(approvalService)

        val language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language["CommandFactionApproveWarTruceActive", "Attacker", "Defender", "60"]).thenReturn("truce refusal")

        faction = mockFaction("Attacker")
        target = mockFaction("Defender")
        `when`(factionService.getFaction(faction.id)).thenReturn(faction)
        `when`(factionService.getFaction(target.id)).thenReturn(target)

        val saved = MfFactionRelationship(factionId = faction.id, targetId = target.id, type = AT_WAR)
        doReturn(Success(saved)).`when`(relationshipService).save(anyRelationship())

        sender = mock(CommandSender::class.java)
        `when`(sender.hasPermission("mf.approve")).thenReturn(true)

        uut = MfFactionApproveCommand(plugin)
    }

    @Test
    fun `a war declaration cannot be approved while the two factions are under truce`() {
        val now = Instant.parse("2026-11-15T12:00:00Z")
        val truceService = MfTruceService({ 60L }, null, { now })
        truceService.startTruce(faction.id, target.id)
        `when`(plugin.truceService).thenReturn(truceService)
        val request = approvalService.addRequest(
            MfApprovalRequest(
                factionId = faction.id,
                targetId = target.id,
                type = MfApprovalRequestType.WAR,
                requesterId = MfPlayerId(UUID.randomUUID().toString()),
                reason = null
            )
        )

        uut.onCommand(sender, mock(Command::class.java), "approve", arrayOf(request.id.value))

        verify(sender).sendMessage("${RED}truce refusal")
        verify(relationshipService, never()).save(anyRelationship())
        assertNotNull(approvalService.getRequest(request.id))
    }
}
