package com.dansplugins.factionsystem.command.power

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.power.set.MfPowerSetCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfPowerCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val powerSetCommand = MfPowerSetCommand(plugin)

    private val setAliases = listOf("set", plugin.language["CmdPowerSet"])

    private val subcommands = setAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            in setAliases -> powerSetCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandPowerUsage"]}")
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
            in setAliases -> powerSetCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }
}
