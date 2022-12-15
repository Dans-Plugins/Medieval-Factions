package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
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

class MfFactionRoleSetDefaultCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.role.setdefault")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetDefaultNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetDefaultNotAPlayer"]}")
            return true
        }
        var lastArgOffset = 0
        val returnPage = if (args.lastOrNull()?.startsWith("p=") == true) {
            lastArgOffset = 1
            args.last().substring("p=".length).toIntOrNull()
        } else {
            null
        }
        if (args.dropLast(lastArgOffset).isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetDefaultUsage"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetDefaultFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetDefaultMustBeInAFaction"]}")
                    return@Runnable
                }
                val targetRoleName = args.dropLast(lastArgOffset).joinToString(" ")
                val targetRole =
                    faction.roles.getRole(MfFactionRoleId(targetRoleName)) ?: faction.roles.getRole(targetRoleName)
                if (targetRole == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetDefaultInvalidTargetRole"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null ||
                    !role.hasPermission(faction, plugin.factionPermissions.setDefaultRole) ||
                    !role.hasPermission(faction, plugin.factionPermissions.setMemberRole(targetRole.id))
                ) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetDefaultNoFactionPermission"]}")
                    return@Runnable
                }
                factionService.save(
                    faction.copy(
                        roles = faction.roles.copy(defaultRoleId = targetRole.id)
                    )
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetDefaultFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionRoleSetDefaultSuccess", targetRole.name]}")
                if (returnPage != null) {
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            sender.performCommand("faction role list $returnPage")
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
        if (sender !is Player) return emptyList()
        val playerId = MfPlayerId.fromBukkitPlayer(sender)
        val factionService = plugin.services.factionService
        val faction = factionService.getFaction(playerId) ?: return emptyList()
        return when {
            args.isEmpty() -> faction.roles.map { it.name }
            args.size == 1 -> faction.roles.filter { it.name.lowercase().startsWith(args[0].lowercase()) }.map { it.name }
            else -> emptyList()
        }
    }
}
