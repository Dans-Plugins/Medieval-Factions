package com.dansplugins.factionsystem.command.power

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.power.set.MfPowerSetCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfPowerCommand(private val plugin: MedievalFactions) : CommandExecutor {

    private val powerSetCommand = MfPowerSetCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandPowerUsage"]}")
            return true
        }
        return when (args.first().lowercase()) {
            "set" -> powerSetCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandPowerUsage"]}")
                true
            }
        }
    }
}