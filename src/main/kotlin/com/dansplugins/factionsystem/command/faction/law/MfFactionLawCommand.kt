package com.dansplugins.factionsystem.command.faction.law

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionLawCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val factionLawAddCommand = MfFactionLawAddCommand(plugin)
    private val factionLawEditCommand = MfFactionLawEditCommand(plugin)
    private val factionLawMoveCommand = MfFactionLawMoveCommand(plugin)
    private val factionLawRemoveCommand = MfFactionLawRemoveCommand(plugin)
    private val factionLawListCommand = MfFactionLawListCommand(plugin)

    private val addAliases = listOf("add", plugin.language["CmdFactionLawAdd"])
    private val editAliases = listOf("edit", plugin.language["CmdFactionLawEdit"])
    private val moveAliases = listOf("move", plugin.language["CmdFactionLawMove"])
    private val removeAliases = listOf("remove", plugin.language["CmdFactionLawRemove"])
    private val listAliases = listOf("list", plugin.language["CmdFactionLawList"])

    private val subcommands = addAliases + editAliases + moveAliases + removeAliases + listAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            in addAliases -> factionLawAddCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in editAliases -> factionLawEditCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in moveAliases -> factionLawMoveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in removeAliases -> factionLawRemoveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in listAliases -> factionLawListCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionLawUsage"]}")
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
            in addAliases -> factionLawAddCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in removeAliases -> factionLawRemoveCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in listAliases -> factionLawListCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }
}
