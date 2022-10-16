package com.dansplugins.factionsystem.command.accessors

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.accessors.add.MfAccessorsAddCommand
import com.dansplugins.factionsystem.command.accessors.list.MfAccessorsListCommand
import com.dansplugins.factionsystem.command.accessors.remove.MfAccessorsRemoveCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfAccessorsCommand(private val plugin: MedievalFactions) : CommandExecutor {

    private val accessorsAddCommand = MfAccessorsAddCommand(plugin)
    private val accessorsRemoveCommand = MfAccessorsRemoveCommand(plugin)
    private val accessorsListCommand = MfAccessorsListCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandAccessorsUsage"]}")
            return true
        }
        return when (args[0].lowercase()) {
            "add", "a", plugin.language["CmdAccessorsAdd"] -> accessorsAddCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "remove", "rm", "r", plugin.language["CmdAccessorsRemove"] -> accessorsRemoveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "list", "ls", "l", plugin.language["CmdAccessorsList"] -> accessorsListCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandAccessorsUsage"]}")
                true
            }
        }
    }
}