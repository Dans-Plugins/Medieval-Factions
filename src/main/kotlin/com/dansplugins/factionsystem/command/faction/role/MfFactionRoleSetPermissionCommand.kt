package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
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

class MfFactionRoleSetPermissionCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean =
        execute(sender, args, null)

    /**
     * @param forcedFactionId a faction named with mf.force.role (see [MfFactionRoleCommand]). It replaces the sender's own
     * faction, and its role-permission checks are skipped.
     */
    fun execute(sender: CommandSender, args: Array<out String>, forcedFactionId: MfFactionId?): Boolean {
        if (!sender.hasPermission("mf.role.setpermission")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPermissionNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPermissionNotAPlayer"]}")
            return true
        }
        if (args.size < 3) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPermissionUsage"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPermissionFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
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
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPermissionMustBeInAFaction"]}")
                        return@Runnable
                    }
                }
                var lastArgOffset = 0
                val returnPage = if (args.last().startsWith("p=")) {
                    lastArgOffset = 1
                    args.last().substring("p=".length).toIntOrNull()
                } else {
                    null
                }
                val permission = plugin.factionPermissions.parse(args[args.lastIndex - (1 + lastArgOffset)])
                if (permission == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPermissionInvalidPermission"]}")
                    return@Runnable
                }
                val permissionValue = when (args[args.lastIndex - lastArgOffset]) {
                    "allow" -> true
                    "deny" -> false
                    "default" -> null
                    else -> null
                }
                val targetRole = faction.getRole(args.dropLast(2 + lastArgOffset).joinToString(" "))
                if (targetRole == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPermissionInvalidTargetRole"]}")
                    return@Runnable
                }
                // With mf.force.role the admin is not acting through a role in the faction, so neither the role check
                // nor the guard against removing one's own ability to modify one's own role applies.
                if (forcedFactionId == null) {
                    val playerRole = faction.getRole(mfPlayer.id)
                    if (playerRole == null || !playerRole.hasPermission(
                            faction,
                            plugin.factionPermissions.setRolePermission(permission)
                        ) || !playerRole.hasPermission(faction, plugin.factionPermissions.modifyRole(targetRole.id))
                    ) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPermissionNoFactionPermission"]}")
                        return@Runnable
                    }

                    if (permission == plugin.factionPermissions.modifyRole(playerRole.id)) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPermissionCannotModifyRolePermissionToModifyOwnRolePermission"]}")
                        return@Runnable
                    }
                }
                val updatedFaction =
                    factionService.save(
                        faction.copy(
                            roles = faction.roles.copy(
                                roles = faction.roles.map {
                                    if (it.id.value == targetRole.id.value) {
                                        targetRole.copy(permissionsByName = targetRole.permissionsByName + (permission.name to permissionValue))
                                    } else {
                                        it
                                    }
                                }
                            )
                        )
                    ).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPermissionFailedToSaveFaction"]}")
                        plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                sender.sendMessage(
                    "$GREEN${
                    plugin.language[
                        "CommandFactionRoleSetPermissionSuccess", targetRole.name, permission.translate(
                            updatedFaction
                        ), when (permissionValue) {
                            true -> "allow"
                            false -> "deny"
                            null -> "default"
                        }
                    ]
                    }"
                )
                if (returnPage != null) {
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            sender.performCommand("${roleCommandPrefix(forcedFactionId)} view ${targetRole.id.value} $returnPage")
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
            args.size == 1 -> faction.roles.filter { it.name.lowercase().startsWith(args[0].lowercase()) }
                .map { it.name }

            args.size == 2 -> plugin.factionPermissions.permissionsFor(faction)
                .filter { it.name.lowercase().startsWith(args[1].lowercase()) }.map { it.name }

            args.size == 3 -> listOf("allow", "deny", "default").filter { it.startsWith(args[2].lowercase()) }
            else -> emptyList()
        }
    }
}
