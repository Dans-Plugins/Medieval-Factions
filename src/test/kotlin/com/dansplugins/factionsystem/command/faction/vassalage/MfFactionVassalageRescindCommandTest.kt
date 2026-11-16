package com.dansplugins.factionsystem.command.faction.vassalage

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.approval.MfApprovalRequest
import com.dansplugins.factionsystem.approval.MfApprovalRequestService
import com.dansplugins.factionsystem.approval.MfApprovalRequestType
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
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipId
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

/** Covers `/faction vassalage rescind` (#1639). */
class MfFactionVassalageRescindCommandTest {

    private val senderId = MfPlayerId(UUID.randomUUID().toString())
    private val vassalize = MfFactionPermission("VASSALIZE", "Vassalize", false)
    private val deleted = mutableListOf<MfFactionRelationshipId>()

    private lateinit var plugin: MedievalFactions
    private lateinit var player: Player
    private lateinit var factionService: MfFactionService
    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var approvalService: MfApprovalRequestService
    private lateinit var north: MfFaction
    private lateinit var south: MfFaction
    private val northId: MfFactionId get() = north.id
    private val southId: MfFactionId get() = south.id

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
        `when`(scheduler.runTask(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }
        val permissions = mock(MfFactionPermissions::class.java)
        `when`(plugin.factionPermissions).thenReturn(permissions)
        `when`(permissions.vassalize).thenReturn(vassalize)

        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        val playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
        relationshipService = mock(MfFactionRelationshipService::class.java)
        `when`(services.factionRelationshipService).thenReturn(relationshipService)
        approvalService = MfApprovalRequestService()
        `when`(services.approvalRequestService).thenReturn(approvalService)

        player = mock(Player::class.java)
        `when`(player.hasPermission("mf.vassalize")).thenReturn(true)
        `when`(playerService.getPlayer(player)).thenReturn(MfPlayer(senderId))

        north = faction("North")
        south = faction("South")
        val role = mock(MfFactionRole::class.java)
        `when`(role.hasPermission(north, vassalize)).thenReturn(true)
        `when`(north.getRole(senderId)).thenReturn(role)
        `when`(factionService.getFaction(senderId)).thenReturn(north)
    }

    @Test
    fun rescind_withdrawsAPendingOfferAndTellsBothFactions() {
        val offer = MfFactionRelationship(factionId = northId, targetId = southId, type = VASSAL)
        stubRelationships(northId, southId, listOf(offer))

        run("South")

        assertEquals(listOf(offer.id), deleted)
        verify(player).sendMessage("${GREEN}CommandFactionVassalageRescindSuccess|South")
        verify(north).sendMessage(
            "FactionVassalizationRequestRescindedNotificationTitle|South",
            "FactionVassalizationRequestRescindedNotificationBody|South"
        )
        verify(south).sendMessage(
            "FactionVassalizationRequestWithdrawnNotificationTitle|North",
            "FactionVassalizationRequestWithdrawnNotificationBody|North"
        )
    }

    @Test
    fun rescind_withNoPendingOfferSaysSo() {
        run("South")

        assertEquals(emptyList<MfFactionRelationshipId>(), deleted)
        verify(player).sendMessage("${RED}CommandFactionVassalageRescindNoPendingOffer|South")
        verify(south, never()).sendMessage(anyString(), anyString())
    }

    @Test
    fun rescind_doesNotEndAnAcceptedVassalage() {
        stubRelationships(northId, southId, listOf(MfFactionRelationship(factionId = northId, targetId = southId, type = VASSAL)))
        stubRelationships(southId, northId, listOf(MfFactionRelationship(factionId = southId, targetId = northId, type = LIEGE)))

        run("South")

        assertEquals(emptyList<MfFactionRelationshipId>(), deleted)
        verify(player).sendMessage("${RED}CommandFactionVassalageRescindAlreadyVassal|South")
    }

