package com.dansplugins.factionsystem.command.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.faction.ally.MfFactionAllyCommand
import com.dansplugins.factionsystem.command.faction.breakalliance.MfFactionBreakAllianceCommand
import com.dansplugins.factionsystem.command.faction.create.MfFactionCreateCommand
import com.dansplugins.factionsystem.command.faction.declarewar.MfFactionDeclareWarCommand
import com.dansplugins.factionsystem.command.faction.invite.MfFactionInviteCommand
import com.dansplugins.factionsystem.command.faction.join.MfFactionJoinCommand
import com.dansplugins.factionsystem.command.faction.law.MfFactionLawCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfFactionCommand(private val plugin: MedievalFactions) : CommandExecutor {

    private val factionCreateCommand = MfFactionCreateCommand(plugin)
    private val factionLawCommand = MfFactionLawCommand(plugin)
    private val factionAllyCommand = MfFactionAllyCommand(plugin)
    private val factionBreakAllianceCommand = MfFactionBreakAllianceCommand(plugin)
    private val factionInviteCommand = MfFactionInviteCommand(plugin)
    private val factionJoinCommand = MfFactionJoinCommand(plugin)
    private val factionDeclareWarCommand = MfFactionDeclareWarCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUsage"]}")
            return true
        }
        return when (args[0].lowercase()) {
            "create", plugin.language["CmdFactionCreate"] -> factionCreateCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "law", plugin.language["CmdFactionLaw"] -> factionLawCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "ally", plugin.language["CmdFactionAlly"] -> factionAllyCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "breakalliance", "ba", plugin.language["CmdFactionBreakAlliance"] -> factionBreakAllianceCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "invite", plugin.language["CmdFactionInvite"] -> factionInviteCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "join", plugin.language["CmdFactionJoin"] -> factionJoinCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "declarewar", "dw", plugin.language["CmdFactionDeclareWar"] -> factionDeclareWarCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionUsage"]}")
                true
            }
        }
    }
}