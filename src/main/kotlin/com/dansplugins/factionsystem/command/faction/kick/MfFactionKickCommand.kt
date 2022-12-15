package com.dansplugins.factionsystem.command.faction.kick

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.event.faction.FactionKickEvent
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.*
import java.util.logging.Level.SEVERE

class MfFactionKickCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.kick")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionKickNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionKickNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionKickUsage"]}")
            return true
        }
        val target = try {
            plugin.server.getPlayer(UUID.fromString(args.last())) ?: plugin.server.getOfflinePlayer(args.last())
        } catch (exception: IllegalArgumentException) {
            plugin.server.getOfflinePlayer(args.last())
        }
        if (!target.isOnline && !target.hasPlayedBefore()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionKickInvalidTarget"]}")
            return true
        }
        val hasForcePermission = sender.hasPermission("mf.force.kick")
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionKickFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val targetMfPlayer = playerService.getPlayer(target)
                    ?: playerService.save(MfPlayer(plugin, target)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionKickFailedToSaveTargetPlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = if (args.size > 1 && hasForcePermission) {
                    factionService.getFaction(args.dropLast(1).joinToString(" "))
                } else {
                    factionService.getFaction(mfPlayer.id)
                }
                if (faction == null) {
                    if (args.size > 1 && hasForcePermission) {
                        sender.sendMessage(
                            "$RED${plugin.language[
                                "CommandFactionKickInvalidFaction",
                                args.dropLast(1).joinToString(" ")
                            ]}"
                        )
                    } else {
                        sender.sendMessage("$RED${plugin.language["CommandFactionKickMustBeInAFaction"]}")
                    }
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.kick)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionKickNoFactionPermission"]}")
                    return@Runnable
                }
                if (mfPlayer.id.value == targetMfPlayer.id.value) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionKickCannotKickSelf"]}")
                    return@Runnable
                }
                val targetRole = faction.getRole(targetMfPlayer.id)
                if (targetRole != null && faction.members.filter { it.playerId != targetMfPlayer.id }.none {
                    val memberRole = faction.getRole(it.playerId)
                    memberRole?.hasPermission(faction, plugin.factionPermissions.setMemberRole(targetRole.id)) == true
                }
                ) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionKickNoOneCanSetTheirRole"]}")
                    return@Runnable
                }
                if (faction.members.none { it.playerId == targetMfPlayer.id }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionKickTargetNotInFaction"]}")
                    return@Runnable
                }
                val event = FactionKickEvent(faction.id, targetMfPlayer.id, true)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionKickEventCancelled"]}")
                    return@Runnable
                }
                factionService.save(
                    faction.copy(members = faction.members.filter { it.playerId != targetMfPlayer.id })
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionKickFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionKickSuccess", target.name ?: plugin.language["UnknownPlayer"], faction.name]}")
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = when {
        args.isEmpty() ->
            plugin.server.offlinePlayers
                .mapNotNull { it.name }
        args.size == 1 ->
            plugin.server.offlinePlayers
                .filter { it.name?.lowercase()?.startsWith(args[0].lowercase()) == true }
                .mapNotNull { it.name }
        else -> emptyList()
    }
}
