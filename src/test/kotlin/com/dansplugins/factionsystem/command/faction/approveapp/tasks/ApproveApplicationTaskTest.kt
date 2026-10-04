package com.dansplugins.factionsystem.command.faction.approveapp.tasks

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionApplication
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

class ApproveApplicationTaskTest {

    /**
     * Approving an application from a player who is already in the approving faction first
     * removes them from it, which saves the faction and bumps its version. The approval must then
     * start from the faction as stored after that removal, not the copy read before it, or the
     * save is a stale write: an optimistic-lock conflict on H2, and on MariaDB (before #2076) a
     * write dropped without error.
     */
    @Test
    fun `an applicant already in the faction is added to the faction as stored after the removal`() {
        val plugin = mock(MedievalFactions::class.java)
        val services = mock(Services::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val server = mock(Server::class.java)
        val permissions = mock(MfFactionPermissions::class.java)
        `when`(plugin.services).thenReturn(services)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.playerService).thenReturn(playerService)
        `when`(plugin.server).thenReturn(server)
        `when`(plugin.language).thenReturn(mock(Language::class.java))
        `when`(plugin.config).thenReturn(mock(FileConfiguration::class.java))
        `when`(plugin.logger).thenReturn(Logger.getLogger("ApproveApplicationTaskTest"))
        `when`(plugin.factionPermissions).thenReturn(permissions)
        `when`(permissions.approveApp).thenReturn(MfFactionPermission("APPROVE_APP", "Approve applications", true))

        val sender = mock(Player::class.java)
        val approver = MfPlayer(MfPlayerId(UUID.randomUUID().toString()), name = "Approver")
        `when`(playerService.getPlayer(sender)).thenReturn(approver)
        val target = mock(OfflinePlayer::class.java)
        `when`(target.isOnline).thenReturn(true)
        `when`(server.getOfflinePlayer("Applicant")).thenReturn(target)
        val applicant = MfPlayer(MfPlayerId(UUID.randomUUID().toString()), name = "Applicant")
        `when`(playerService.getPlayer(target)).thenReturn(applicant)

        val memberRole = MfFactionRole(plugin, name = "Member")
        val officerRole = MfFactionRole(plugin, name = "Officer")
        val faction = MfFaction(
            plugin = plugin,
            version = 7,
            name = "Approvers",
            members = listOf(MfFactionMember(approver.id, officerRole), MfFactionMember(applicant.id, officerRole)),
            flags = MfFlagValues(plugin, emptyMap()),
            roles = MfFactionRoles(memberRole.id, listOf(memberRole, officerRole)),
            defaultPermissionsByName = emptyMap()
        )
        val factionWithApplication = faction.copy(applications = listOf(MfFactionApplication(faction.id, applicant.id)))
        val storedAfterRemoval = factionWithApplication.copy(version = 8, members = listOf(MfFactionMember(approver.id, officerRole)))
        `when`(factionService.getFaction(approver.id)).thenReturn(factionWithApplication)
        `when`(factionService.getFaction(applicant.id)).thenReturn(factionWithApplication)
        `when`(factionService.getFaction(faction.id)).thenReturn(storedAfterRemoval)
        val saved = mutableListOf<MfFaction>()
        `when`(factionService.save(anyFaction())).thenAnswer { invocation ->
            saved += invocation.getArgument<MfFaction>(0)
            Success(mock(MfFaction::class.java))
        }

        ApproveApplicationTask(plugin, sender, "Applicant").run()

        assertEquals(2, saved.size)
        assertEquals(7, saved[0].version, "the removal saves the faction as read")
        val approval = saved[1]
        assertEquals(8, approval.version, "the approval must be saved from the faction as stored after the removal")
        assertEquals(
            listOf(MfFactionMember(approver.id, officerRole), MfFactionMember(applicant.id, memberRole)),
            approval.members
        )
        assertEquals(emptyList<MfFactionApplication>(), approval.applications)
    }

    /** See MfFactionAddMemberCommandTest: avoids Kotlin's null check on Mockito's matcher result. */
    private fun <T> anyFaction(): T {
        ArgumentMatchers.any<MfFaction>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}
