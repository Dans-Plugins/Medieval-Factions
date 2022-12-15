package com.dansplugins.factionsystem.command.faction.grantindependence

import com.dansplugins.factionsystem.MedievalFactions
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

class MfFactionGrantIndependenceCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.grantindependence")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionGrantIndependenceNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionGrantIndependenceUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionGrantIndependenceNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionGrantIndependenceFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionGrantIndependenceMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.grantIndependence)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionGrantIndependenceNoFactionPermission"]}")
                    return@Runnable
                }
                val target = factionService.getFaction(args.joinToString(" "))
                if (target == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionGrantIndependenceInvalidTarget"]}")
                    return@Runnable
                }
                val factionRelationshipService = plugin.services.factionRelationshipService
                val relationships = factionRelationshipService.getRelationships(faction.id, target.id)
                val reverseRelationships = factionRelationshipService.getRelationships(target.id, faction.id)
                if (relationships.none { it.type == VASSAL } || reverseRelationships.none { it.type == LIEGE }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionGrantIndependenceNotAVassal"]}")
                    return@Runnable
                }
                (relationships.filter { it.type == VASSAL } + reverseRelationships.filter { it.type == LIEGE })
                    .forEach { relationship ->
                        factionRelationshipService.delete(relationship.id).onFailure {
                            sender.sendMessage("$RED${plugin.language["CommandFactionGrantIndependenceFailedToDeleteRelationship"]}")
                            plugin.logger.log(SEVERE, "Failed to delete relationship: ${it.reason.message}", it.reason.cause)
                            return@Runnable
                        }
                    }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionGrantIndependenceSuccess", target.name]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        faction.sendMessage(
                            plugin.language["VassalGrantedIndependenceNotificationTitle", target.name],
                            plugin.language["VassalGrantedIndependenceNotificationBody", target.name]
                        )
                        target.sendMessage(
                            plugin.language["FactionGrantedIndependenceNotificationTitle", faction.name],
                            plugin.language["FactionGrantedIndependenceNotificationBody", faction.name]
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
