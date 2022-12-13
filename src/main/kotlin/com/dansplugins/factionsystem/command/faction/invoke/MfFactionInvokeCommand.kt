package com.dansplugins.factionsystem.command.faction.invoke

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import preponderous.ponder.command.unquote
import java.util.logging.Level

class MfFactionInvokeCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.invoke")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionInvokeNoPermission"]}")
            return true
        }
        val unquotedArgs = args.unquote()
        if (unquotedArgs.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionInvokeUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionInvokeNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionInvokeFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInvokerMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.invoke)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInvokeNoFactionPermission"]}")
                    return@Runnable
                }
                val ally = factionService.getFaction(unquotedArgs[0])
                if (ally == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInvokeInvalidAlly", unquotedArgs[0]]}")
                    return@Runnable
                }
                if (ally.id.value == faction.id.value) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInvokeCannotInvokeSelf"]}")
                    return@Runnable
                }
                val factionRelationshipService = plugin.services.factionRelationshipService
                val existingAllyRelationships = factionRelationshipService.getRelationships(faction.id, ally.id)
                val reverseAllyRelationships = factionRelationshipService.getRelationships(ally.id, faction.id)
                val isAlly = existingAllyRelationships.any { it.type == ALLY } && reverseAllyRelationships.any { it.type == ALLY }
                val isVassal = existingAllyRelationships.any { it.type == VASSAL } && reverseAllyRelationships.any { it.type == LIEGE }
                if (!isAlly && !isVassal) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInvokeNotAllied", ally.name]}")
                    return@Runnable
                }
                if (ally.flags[plugin.flags.isNeutral]) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInvokeNeutralAlly"]}")
                    return@Runnable
                }
                val enemy = factionService.getFaction(unquotedArgs[1])
                if (enemy == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInvokeInvalidEnemy", unquotedArgs[1]]}")
                    return@Runnable
                }
                val existingEnemyRelationships = factionRelationshipService.getRelationships(faction.id, enemy.id)
                val reverseEnemyRelationships = factionRelationshipService.getRelationships(enemy.id, faction.id)
                if (existingEnemyRelationships.none { it.type == AT_WAR } && reverseEnemyRelationships.none { it.type == AT_WAR }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInvokeNotAtWarWithEnemy"]}")
                    return@Runnable
                }
                val existingAllyEnemyRelationships = factionRelationshipService.getRelationships(ally.id, enemy.id)
                val existingEnemyAllyRelationships = factionRelationshipService.getRelationships(enemy.id, ally.id)
                if (existingAllyEnemyRelationships.any { it.type == AT_WAR } || existingEnemyAllyRelationships.any { it.type == AT_WAR }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInvokeAllyAlreadyAtWarWithEnemy"]}")
                    return@Runnable
                }
                val isAllyAlliedToEnemy = existingAllyEnemyRelationships.any { it.type == ALLY } && existingEnemyAllyRelationships.any { it.type == ALLY }
                val isEnemyVassalOfAlly = existingAllyEnemyRelationships.any { it.type == VASSAL } && existingEnemyAllyRelationships.any { it.type == LIEGE }
                if (isAllyAlliedToEnemy || isEnemyVassalOfAlly) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInvokeAllyAlliedToEnemy"]}")
                    return@Runnable
                }
                factionRelationshipService.save(MfFactionRelationship(factionId = ally.id, targetId = enemy.id, type = AT_WAR))
                    .onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionInvokeFailedToSaveRelationship"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save faction relationship: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                factionRelationshipService.save(MfFactionRelationship(factionId = enemy.id, targetId = ally.id, type = AT_WAR))
                    .onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionInvokeFailedToSaveReverseRelationship"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save faction relationship: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                sender.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionInvokeSuccess", ally.name, enemy.name]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        faction.sendMessage(
                            plugin.language["AllyInvokedNotificationTitle", ally.name, enemy.name],
                            plugin.language["AllyInvokedNotificationBody", ally.name, enemy.name]
                        )
                        ally.sendMessage(
                            plugin.language["InvokedByAllyNotificationTitle", faction.name, enemy.name],
                            plugin.language["InvokedByAllyNotificationBody", faction.name, enemy.name]
                        )
                        enemy.sendMessage(
                            plugin.language["EnemyInvokedAllyNotificationTitle", faction.name, ally.name],
                            plugin.language["EnemyInvokedAllyNotificationBody", faction.name, ally.name]
                        )
                        plugin.server.onlinePlayers.filter { onlinePlayer ->
                            (faction.members + ally.members + enemy.members).none { member -> member.playerId.toBukkitPlayer().uniqueId == onlinePlayer.uniqueId }
                        }.forEach { onlinePlayer -> onlinePlayer.sendMessage("$RED${plugin.language["FactionInvokedAlly", faction.name, ally.name, enemy.name]}") }
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
            args.size == 2 ->
                factionService.factions
                    .filter { it.name.lowercase().startsWith(args[1].lowercase()) }
                    .map(MfFaction::name)
            else -> emptyList()
        }
    }
}
