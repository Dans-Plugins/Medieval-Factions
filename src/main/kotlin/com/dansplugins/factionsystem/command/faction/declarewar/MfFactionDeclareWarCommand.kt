package com.dansplugins.factionsystem.command.faction.declarewar

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionDeclareWarCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.declarewar")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.declareWar)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarNoFactionPermission"]}")
                    return@Runnable
                }
                val target = factionService.getFaction(args.joinToString(" "))
                if (target == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarInvalidTarget"]}")
                    return@Runnable
                }
                if (target.id.value == faction.id.value) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarCannotDeclareWarOnSelf"]}")
                    return@Runnable
                }
                val factionRelationshipService = plugin.services.factionRelationshipService
                val existingRelationships = factionRelationshipService.getRelationships(faction.id, target.id)
                val reverseRelationships = factionRelationshipService.getRelationships(target.id, faction.id)
                if (existingRelationships.any { it.type == AT_WAR }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarAlreadyAtWar", target.name]}")
                    return@Runnable
                }
                if (existingRelationships.any { it.type == ALLY } && reverseRelationships.any { it.type == ALLY }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarAlliedTarget"]}")
                    return@Runnable
                }
                if (existingRelationships.any { it.type == VASSAL }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarVassalTarget"]}")
                    return@Runnable
                }
                if (target.flags[plugin.flags.isNeutral]) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarNeutralTarget"]}")
                    return@Runnable
                }
                factionRelationshipService.save(MfFactionRelationship(factionId = faction.id, targetId = target.id, type = AT_WAR))
                    .onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarFailedToSaveRelationship"]}")
                        plugin.logger.log(SEVERE, "Failed to save faction relationship: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                factionRelationshipService.save(MfFactionRelationship(factionId = target.id, targetId = faction.id, type = AT_WAR))
                    .onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDeclareWarFailedToSaveReverseRelationship"]}")
                        plugin.logger.log(SEVERE, "Failed to save faction relationship: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                sender.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionDeclareWarSuccess", target.name]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        faction.sendMessage(
                            plugin.language["FactionAtWarNotificationTitle", target.name],
                            plugin.language["FactionAtWarNotificationBody", target.name]
                        )
                        target.sendMessage(
                            plugin.language["FactionAtWarNotificationTitle", faction.name],
                            plugin.language["FactionAtWarNotificationBody", faction.name]
                        )
                        plugin.server.onlinePlayers.filter { onlinePlayer ->
                            (faction.members + target.members).none { member -> member.playerId.toBukkitPlayer().uniqueId == onlinePlayer.uniqueId }
                        }.forEach { onlinePlayer -> onlinePlayer.sendMessage("$RED${plugin.language["FactionDeclaredWar", faction.name, target.name]}") }
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
