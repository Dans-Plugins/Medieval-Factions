package com.dansplugins.factionsystem.command.faction.declinevassalization

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
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

class MfFactionDeclineVassalizationCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.declinevassalization")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDeclineVassalizationNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDeclineVassalizationUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDeclineVassalizationNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDeclineVassalizationFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclineVassalizationMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.swearFealty)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclineVassalizationNoFactionPermission"]}")
                    return@Runnable
                }
                val target = factionService.getFaction(args.joinToString(" "))
                if (target == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclineVassalizationInvalidTarget"]}")
                    return@Runnable
                }
                val factionRelationshipService = plugin.services.factionRelationshipService
                val reverseRelationships = factionRelationshipService.getRelationships(target.id, faction.id)
                val vassalizationRequest = reverseRelationships.firstOrNull { it.type == VASSAL }
                if (vassalizationRequest == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclineVassalizationNoVassalizationRequest"]}")
                    return@Runnable
                }
                factionRelationshipService.delete(vassalizationRequest.id).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclineVassalizationFailedToDeleteRelationship"]}")
                    plugin.logger.log(SEVERE, "Failed to delete faction relationship: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionDeclineVassalizationSuccess", target.name]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        faction.sendMessage(
                            plugin.language["FactionVassalizationRequestDeclinedNotificationTitle", target.name],
                            plugin.language["FactionVassalizationRequestDeclinedNotificationBody", target.name]
                        )
                        target.sendMessage(
                            plugin.language["FactionVassalizationRequestRejectedNotificationTitle", faction.name],
                            plugin.language["FactionVassalizationRequestRejectedNotificationBody", faction.name]
                        )
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
        if (sender !is Player) return emptyList()
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(sender) ?: return emptyList()
        val factionService = plugin.services.factionService
        val faction = factionService.getFaction(mfPlayer.id) ?: return emptyList()
        val factionRelationshipService = plugin.services.factionRelationshipService
        // Find factions that have sent vassalization requests TO this faction
        val factionsWithRequests = factionService.factions.filter { otherFaction ->
            val relationships = factionRelationshipService.getRelationships(otherFaction.id, faction.id)
            relationships.any { it.type == VASSAL }
        }
        return when {
            args.isEmpty() -> factionsWithRequests.map(MfFaction::name)
            args.size == 1 ->
                factionsWithRequests
                    .filter { it.name.lowercase().startsWith(args[0].lowercase()) }
                    .map(MfFaction::name)
            else -> emptyList()
        }
    }
}
