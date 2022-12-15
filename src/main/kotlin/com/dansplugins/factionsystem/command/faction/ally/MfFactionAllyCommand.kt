package com.dansplugins.factionsystem.command.faction.ally

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionAllyCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.ally")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAllyNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAllyUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAllyNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionAllyFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAllyMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.requestAlliance)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAllyNoFactionPermission"]}")
                    return@Runnable
                }
                val target = factionService.getFaction(args.joinToString(" "))
                if (target == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAllyInvalidTarget"]}")
                    return@Runnable
                }
                if (target.id.value == faction.id.value) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAllyCannotAllyWithSelf"]}")
                    return@Runnable
                }
                val factionRelationshipService = plugin.services.factionRelationshipService
                val existingRelationships = factionRelationshipService.getRelationships(faction.id, target.id)
                val reverseRelationships = factionRelationshipService.getRelationships(target.id, faction.id)
                if (existingRelationships.any { it.type == ALLY }) {
                    if (reverseRelationships.any { it.type == ALLY }) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionAllyAlreadyAlly"]}")
                        return@Runnable
                    } else {
                        sender.sendMessage("$RED${plugin.language["CommandFactionAllyAlreadyRequested"]}")
                        return@Runnable
                    }
                }
                if (existingRelationships.any { it.type == AT_WAR } || reverseRelationships.any { it.type == AT_WAR }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAllyAtWar"]}")
                    return@Runnable
                }
                factionRelationshipService.save(MfFactionRelationship(factionId = faction.id, targetId = target.id, type = ALLY))
                    .onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionAllyFailedToSaveRelationship"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save faction relationship: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                if (reverseRelationships.any { it.type == ALLY }) {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionAllySuccess", target.name]}")
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            faction.sendMessage(
                                plugin.language["FactionAllyNotificationTitle", target.name],
                                plugin.language["FactionAllyNotificationBody", target.name]
                            )
                            target.sendMessage(
                                plugin.language["FactionAllyNotificationTitle", faction.name],
                                plugin.language["FactionAllyNotificationBody", faction.name]
                            )
                        }
                    )
                } else {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionAllyRequested", target.name]}")
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            faction.sendMessage(
                                plugin.language["FactionAllyRequestSentNotificationTitle", sender.name, target.name],
                                plugin.language["FactionAllyRequestSentNotificationBody", sender.name, target.name]
                            )
                            target.sendMessage(
                                plugin.language["FactionAllyRequestReceivedNotificationTitle", sender.name, faction.name],
                                plugin.language["FactionAllyRequestReceivedNotificationBody", sender.name, faction.name]
                            )
                        }
                    )
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
            args.isEmpty() -> factionService.factions.map(MfFaction::name)
            args.size == 1 ->
                factionService.factions
                    .filter { it.name.lowercase().startsWith(args[0].lowercase()) }
                    .map(MfFaction::name)
            else -> emptyList()
        }
    }
}
