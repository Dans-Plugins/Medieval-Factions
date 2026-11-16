package com.dansplugins.factionsystem.command.power

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.power.clearfixed.MfPowerClearFixedCommand
import com.dansplugins.factionsystem.command.power.set.MfPowerSetCommand
import com.dansplugins.factionsystem.command.power.setfixed.MfPowerSetFixedCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfPowerCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val powerSetCommand = MfPowerSetCommand(plugin)
    private val powerSetFixedCommand = MfPowerSetFixedCommand(plugin)
    private val powerClearFixedCommand = MfPowerClearFixedCommand(plugin)

    private val setAliases = listOf("set", plugin.language["CmdPowerSet"])
    private val setFixedAliases = listOf("setfixed", plugin.language["CmdPowerSetFixed"])
    private val clearFixedAliases = listOf("clearfixed", plugin.language["CmdPowerClearFixed"])

    private val subcommands = setAliases + setFixedAliases + clearFixedAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            in setAliases -> powerSetCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in setFixedAliases -> powerSetFixedCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in clearFixedAliases -> powerClearFixedCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
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
            in setFixedAliases -> powerSetFixedCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in clearFixedAliases -> powerClearFixedCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }
}
