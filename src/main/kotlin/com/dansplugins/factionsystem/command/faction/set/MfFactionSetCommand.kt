package com.dansplugins.factionsystem.command.faction.set

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.faction.set.description.MfFactionSetDescriptionCommand
import com.dansplugins.factionsystem.command.faction.set.name.MfFactionSetNameCommand
import com.dansplugins.factionsystem.command.faction.set.prefix.MfFactionSetPrefixCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionSetCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val factionSetNameCommand = MfFactionSetNameCommand(plugin)
    private val factionSetDescriptionCommand = MfFactionSetDescriptionCommand(plugin)
    private val factionSetPrefixCommand = MfFactionSetPrefixCommand(plugin)

    private val nameAliases = listOf("name", plugin.language["CmdFactionSetName"])
    private val descriptionAliases = listOf("description", plugin.language["CmdFactionSetDescription"])
    private val prefixAliases = listOf("prefix", plugin.language["CmdFactionSetPrefix"])

    private val subcommands = nameAliases + descriptionAliases + prefixAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            in nameAliases -> factionSetNameCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in descriptionAliases -> factionSetDescriptionCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in prefixAliases -> factionSetPrefixCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionSetUsage"]}")
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
            in nameAliases -> factionSetNameCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in descriptionAliases -> factionSetDescriptionCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in prefixAliases -> factionSetPrefixCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }
}
