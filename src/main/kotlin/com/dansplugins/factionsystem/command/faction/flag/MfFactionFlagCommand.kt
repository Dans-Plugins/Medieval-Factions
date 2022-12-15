package com.dansplugins.factionsystem.command.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionFlagCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val factionFlagListCommand = MfFactionFlagListCommand(plugin)
    private val factionFlagSetCommand = MfFactionFlagSetCommand(plugin)

    private val listAliases = listOf("list", plugin.language["CmdFactionFlagList"])
    private val setAliases = listOf("set", plugin.language["CmdFactionFlagSet"])

    private val subcommands = listAliases + setAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            in listAliases -> factionFlagListCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in setAliases -> factionFlagSetCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionFlagUsage"]}")
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
            in listAliases -> factionFlagListCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in setAliases -> factionFlagSetCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }
}
