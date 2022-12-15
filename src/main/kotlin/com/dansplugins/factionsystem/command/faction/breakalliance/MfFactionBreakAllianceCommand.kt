package com.dansplugins.factionsystem.command.faction.breakalliance

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionBreakAllianceCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.breakalliance")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionBreakAllianceNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionBreakAllianceUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionBreakAllianceNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionBreakAllianceFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionBreakAllianceMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.breakAlliance)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionBreakAllianceNoFactionPermission"]}")
                    return@Runnable
                }
                val target = factionService.getFaction(args.joinToString(" "))
                if (target == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionBreakAllianceInvalidTarget"]}")
                    return@Runnable
                }
                val factionRelationshipService = plugin.services.factionRelationshipService
                val existingRelationships = factionRelationshipService.getRelationships(faction.id, target.id)
                val reverseRelationships = factionRelationshipService.getRelationships(target.id, faction.id)
                if (existingRelationships.none { it.type == ALLY } || reverseRelationships.none { it.type == ALLY }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionBreakAllianceNotAnAlly"]}")
                    return@Runnable
                }
                (existingRelationships + reverseRelationships).filter { it.type == ALLY }
                    .forEach { relationship ->
                        factionRelationshipService.delete(relationship.id).onFailure {
                            sender.sendMessage("$RED${plugin.language["CommandFactionBreakAllianceFailedToDeleteRelationship"]}")
                            return@forEach
                        }
                    }
                sender.sendMessage("$RED${plugin.language["CommandFactionBreakAllianceSuccess", target.name]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        faction.sendMessage(
                            plugin.language["FactionAllianceBrokenNotificationTitle", target.name],
                            plugin.language["FactionAllianceBrokenNotificationBody", target.name]
                        )
                        target.sendMessage(
                            plugin.language["FactionAllianceBrokenNotificationTitle", faction.name],
                            plugin.language["FactionAllianceBrokenNotificationBody", faction.name]
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
        val factionService = plugin.services.factionService
        return when {
            args.isEmpty() -> factionService.factions.map(MfFaction::name)
            args.size == 1 ->
                factionService.factions
                    .filter { it.name.lowercase().startsWith(args[0].lowercase()) }
                    .map(MfFaction::name)
            else -> emptyList()
        }
    }
}
