package com.dansplugins.factionsystem.command.faction.relationship

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionRelationshipCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val factionRelationshipViewCommand = MfFactionRelationshipViewCommand(plugin)
    private val factionRelationshipAddCommand = MfFactionRelationshipAddCommand(plugin)
    private val factionRelationshipRemoveCommand = MfFactionRelationshipRemoveCommand(plugin)

    private val viewAliases = listOf("view", plugin.language["CmdFactionRelationshipView"])
    private val addAliases = listOf("add", "create", plugin.language["CmdFactionRelationshipAdd"])
    private val removeAliases = listOf("remove", "delete", plugin.language["CmdFactionRelationshipRemove"])

    private val subcommands = viewAliases + addAliases + removeAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            in viewAliases -> factionRelationshipViewCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in addAliases -> factionRelationshipAddCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in removeAliases -> factionRelationshipRemoveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionRelationshipUsage"]}")
                true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = when {
        args.isEmpty() -> subcommands
        args.size == 1 -> subcommands.filter { it.startsWith(args[0].lowercase()) }
        else -> when (args.first().lowercase()) {
            in viewAliases -> factionRelationshipViewCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in addAliases -> factionRelationshipAddCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in removeAliases -> factionRelationshipRemoveCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }
}
