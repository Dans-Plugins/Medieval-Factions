package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.unquote
import com.dansplugins.factionsystem.faction.MfFactionId
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
import java.util.logging.Level

/**
 * `/faction role move <role> <up|down>`: moves a role one place up or down the faction's role list. The order is
 * only how the roles are listed; it grants nothing. Moving a role needs the same faction permission as renaming it,
 * MODIFY_ROLE for that role.
 */
class MfFactionRoleMoveCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean =
        execute(sender, args, null)

    /**
     * @param forcedFactionId a faction named with mf.force.role (see [MfFactionRoleCommand]). It replaces the sender's own
     * faction, and its role-permission checks are skipped.
     */
    fun execute(sender: CommandSender, args: Array<out String>, forcedFactionId: MfFactionId?): Boolean {
        if (!sender.hasPermission("mf.role.move")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleMoveNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleMoveNotAPlayer"]}")
            return true
        }
        var lastArgOffset = 0
        val returnPage = if (args.lastOrNull()?.startsWith("p=") == true) {
            lastArgOffset = 1
            args.last().substring("p=".length).toIntOrNull()
        } else {
            null
        }
        val unquotedArgs = args.dropLast(lastArgOffset).toTypedArray().unquote()
        val offset = when (unquotedArgs.getOrNull(1)?.lowercase()) {
            "up" -> -1
            "down" -> 1
            else -> null
        }
        if (unquotedArgs.size != 2 || offset == null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleMoveUsage"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleMoveFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = if (forcedFactionId != null) {
                    factionService.getFaction(forcedFactionId) ?: run {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleInvalidFaction", forcedFactionId.value]}")
                        return@Runnable
                    }
                } else {
                    factionService.getFaction(mfPlayer.id) ?: run {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleMoveMustBeInAFaction"]}")
                        return@Runnable
                    }
                }
                val targetRole = faction.roles.getRole(MfFactionRoleId(unquotedArgs[0])) ?: faction.roles.getRole(unquotedArgs[0])
                if (targetRole == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleMoveInvalidTargetRole"]}")
                    return@Runnable
                }
                if (forcedFactionId == null) {
                    val role = faction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(faction, plugin.factionPermissions.modifyRole(targetRole.id))) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleMoveNoFactionPermission"]}")
                        return@Runnable
                    }
                }
                val reorderedRoles = faction.roles.move(targetRole.id, offset) ?: run {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleMoveCannotMoveFurther", targetRole.name]}")
                    return@Runnable
                }
                factionService.save(faction.copy(roles = reorderedRoles)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleMoveFailedToSaveFaction"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionRoleMoveSuccess", targetRole.name]}")
                if (returnPage != null) {
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            sender.performCommand("${roleCommandPrefix(forcedFactionId)} list $returnPage")
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
            args.size == 2 -> listOf("up", "down").filter { it.startsWith(args[1].lowercase()) }
            else -> emptyList()
        }
    }
}
