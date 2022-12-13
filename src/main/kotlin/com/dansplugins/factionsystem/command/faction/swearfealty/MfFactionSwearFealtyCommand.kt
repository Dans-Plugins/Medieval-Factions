package com.dansplugins.factionsystem.command.faction.swearfealty

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

class MfFactionSwearFealtyCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.swearfealty")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.swearFealty)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyNoFactionPermission"]}")
                    return@Runnable
                }
                val target = factionService.getFaction(args.joinToString(" "))
                if (target == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyInvalidTarget"]}")
                    return@Runnable
                }
                val factionRelationshipService = plugin.services.factionRelationshipService
                val relationships = factionRelationshipService.getRelationships(faction.id, target.id)
                val reverseRelationships = factionRelationshipService.getRelationships(target.id, faction.id)
                if (reverseRelationships.none { it.type == VASSAL }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyNoVassalizationRequest"]}")
                    return@Runnable
                }
                if (relationships.any { it.type == LIEGE }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyAlreadyVassal"]}")
                    return@Runnable
                }
                if (reverseRelationships.any { it.type == LIEGE } && relationships.any { it.type == VASSAL }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyCannotSwearFealtyToVassal"]}")
                    return@Runnable
                }
                val liegeRelationship = factionRelationshipService.getRelationships(faction.id, LIEGE)
                    .singleOrNull { vassalRelationship ->
                        factionRelationshipService.getRelationships(vassalRelationship.targetId, faction.id).any {
                            it.type == VASSAL
                        }
                    }
                if (liegeRelationship != null) {
                    val targetLiege = factionService.getFaction(liegeRelationship.targetId).let(::requireNotNull)
                    sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyAlreadyVassalOfOtherFaction", targetLiege.name]}")
                    return@Runnable
                }
                if (factionRelationshipService.getVassalTree(faction.id).contains(target.id)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyWouldCreateCycle"]}")
                    return@Runnable
                }
                factionRelationshipService.save(
                    MfFactionRelationship(
                        factionId = faction.id,
                        targetId = target.id,
                        type = LIEGE
                    )
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionSwearFealtyFailedToSaveRelationship"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction relationship: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionSwearFealtySuccess", target.name]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        faction.sendMessage(
                            plugin.language["FactionFealtySwornNotificationTitle", target.name],
                            plugin.language["FactionFealtySwornNotificationBody", target.name]
                        )
                        target.sendMessage(
                            plugin.language["FactionNewVassalNotificationTitle", faction.name],
                            plugin.language["FactionNewVassalNotificationBody", faction.name]
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
