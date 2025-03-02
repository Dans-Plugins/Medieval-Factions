package com.dansplugins.factionsystem.command.faction.addmember

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import preponderous.ponder.command.dropFirst
import java.util.logging.Level
import java.util.logging.Level.SEVERE

class MfFactionAddMemberCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.force.addmember") && !sender.hasPermission("mf.force.join")) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberUsage"]}")
            return true
        }

        // get target player
        val targetPlayer = plugin.server.getOfflinePlayer(args[0])
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberInvalidTargetPlayer"]}")
            return true
        }
        val playerService = plugin.services.playerService
        val targetMfPlayer = playerService.getPlayer(targetPlayer)
            ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberFailedToSavePlayer"]}")
                plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return true
            }

        val factionService = plugin.services.factionService

        // if target player is already in a faction, cancel
        if (factionService.getFaction(targetMfPlayer.id) != null) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberTargetPlayerAlreadyInFaction"]}")
            return true
            // question: should we remove the player from their current faction in this case instead of returning?
        }

        // get target faction
        val targetFaction = factionService.getFaction(args.dropFirst().joinToString(" "))
        if (targetFaction == null) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberInvalidTargetFaction"]}")
            return true
        }

        val maxMembers = plugin.config.getInt("factions.maxMembers")
        if (maxMembers > 0 && targetFaction.members.size >= maxMembers) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberTargetFactionFull"]}")
            return true
        }

        // add member to faction
        val updatedFaction = factionService.save(
            targetFaction.copy(
                members = targetFaction.members + MfFactionMember(targetMfPlayer.id, targetFaction.roles.default),
                invites = targetFaction.invites.filter { it.playerId != targetMfPlayer.id }
            )
        ).onFailure {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberFailedToSaveFaction"]}")
            plugin.logger.log(Level.SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
            return true
        }
        var targetName = targetMfPlayer.name
        if (targetName == null) {
            targetName = "${ChatColor.RED}${plugin.language["CommandFactionAddMemberUnknownNewPlayerFaction"]}"
        }
        updatedFaction.sendMessage(
            plugin.language["FactionNewMemberNotificationTitle", targetName],
            plugin.language["FactionNewMemberNotificationBody", targetName]
        )
        sender.sendMessage(
            "${ChatColor.GREEN}${plugin.language["CommandFactionAddMemberSuccess", targetFaction.name]}"
        )
        try {
            factionService.cancelAllApplicationsForPlayer(targetMfPlayer)
        } catch (e: Exception) {
            sender.sendMessage("${org.bukkit.ChatColor.RED}${plugin.language["CommandFactionAddMemberFailedToCancelApplications"]}")
            plugin.logger.log(SEVERE, "Failed to cancel applications: ${e.message}", e)
        }
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
