package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.unquote
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level

/**
 * `/faction role setprefix [role] [prefix]` sets the prefix shown for a role's members, such as `[Knight]`, and
 * `/faction role setprefix [role]` clears it (#1525). It needs the same `MODIFY_ROLE` faction permission for the role as
 * `/faction role rename`.
 */
class MfFactionRoleSetPrefixCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean =
        execute(sender, args, null)

    /**
     * @param forcedFactionId a faction named with mf.force.role (see [MfFactionRoleCommand]). It replaces the sender's own
     * faction, and its role-permission checks are skipped.
     */
    fun execute(sender: CommandSender, args: Array<out String>, forcedFactionId: MfFactionId?): Boolean {
        if (!sender.hasPermission("mf.role.setprefix")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPrefixNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPrefixNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPrefixUsage"]}")
            return true
        }
        val unquotedArgs = args.unquote()
        val prefix = parsePrefix(unquotedArgs.drop(1))
        if (prefix != null && prefix.length > MfFactionRole.MAX_PREFIX_LENGTH) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPrefixTooLong", MfFactionRole.MAX_PREFIX_LENGTH.toString()]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPrefixFailedToSavePlayer"]}")
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
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPrefixMustBeInAFaction"]}")
                        return@Runnable
                    }
                }
                val targetRole = faction.roles.getRole(MfFactionRoleId(unquotedArgs[0])) ?: faction.roles.getRole(unquotedArgs[0])
                if (targetRole == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPrefixInvalidTargetRole"]}")
                    return@Runnable
                }
                if (forcedFactionId == null) {
                    val role = faction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(faction, plugin.factionPermissions.modifyRole(targetRole.id))) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPrefixNoFactionPermission"]}")
                        return@Runnable
                    }
                }
                factionService.save(
                    faction.copy(
                        roles = faction.roles.copy(
                            roles = faction.roles.map { existingRole ->
                                if (existingRole.id == targetRole.id) existingRole.copy(prefix = prefix) else existingRole
                            }
                        )
                    )
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleSetPrefixFailedToSaveFaction"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                if (prefix == null) {
                    sender.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionRoleSetPrefixCleared", targetRole.name]}")
                } else {
                    sender.sendMessage(
                        "${ChatColor.GREEN}${plugin.language["CommandFactionRoleSetPrefixSuccess", targetRole.name, ChatColor.translateAlternateColorCodes('&', prefix) + ChatColor.GREEN]}"
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

    internal companion object {
        /** Joins the words after the role into the prefix; no words, or only spaces, clears it. */
        fun parsePrefix(words: List<String>): String? = words.joinToString(" ").trim().ifEmpty { null }
    }
}
