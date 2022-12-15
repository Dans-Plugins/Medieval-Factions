package com.dansplugins.factionsystem.command.faction.vassalize

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
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

class MfFactionVassalizeCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.vassalize")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.vassalize)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeNoFactionPermission"]}")
                    return@Runnable
                }
                val target = factionService.getFaction(args.joinToString(" "))
                if (target == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeInvalidTarget"]}")
                    return@Runnable
                }
                if (faction.id.value == target.id.value) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeCannotVassalizeSelf"]}")
                    return@Runnable
                }
                val factionRelationshipService = plugin.services.factionRelationshipService
                val relationships = factionRelationshipService.getRelationships(faction.id, target.id)
                val reverseRelationships = factionRelationshipService.getRelationships(target.id, faction.id)
                if (relationships.any { it.type == LIEGE } && reverseRelationships.any { it.type == VASSAL }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeCannotVassalizeLiege"]}")
                    return@Runnable
                }
                val targetLiegeRelationship = factionRelationshipService.getRelationships(target.id, LIEGE)
                    .singleOrNull { liegeRelationship ->
                        factionRelationshipService.getRelationships(liegeRelationship.targetId, target.id).any {
                            it.type == VASSAL
                        }
                    }
                if (targetLiegeRelationship != null) {
                    val targetLiege = factionService.getFaction(targetLiegeRelationship.targetId).let(::requireNotNull)
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeCannotVassalizeVassal", targetLiege.name]}")
                    return@Runnable
                }
                if (factionRelationshipService.getVassalTree(target.id).contains(faction.id)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeWouldCreateCycle"]}")
                    return@Runnable
                }
                if (relationships.any { it.type == VASSAL }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeAlreadyRequestedVassalization"]}")
                    return@Runnable
                }
                factionRelationshipService.save(
                    MfFactionRelationship(
                        factionId = faction.id,
                        targetId = target.id,
                        type = VASSAL
                    )
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionVassalizeFailedToSaveRelationship"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction relationship: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionVassalizeSuccess", target.name]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        faction.sendMessage(
                            plugin.language["FactionVassalizationRequestSentNotificationTitle", target.name],
                            plugin.language["FactionVassalizationRequestSentNotificationBody", target.name]
                        )
                        target.sendMessage(
                            plugin.language["FactionVassalizationRequestReceivedNotificationTitle", faction.name],
                            plugin.language["FactionVassalizationRequestReceivedNotificationBody", faction.name]
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
