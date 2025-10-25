package com.dansplugins.factionsystem.command.faction.admin

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import preponderous.ponder.command.dropFirst
import java.util.logging.Level.SEVERE

class MfFactionAdminSetLeaderCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.admin.setleader")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderNoPermission"]}")
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderUsage"]}")
            return true
        }
        
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                // Get target player
                val targetPlayer = plugin.server.getOfflinePlayer(args[0])
                if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderInvalidTargetPlayer"]}")
                    return@Runnable
                }
                
                val playerService = plugin.services.playerService
                val targetMfPlayer = playerService.getPlayer(targetPlayer)
                    ?: playerService.save(MfPlayer(plugin, targetPlayer)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                
                val factionService = plugin.services.factionService
                
                // Check if target player is already in a faction
                if (factionService.getFaction(targetMfPlayer.id) != null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderTargetPlayerAlreadyInFaction"]}")
                    return@Runnable
                }
                
                // Get target faction
                val targetFaction = factionService.getFaction(args.dropFirst().joinToString(" "))
                if (targetFaction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderInvalidTargetFaction"]}")
                    return@Runnable
                }
                
                val maxMembers = plugin.config.getInt("factions.maxMembers")
                if (maxMembers > 0 && targetFaction.members.size >= maxMembers) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderTargetFactionFull"]}")
                    return@Runnable
                }
                
                // Find the Owner role
                val ownerRole = targetFaction.roles.roles.find { it.name == "Owner" }
                if (ownerRole == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderNoOwnerRole"]}")
                    return@Runnable
                }
                
                // Add player as the owner
                val updatedFaction = factionService.save(
                    targetFaction.copy(
                        members = targetFaction.members + MfFactionMember(targetMfPlayer.id, ownerRole),
                        invites = targetFaction.invites.filter { it.playerId != targetMfPlayer.id }
                    )
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                
                val targetName = targetMfPlayer.name ?: plugin.language["CommandFactionAdminSetLeaderUnknownPlayer"]
                updatedFaction.sendMessage(
                    plugin.language["FactionNewLeaderNotificationTitle", targetName],
                    plugin.language["FactionNewLeaderNotificationBody", targetName]
                )
                sender.sendMessage(
                    "$GREEN${plugin.language["CommandFactionAdminSetLeaderSuccess", targetName, targetFaction.name]}"
                )
                
                try {
                    factionService.cancelAllApplicationsForPlayer(targetMfPlayer)
                } catch (e: Exception) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderFailedToCancelApplications"]}")
                    plugin.logger.log(SEVERE, "Failed to cancel applications: ${e.message}", e)
                }
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
        val factionService = plugin.services.factionService
        return when {
            args.isEmpty() ->
                plugin.server.offlinePlayers
                    .mapNotNull { it.name }
            args.size == 1 ->
                plugin.server.offlinePlayers
                    .filter { it.name?.lowercase()?.startsWith(args[0].lowercase()) == true }
                    .mapNotNull { it.name }
            args.size == 2 ->
                factionService.factions
                    .filter { it.name.lowercase().startsWith(args[1].lowercase()) }
                    .map(MfFaction::name)
            else -> emptyList()
        }
    }
}
