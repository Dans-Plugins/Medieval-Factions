package com.dansplugins.factionsystem.command.faction.relationship

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfFactionRelationshipCommand(private val plugin: MedievalFactions) : CommandExecutor {
    private val factionRelationshipViewCommand = MfFactionRelationshipViewCommand(plugin)
    private val factionRelationshipAddCommand = MfFactionRelationshipAddCommand(plugin)
    private val factionRelationshipRemoveCommand = MfFactionRelationshipRemoveCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            "view", plugin.language["CmdFactionRelationshipView"] -> factionRelationshipViewCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "add", "create", plugin.language["CmdFactionRelationshipAdd"] -> factionRelationshipAddCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "remove", "delete", plugin.language["CmdFactionRelationshipRemove"] -> factionRelationshipRemoveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionRelationshipUsage"]}")
                true
            }
        }
    }
}