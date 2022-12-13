package com.dansplugins.factionsystem.command.faction.relationship

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import preponderous.ponder.command.unquote
import java.util.logging.Level.SEVERE

class MfFactionRelationshipRemoveCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.relationship.remove")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRelationshipRemoveNoPermission"]}")
            return true
        }
        val unquotedArgs = args.unquote()
        if (unquotedArgs.size < 3) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRelationshipRemoveUsage"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val factionService = plugin.services.factionService
                val faction1 = factionService.getFaction(MfFactionId(unquotedArgs[0])) ?: factionService.getFaction(unquotedArgs[0])
                if (faction1 == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRelationshipRemoveInvalidFaction", unquotedArgs[0]]}")
                    return@Runnable
                }
                val faction2 = factionService.getFaction(MfFactionId(unquotedArgs[1])) ?: factionService.getFaction(unquotedArgs[1])
                if (faction2 == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRelationshipRemoveInvalidFaction", unquotedArgs[1]]}")
                    return@Runnable
                }
                val relationshipType = try {
                    MfFactionRelationshipType.valueOf(unquotedArgs[2].uppercase().replace(' ', '_'))
                } catch (exception: IllegalArgumentException) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRelationshipRemoveInvalidRelationshipType"]}")
                    return@Runnable
                }
                val relationshipService = plugin.services.factionRelationshipService
                val relationships = relationshipService.getRelationships(faction1.id, faction2.id)
                val relationshipsToRemove = relationships.filter { it.type == relationshipType }
                if (relationshipsToRemove.isEmpty()) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRelationshipRemoveNoMatchingRelationships"]}")
                    return@Runnable
                }
                relationshipsToRemove.forEach { relationship ->
                    relationshipService.delete(relationship.id).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRelationshipRemoveFailedToDeleteRelationship"]}")
                        plugin.logger.log(SEVERE, "Failed to delete relationship: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionRelationshipRemoveSuccess"]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        plugin.server.dispatchCommand(sender, "faction relationship view ${faction1.id.value} ${faction2.id.value}")
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
            args.size == 3 -> MfFactionRelationshipType.values()
                .map { it.name.lowercase() }
                .filter { it.startsWith(args[2].lowercase()) }
            else -> emptyList()
        }
    }
}
