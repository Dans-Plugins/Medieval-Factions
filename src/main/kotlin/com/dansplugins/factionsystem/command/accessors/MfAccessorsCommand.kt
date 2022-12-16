package com.dansplugins.factionsystem.command.accessors

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.accessors.add.MfAccessorsAddCommand
import com.dansplugins.factionsystem.command.accessors.list.MfAccessorsListCommand
import com.dansplugins.factionsystem.command.accessors.remove.MfAccessorsRemoveCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfAccessorsCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val accessorsAddCommand = MfAccessorsAddCommand(plugin)
    private val accessorsRemoveCommand = MfAccessorsRemoveCommand(plugin)
    private val accessorsListCommand = MfAccessorsListCommand(plugin)

    private val addAliases = listOf(
        plugin.language["CmdAccessorsAdd"],
        "add",
        "a"
    )

    private val removeAliases = listOf(
        plugin.language["CmdAccessorsRemove"],
        "remove",
        "rm",
        "r"
    )

    private val listAliases = listOf(
        plugin.language["CmdAccessorsList"],
        "list",
        "ls",
        "l"
    )

    private val subcommands = addAliases + removeAliases + listAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandAccessorsUsage"]}")
            return true
        }
        return when (args[0].lowercase()) {
            in addAliases -> accessorsAddCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in removeAliases -> accessorsRemoveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in listAliases -> accessorsListCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandAccessorsUsage"]}")
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
            in addAliases -> accessorsAddCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in removeAliases -> accessorsRemoveCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in listAliases -> accessorsListCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }
}
