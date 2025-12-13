package com.dansplugins.factionsystem.command.faction.addmember

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level
import java.util.logging.Level.SEVERE
import net.md_5.bungee.api.ChatColor as SpigotChatColor

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

        // Check for force flag
        var lastArgOffset = 0
        val force = if (args.lastOrNull() == "-f") {
            lastArgOffset = 1
            true
        } else {
            false
        }

        // Ensure we have enough args after accounting for potential flag
        if (args.size <= lastArgOffset + 1) {
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

        // get target faction
        val factionNameArgs = args.drop(1).dropLast(lastArgOffset)
        val targetFaction = factionService.getFaction(factionNameArgs.joinToString(" "))
        if (targetFaction == null) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberInvalidTargetFaction"]}")
            return true
        }

        // if target player is already in a faction, handle accordingly
        val currentFaction = factionService.getFaction(targetMfPlayer.id)
        if (currentFaction != null) {
            if (!force) {
                // Prompt user to confirm removal from current faction
                confirmAddMember(sender, targetMfPlayer, currentFaction, targetFaction, args.dropLast(lastArgOffset))
                return true
            } else {
                // Remove from current faction
                val updatedCurrentFaction = factionService.save(
                    currentFaction.copy(
                        members = currentFaction.members.filter { it.playerId != targetMfPlayer.id }
                    )
                ).onFailure {
                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberFailedToRemoveFromCurrentFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to remove player from current faction: ${it.reason.message}", it.reason.cause)
                    return true
                }
                // Notify old faction
                var targetName = targetMfPlayer.name ?: plugin.language["CommandFactionAddMemberUnknownNewPlayerFaction"]
                updatedCurrentFaction.sendMessage(
                    plugin.language["CommandFactionAddMemberRemovedFromFactionTitle", targetName],
                    plugin.language["CommandFactionAddMemberRemovedFromFactionBody", targetName]
                )
            }
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

    private fun confirmAddMember(
        player: Player,
        targetPlayer: MfPlayer,
        currentFaction: MfFaction,
        targetFaction: MfFaction,
        originalArgs: List<String>
    ) {
        val targetName = targetPlayer.name ?: plugin.language["CommandFactionAddMemberUnknownNewPlayerFaction"]
        player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionAddMemberConfirmRemoval", targetName, currentFaction.name, targetFaction.name]}")
        player.spigot().sendMessage(
            TextComponent(plugin.language["CommandFactionAddMemberConfirmButton"]).apply {
                color = SpigotChatColor.GREEN
                isBold = true
                hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionAddMemberConfirmButtonHover"]))
                clickEvent = ClickEvent(RUN_COMMAND, "/mf addmember ${originalArgs.joinToString(" ")} -f")
            },
            TextComponent(" "),
            TextComponent(plugin.language["CommandFactionAddMemberCancelButton"]).apply {
                color = SpigotChatColor.RED
                isBold = true
                hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionAddMemberCancelButtonHover"]))
                clickEvent = ClickEvent(RUN_COMMAND, "/mf help")
            }
        )
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
