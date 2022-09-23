package com.dansplugins.factionsystem.command.faction.set

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.faction.set.description.MfFactionSetDescriptionCommand
import com.dansplugins.factionsystem.command.faction.set.name.MfFactionSetNameCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfFactionSetCommand(private val plugin: MedievalFactions) : CommandExecutor {
    private val factionSetNameCommand = MfFactionSetNameCommand(plugin)
    private val factionSetDescriptionCommand = MfFactionSetDescriptionCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionSetUsage"]}")
            return true
        }
        return when (args[0].lowercase()) {
            "name", plugin.language["CmdFactionSetName"] -> factionSetNameCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "description", plugin.language["CmdFactionSetDescription"] -> factionSetDescriptionCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionSetUsage"]}")
                true
            }
        }
    }
}