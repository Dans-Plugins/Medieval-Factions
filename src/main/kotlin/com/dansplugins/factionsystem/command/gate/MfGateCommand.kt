package com.dansplugins.factionsystem.command.gate

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.gate.cancel.MfGateCancelCommand
import com.dansplugins.factionsystem.command.gate.create.MfGateCreateCommand
import com.dansplugins.factionsystem.command.gate.remove.MfGateRemoveCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfGateCommand(private val plugin: MedievalFactions) : CommandExecutor {

    private val gateCreateCommand = MfGateCreateCommand(plugin)
    private val gateRemoveCommand = MfGateRemoveCommand(plugin)
    private val gateCancelCommand = MfGateCancelCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandGateUsage"]}")
            return true
        }
        return when (args.first().lowercase()) {
            "create", "new", plugin.language["CmdGateCreate"] -> gateCreateCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "remove", "delete", plugin.language["CmdGateRemove"] -> gateRemoveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "cancel", plugin.language["CmdGateCancel"] -> gateCancelCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandGateUsage"]}")
                true
            }
        }
    }
}