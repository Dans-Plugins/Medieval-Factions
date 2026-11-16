package com.dansplugins.factionsystem.command.faction.rival

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.rival.MfFactionRival
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.util.logging.Level.SEVERE
import kotlin.math.ceil

/**
 * `/faction rival [faction]` names a rival, and `/faction unrival [faction]` (with [remove]) ends a rivalry (#1896).
 *
 * A rivalry is one-sided and changes nothing in the world: it is shown in `/faction info` and both factions are told.
 * Naming or ending one needs the same faction role permission as declaring war. With
 * `factions.minimumRivalryHours` above 0, a rivalry cannot be ended until it is that many hours old.
 */
class MfFactionRivalCommand(private val plugin: MedievalFactions, private val remove: Boolean) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.rival")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRivalNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language[if (remove) "CommandFactionUnrivalUsage" else "CommandFactionRivalUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRivalNotAPlayer"]}")
            return true
        }
        val minimumRivalryHours = plugin.config.getDouble("factions.minimumRivalryHours")
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRivalFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRivalMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.declareWar)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRivalNoFactionPermission"]}")
                    return@Runnable
                }
                val target = factionService.getFaction(args.joinToString(" "))
                if (target == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRivalInvalidTarget"]}")
                    return@Runnable
                }
                if (target.id == faction.id) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRivalCannotRivalSelf"]}")
                    return@Runnable
                }
                if (remove) {
                    endRivalry(sender, faction, target, minimumRivalryHours)
                } else {
                    nameRival(sender, faction, target)
                }
            }
        )
        return true
    }

    private fun nameRival(sender: Player, faction: MfFaction, target: MfFaction) {
        val rivalService = plugin.services.factionRivalService
        if (rivalService.getRival(faction.id, target.id) != null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRivalAlreadyRival", target.name]}")
            return
        }
        val relationshipService = plugin.services.factionRelationshipService
        if (relationshipService.getRelationships(faction.id, target.id).any { it.type == ALLY } &&
            relationshipService.getRelationships(target.id, faction.id).any { it.type == ALLY }
        ) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRivalAllied", target.name]}")
            return
        }
        rivalService.add(MfFactionRival(faction.id, target.id)).onFailure {
            sender.sendMessage("$RED${plugin.language["CommandFactionRivalFailedToSave"]}")
            plugin.logger.log(SEVERE, "Failed to save rival: ${it.reason.message}", it.reason.cause)
            return
        }
        sender.sendMessage("$GREEN${plugin.language["CommandFactionRivalSuccess", target.name]}")
        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                target.sendMessage(
                    plugin.language["FactionRivalNotificationTitle"],
                    plugin.language["FactionRivalNotificationBody", faction.name]
                )
            }
        )
    }

    private fun endRivalry(sender: Player, faction: MfFaction, target: MfFaction, minimumRivalryHours: Double) {
        val rivalService = plugin.services.factionRivalService
        val rival = rivalService.getRival(faction.id, target.id)
        if (rival == null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnrivalNotARival", target.name]}")
            return
        }
        if (minimumRivalryHours > 0) {
            val endsAt = rival.createdAt.plusMillis((minimumRivalryHours * 3_600_000).toLong())
            val now = Instant.now()
            if (now.isBefore(endsAt)) {
                val hoursLeft = ceil(Duration.between(now, endsAt).toMinutes() / 60.0).toInt().coerceAtLeast(1)
                sender.sendMessage("$RED${plugin.language["CommandFactionUnrivalTooSoon", target.name, hoursLeft.toString()]}")
                return
            }
        }
        rivalService.remove(faction.id, target.id).onFailure {
            sender.sendMessage("$RED${plugin.language["CommandFactionRivalFailedToSave"]}")
            plugin.logger.log(SEVERE, "Failed to delete rival: ${it.reason.message}", it.reason.cause)
            return
        }
        sender.sendMessage("$GREEN${plugin.language["CommandFactionUnrivalSuccess", target.name]}")
        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                target.sendMessage(
                    plugin.language["FactionUnrivalNotificationTitle"],
                    plugin.language["FactionUnrivalNotificationBody", faction.name]
                )
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
            args.isEmpty() -> factionService.factions.map(MfFaction::name)
            args.size == 1 ->
                factionService.factions
                    .filter { it.name.lowercase().startsWith(args[0].lowercase()) }
                    .map(MfFaction::name)
            else -> emptyList()
        }
    }
}
