package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionRoleSetCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.role.set")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetNotAPlayer"]}")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetUsage"]}")
            return true
        }
        val target = plugin.server.getOfflinePlayer(args[0])
        if (!target.isOnline && !target.hasPlayedBefore()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetInvalidTarget"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val targetMfPlayer = playerService.getPlayer(target)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetFailedToSaveTargetPlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save target player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                if (mfPlayer.id.value == targetMfPlayer.id.value && !mfPlayer.isBypassEnabled) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetCannotSetOwnRole"]}")
                    return@Runnable
                }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(targetMfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetMustBeInAFaction"]}")
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetTargetMustBeInAFaction"]}")
                    return@Runnable
                }
                if (faction.members.none { it.playerId == targetMfPlayer.id }) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetTargetMustBeInFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                val targetRole = faction.getRole(args.drop(1).joinToString(" "))
                if (targetRole == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetInvalidTargetRole"]}")
                    return@Runnable
                }
                if (!mfPlayer.isBypassEnabled) {
                    if (role == null || !role.hasPermission(
                            faction,
                            plugin.factionPermissions.setMemberRole(targetRole.id)
                        )
                    ) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetNoFactionPermission"]}")
                        return@Runnable
                    }
                }
                factionService.save(
                    faction.copy(
                        members = faction.members.map { member ->
                            if (member.playerId == targetMfPlayer.id) {
                                member.copy(role = targetRole)
                            } else {
                                member
                            }
                        }
                    )
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionRoleSetSuccess", target.name ?: "", targetRole.name]}")
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
        return when {
            args.isEmpty() ->
                plugin.server.offlinePlayers
                    .mapNotNull { it.name }
            args.size == 1 ->
                plugin.server.offlinePlayers
                    .filter { it.name?.lowercase()?.startsWith(args[0].lowercase()) == true }
                    .mapNotNull { it.name }
            args.size == 2 -> {
                if (sender !is Player) return emptyList()
                val playerId = MfPlayerId.fromBukkitPlayer(sender)
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(playerId) ?: return emptyList()
                faction.roles.filter { it.name.lowercase().startsWith(args[0].lowercase()) }.map { it.name }
            }
            else -> emptyList()
        }
    }
}
