package com.dansplugins.factionsystem.command.gate

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.gate.cancel.MfGateCancelCommand
import com.dansplugins.factionsystem.command.gate.create.MfGateCreateCommand
import com.dansplugins.factionsystem.command.gate.remove.MfGateRemoveCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfGateCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val gateCreateCommand = MfGateCreateCommand(plugin)
    private val gateRemoveCommand = MfGateRemoveCommand(plugin)
    private val gateCancelCommand = MfGateCancelCommand(plugin)

    private val createAliases = listOf("create", "new", plugin.language["CmdGateCreate"])
    private val removeAliases = listOf("remove", "delete", plugin.language["CmdGateRemove"])
    private val cancelAliases = listOf("cancel", plugin.language["CmdGateCancel"])

    private val subcommands = createAliases + removeAliases + cancelAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            in createAliases -> gateCreateCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in removeAliases -> gateRemoveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in cancelAliases -> gateCancelCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandGateUsage"]}")
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
            in createAliases -> gateCreateCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in removeAliases -> gateRemoveCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in cancelAliases -> gateCancelCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }
}
