package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfFactionRoleCommand(private val plugin: MedievalFactions) : CommandExecutor {

    private val factionRoleViewCommand = MfFactionRoleViewCommand(plugin)
    private val factionRoleSetPermissionCommand = MfFactionRoleSetPermissionCommand(plugin)
    private val factionRoleListCommand = MfFactionRoleListCommand(plugin)
    private val factionRoleSetCommand = MfFactionRoleSetCommand(plugin)
    private val factionRoleCreateCommand = MfFactionRoleCreateCommand(plugin)
    private val factionRoleDeleteCommand = MfFactionRoleDeleteCommand(plugin)
    private val factionRoleRenameCommand = MfFactionRoleRenameCommand(plugin)
    private val factionRoleSetDefaultCommand = MfFactionRoleSetDefaultCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleUsage"]}")
            return true
        }
        return when (args[0].lowercase()) {
            "view", plugin.language["CmdFactionRoleView"] -> factionRoleViewCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "setpermission", plugin.language["CmdFactionRoleSetPermission"] -> factionRoleSetPermissionCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "list", plugin.language["CmdFactionRoleList"] -> factionRoleListCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "set", plugin.language["CmdFactionRoleSet"] -> factionRoleSetCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "create", "add", plugin.language["CmdFactionRoleCreate"] -> factionRoleCreateCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "delete", "remove", plugin.language["CmdFactionRoleDelete"] -> factionRoleDeleteCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "rename", "setname", plugin.language["CmdFactionRoleRename"] -> factionRoleRenameCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "setdefault", plugin.language["CmdFactionRoleSetDefault"] -> factionRoleSetDefaultCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionRoleUsage"]}")
                true
            }
        }
    }
}