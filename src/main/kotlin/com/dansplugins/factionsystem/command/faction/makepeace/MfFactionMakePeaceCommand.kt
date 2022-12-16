package com.dansplugins.factionsystem.command.faction.makepeace

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
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

class MfFactionMakePeaceCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.makepeace")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.makePeace)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceNoFactionPermission"]}")
                    return@Runnable
                }
                val target = factionService.getFaction(args.joinToString(" "))
                if (target == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceInvalidTarget"]}")
                    return@Runnable
                }
                if (target.id.value == faction.id.value) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceCannotMakePeaceWithSelf"]}")
                    return@Runnable
                }
                val factionRelationshipService = plugin.services.factionRelationshipService
                val existingRelationships = factionRelationshipService.getRelationships(faction.id, target.id)
                val reverseRelationships = factionRelationshipService.getRelationships(target.id, faction.id)
                if (existingRelationships.none { it.type == AT_WAR }) {
                    if (reverseRelationships.any { it.type == AT_WAR }) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceAlreadyRequestedPeace"]}")
                    } else {
                        sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceNotAtWar"]}")
                    }
                    return@Runnable
                }
                existingRelationships.filter { it.type == AT_WAR }.forEach { relationship ->
                    factionRelationshipService.delete(relationship.id).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionMakePeaceFailedToDeleteRelationship"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to delete faction relationship: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
                if (reverseRelationships.any { it.type == AT_WAR }) {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionMakePeaceRequested"]}")
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            faction.sendMessage(
                                plugin.language["FactionPeaceRequestSentNotificationTitle", sender.name, target.name],
                                plugin.language["FactionPeaceRequestSentNotificationBody", sender.name, target.name]
                            )
                            target.sendMessage(
                                plugin.language["FactionPeaceRequestReceivedNotificationTitle", sender.name, faction.name],
                                plugin.language["FactionPeaceRequestReceivedNotificationBody", sender.name, faction.name]
                            )
                        }
                    )
                } else {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionMakePeaceNowAtPeace"]}")
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            faction.sendMessage(
                                plugin.language["FactionPeaceMadeNotificationTitle", target.name],
                                plugin.language["FactionPeaceMadeNotificationBody", target.name]
                            )
                            target.sendMessage(
                                plugin.language["FactionPeaceMadeNotificationTitle", faction.name],
                                plugin.language["FactionPeaceMadeNotificationBody", faction.name]
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
