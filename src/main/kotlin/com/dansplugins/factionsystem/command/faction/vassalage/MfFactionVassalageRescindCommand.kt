package com.dansplugins.factionsystem.command.faction.vassalage

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.approval.MfApprovalRequestType
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

/**
 * `/faction vassalage rescind [faction]` (#1639): withdraws a vassalization offer the sender's faction made and the
 * target has not yet accepted. A sent offer is a VASSAL relationship from the offering faction to the target with no
 * LIEGE relationship back; an offer still held for moderator approval is a pending VASSALIZE approval request. Both
 * are withdrawn. It needs the same permissions as making the offer: `mf.vassalize` and the VASSALIZE role permission.
 */
class MfFactionVassalageRescindCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.vassalize")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionVassalageRescindNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionVassalageRescindUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionVassalageRescindNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionVassalageRescindFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalageRescindMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.vassalize)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalageRescindNoFactionPermission"]}")
                    return@Runnable
                }
                val target = factionService.getFaction(args.joinToString(" "))
                if (target == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalageRescindInvalidTarget"]}")
                    return@Runnable
                }
                val factionRelationshipService = plugin.services.factionRelationshipService
                val offers = factionRelationshipService.getRelationships(faction.id, target.id).filter { it.type == VASSAL }
                if (offers.isNotEmpty() &&
                    factionRelationshipService.getRelationships(target.id, faction.id).any { it.type == LIEGE }
                ) {
                    // The offer was accepted: the target is already a vassal, which is ended with release instead.
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalageRescindAlreadyVassal", target.name]}")
                    return@Runnable
                }
                val approvalService = plugin.services.approvalRequestService
                val pendingApprovals = approvalService.getAllRequests().filter {
                    it.factionId == faction.id && it.targetId == target.id && it.type == MfApprovalRequestType.VASSALIZE
                }
                if (offers.isEmpty() && pendingApprovals.isEmpty()) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalageRescindNoPendingOffer", target.name]}")
                    return@Runnable
                }
                offers.forEach { offer ->
                    factionRelationshipService.delete(offer.id).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionVassalageRescindFailedToDeleteRelationship"]}")
                        plugin.logger.log(SEVERE, "Failed to delete faction relationship: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
                pendingApprovals.forEach { approvalService.removeRequest(it.id) }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionVassalageRescindSuccess", target.name]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        faction.sendMessage(
                            plugin.language["FactionVassalizationRequestRescindedNotificationTitle", target.name],
                            plugin.language["FactionVassalizationRequestRescindedNotificationBody", target.name]
                        )
                        // An offer still awaiting moderator approval was never sent to the target, so the target is
                        // only told when a sent offer is withdrawn.
                        if (offers.isNotEmpty()) {
                            target.sendMessage(
                                plugin.language["FactionVassalizationRequestWithdrawnNotificationTitle", faction.name],
                                plugin.language["FactionVassalizationRequestWithdrawnNotificationBody", faction.name]
                            )
                        }
                    }
                )
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player || args.size > 1) return emptyList()
        val mfPlayer = plugin.services.playerService.getPlayer(sender) ?: return emptyList()
        val factionService = plugin.services.factionService
        val faction = factionService.getFaction(mfPlayer.id) ?: return emptyList()
        val factionRelationshipService = plugin.services.factionRelationshipService
        // Factions this faction has sent an offer to that they have not accepted.
        val offered = factionRelationshipService.getRelationships(faction.id, VASSAL)
            .map { it.targetId }
            .filter { targetId -> factionRelationshipService.getRelationships(targetId, faction.id).none { it.type == LIEGE } }
            .mapNotNull { factionService.getFaction(it) }
            .map(MfFaction::name)
        val prefix = args.firstOrNull()?.lowercase() ?: ""
        return offered.filter { it.lowercase().startsWith(prefix) }
    }
}
