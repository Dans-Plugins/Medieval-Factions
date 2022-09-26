package com.dansplugins.factionsystem.command.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfFactionFlagCommand(private val plugin: MedievalFactions) : CommandExecutor {
    private val factionFlagListCommand = MfFactionFlagListCommand(plugin)
    private val factionFlagSetCommand = MfFactionFlagSetCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionFlagUsage"]}")
            return true
        }
        return when (args.first().lowercase()) {
            "list", plugin.language["CmdFactionFlagList"] -> factionFlagListCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "set", plugin.language["CmdFactionFlagSet"] -> factionFlagSetCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionFlagUsage"]}")
                true
            }
        }
    }
}