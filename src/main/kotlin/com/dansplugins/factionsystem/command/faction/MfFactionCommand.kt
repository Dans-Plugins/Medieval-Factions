package com.dansplugins.factionsystem.command.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.faction.create.MfFactionCreateCommand
import com.dansplugins.factionsystem.command.faction.law.MfFactionLawCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfFactionCommand(private val plugin: MedievalFactions) : CommandExecutor {

    private val factionCreateCommand = MfFactionCreateCommand(plugin)
    private val factionLawCommand = MfFactionLawCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUsage"]}")
            return true
        }
        return when (args[0].lowercase()) {
            "create" -> factionCreateCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "law" -> factionLawCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionUsage"]}")
                true
            }
        }
    }
}