    @Test
    fun rescind_doesNotTouchAnOfferMadeTheOtherWay() {
        // South offered to make North its vassal; North has nothing of its own to rescind.
        stubRelationships(southId, northId, listOf(MfFactionRelationship(factionId = southId, targetId = northId, type = VASSAL)))

        run("South")

        assertEquals(emptyList<MfFactionRelationshipId>(), deleted)
        verify(player).sendMessage("${RED}CommandFactionVassalageRescindNoPendingOffer|South")
    }

    @Test
    fun rescind_withdrawsAnOfferAwaitingModeratorApprovalWithoutTellingTheTarget() {
        approvalService.addRequest(
            MfApprovalRequest(factionId = northId, targetId = southId, type = MfApprovalRequestType.VASSALIZE, requesterId = senderId)
        )
        val unrelated = approvalService.addRequest(
            MfApprovalRequest(factionId = northId, targetId = southId, type = MfApprovalRequestType.WAR, requesterId = senderId)
        )

        run("South")

        assertEquals(listOf(unrelated), approvalService.getAllRequests())
        verify(player).sendMessage("${GREEN}CommandFactionVassalageRescindSuccess|South")
        verify(south, never()).sendMessage(anyString(), anyString())
    }

    @Test
    fun rescind_needsTheVassalizeRolePermission() {
        `when`(north.getRole(senderId)).thenReturn(mock(MfFactionRole::class.java))
        stubRelationships(northId, southId, listOf(MfFactionRelationship(factionId = northId, targetId = southId, type = VASSAL)))

        run("South")

        assertEquals(emptyList<MfFactionRelationshipId>(), deleted)
        verify(player).sendMessage("${RED}CommandFactionVassalageRescindNoFactionPermission")
    }

    @Test
    fun rescind_needsTheVassalizePermissionNode() {
        `when`(player.hasPermission("mf.vassalize")).thenReturn(false)

        assertTrue(MfFactionVassalageRescindCommand(plugin).onCommand(player, mock(Command::class.java), "f", arrayOf("South")))

        verify(player).sendMessage("${RED}CommandFactionVassalageRescindNoPermission")
    }

    @Test
    fun rescind_withNoFactionNamedShowsTheUsage() {
        run()

        verify(player).sendMessage("${RED}CommandFactionVassalageRescindUsage")
    }

    @Test
    fun tabCompletion_listsOnlyFactionsWithAPendingOffer() {
        val east = faction("East")
        val eastId = east.id
        val offers = listOf(
            MfFactionRelationship(factionId = northId, targetId = southId, type = VASSAL),
            MfFactionRelationship(factionId = northId, targetId = eastId, type = VASSAL)
        )
        `when`(relationshipService.getRelationships(northId, VASSAL)).thenReturn(offers)
        // East accepted, so it is a vassal, not a pending offer.
        stubRelationships(eastId, northId, listOf(MfFactionRelationship(factionId = eastId, targetId = northId, type = LIEGE)))

        val completions = MfFactionVassalageRescindCommand(plugin).onTabComplete(player, mock(Command::class.java), "f", arrayOf(""))

        assertEquals(listOf("South"), completions)
    }

    private fun run(vararg args: String) {
        MfFactionVassalageRescindCommand(plugin).onCommand(player, mock(Command::class.java), "f", arrayOf(*args))
    }

    private fun stubRelationships(factionId: MfFactionId, targetId: MfFactionId, relationships: List<MfFactionRelationship>) {
        `when`(relationshipService.getRelationships(factionId, targetId)).thenReturn(relationships)
        relationships.forEach { relationship ->
            `when`(relationshipService.delete(relationship.id)).thenAnswer {
                deleted.add(relationship.id)
                Success(Unit)
            }
        }
    }

    private fun faction(name: String): MfFaction {
        val faction = mock(MfFaction::class.java)
        val id = MfFactionId.generate()
        `when`(faction.id).thenReturn(id)
        `when`(faction.name).thenReturn(name)
        `when`(factionService.getFaction(name)).thenReturn(faction)
        `when`(factionService.getFaction(id)).thenReturn(faction)
        return faction
    }
}
