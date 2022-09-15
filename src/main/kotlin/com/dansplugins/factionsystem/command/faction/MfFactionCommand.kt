package com.dansplugins.factionsystem.command.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.faction.ally.MfFactionAllyCommand
import com.dansplugins.factionsystem.command.faction.autoclaim.MfFactionAutoclaimCommand
import com.dansplugins.factionsystem.command.faction.breakalliance.MfFactionBreakAllianceCommand
import com.dansplugins.factionsystem.command.faction.checkclaim.MfFactionCheckClaimCommand
import com.dansplugins.factionsystem.command.faction.claim.MfFactionClaimCommand
import com.dansplugins.factionsystem.command.faction.create.MfFactionCreateCommand
import com.dansplugins.factionsystem.command.faction.declareindependence.MfFactionDeclareIndependenceCommand
import com.dansplugins.factionsystem.command.faction.declarewar.MfFactionDeclareWarCommand
import com.dansplugins.factionsystem.command.faction.disband.MfFactionDisbandCommand
import com.dansplugins.factionsystem.command.faction.grantindependence.MfFactionGrantIndependenceCommand
import com.dansplugins.factionsystem.command.faction.help.MfFactionHelpCommand
import com.dansplugins.factionsystem.command.faction.info.MfFactionInfoCommand
import com.dansplugins.factionsystem.command.faction.invite.MfFactionInviteCommand
import com.dansplugins.factionsystem.command.faction.invoke.MfFactionInvokeCommand
import com.dansplugins.factionsystem.command.faction.join.MfFactionJoinCommand
import com.dansplugins.factionsystem.command.faction.kick.MfFactionKickCommand
import com.dansplugins.factionsystem.command.faction.law.MfFactionLawCommand
import com.dansplugins.factionsystem.command.faction.leave.MfFactionLeaveCommand
import com.dansplugins.factionsystem.command.faction.list.MfFactionListCommand
import com.dansplugins.factionsystem.command.faction.makepeace.MfFactionMakePeaceCommand
import com.dansplugins.factionsystem.command.faction.map.MfFactionMapCommand
import com.dansplugins.factionsystem.command.faction.members.MfFactionMembersCommand
import com.dansplugins.factionsystem.command.faction.power.MfFactionPowerCommand
import com.dansplugins.factionsystem.command.faction.role.MfFactionRoleCommand
import com.dansplugins.factionsystem.command.faction.set.MfFactionSetCommand
import com.dansplugins.factionsystem.command.faction.swearfealty.MfFactionSwearFealtyCommand
import com.dansplugins.factionsystem.command.faction.unclaim.MfFactionUnclaimCommand
import com.dansplugins.factionsystem.command.faction.unclaimall.MfFactionUnclaimAllCommand
import com.dansplugins.factionsystem.command.faction.vassalize.MfFactionVassalizeCommand
import com.dansplugins.factionsystem.command.faction.who.MfFactionWhoCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfFactionCommand(private val plugin: MedievalFactions) : CommandExecutor {

    private val factionHelpCommand = MfFactionHelpCommand(plugin)
    private val factionCreateCommand = MfFactionCreateCommand(plugin)
    private val factionLawCommand = MfFactionLawCommand(plugin)
    private val factionAllyCommand = MfFactionAllyCommand(plugin)
    private val factionBreakAllianceCommand = MfFactionBreakAllianceCommand(plugin)
    private val factionInviteCommand = MfFactionInviteCommand(plugin)
    private val factionJoinCommand = MfFactionJoinCommand(plugin)
    private val factionDeclareWarCommand = MfFactionDeclareWarCommand(plugin)
    private val factionMakePeaceCommand = MfFactionMakePeaceCommand(plugin)
    private val factionInfoCommand = MfFactionInfoCommand(plugin)
    private val factionMembersCommand = MfFactionMembersCommand(plugin)
    private val factionRoleCommand = MfFactionRoleCommand(plugin)
    private val factionListCommand = MfFactionListCommand(plugin)
    private val factionClaimCommand = MfFactionClaimCommand(plugin)
    private val factionUnclaimCommand = MfFactionUnclaimCommand(plugin)
    private val factionCheckClaimCommand = MfFactionCheckClaimCommand(plugin)
    private val factionUnclaimAllCommand = MfFactionUnclaimAllCommand(plugin)
    private val factionAutoclaimCommand = MfFactionAutoclaimCommand(plugin)
    private val factionPowerCommand = MfFactionPowerCommand(plugin)
    private val factionWhoCommand = MfFactionWhoCommand(plugin)
    private val factionDisbandCommand = MfFactionDisbandCommand(plugin)
    private val factionInvokeCommand = MfFactionInvokeCommand(plugin)
    private val factionLeaveCommand = MfFactionLeaveCommand(plugin)
    private val factionSetCommand = MfFactionSetCommand(plugin)
    private val factionVassalizeCommand = MfFactionVassalizeCommand(plugin)
    private val factionSwearFealtyCommand = MfFactionSwearFealtyCommand(plugin)
    private val factionGrantIndependenceCommand = MfFactionGrantIndependenceCommand(plugin)
    private val factionDeclareIndependenceCommand = MfFactionDeclareIndependenceCommand(plugin)
    private val factionKickCommand = MfFactionKickCommand(plugin)
    private val factionMapCommand = MfFactionMapCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUsage"]}")
            return true
        }
        return when (args[0].lowercase()) {
            "help", plugin.language["CmdFactionHelp"] -> factionHelpCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "create", plugin.language["CmdFactionCreate"] -> factionCreateCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "law", plugin.language["CmdFactionLaw"] -> factionLawCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "ally", plugin.language["CmdFactionAlly"] -> factionAllyCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "breakalliance", "ba", plugin.language["CmdFactionBreakAlliance"] -> factionBreakAllianceCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "invite", plugin.language["CmdFactionInvite"] -> factionInviteCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "join", plugin.language["CmdFactionJoin"] -> factionJoinCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "declarewar", "dw", plugin.language["CmdFactionDeclareWar"] -> factionDeclareWarCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "makepeace", "mp", plugin.language["CmdFactionMakePeace"] -> factionMakePeaceCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "info", plugin.language["CmdFactionInfo"] -> factionInfoCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "members", plugin.language["CmdFactionMembers"] -> factionMembersCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "role", plugin.language["CmdFactionRole"] -> factionRoleCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "list", plugin.language["CmdFactionList"] -> factionListCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "claim", plugin.language["CmdFactionClaim"] -> factionClaimCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "unclaim", plugin.language["CmdFactionUnclaim"] -> factionUnclaimCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "checkclaim", plugin.language["CmdFactionCheckClaim"] -> factionCheckClaimCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "unclaimall", plugin.language["CmdFactionUnclaimAll"] -> factionUnclaimAllCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "autoclaim", plugin.language["CmdFactionAutoclaim"] -> factionAutoclaimCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "power", plugin.language["CmdFactionPower"] -> factionPowerCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "who", plugin.language["CmdFactionWho"] -> factionWhoCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "disband", plugin.language["CmdFactionDisband"] -> factionDisbandCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "invoke", plugin.language["CmdFactionInvoke"] -> factionInvokeCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "leave", plugin.language["CmdFactionLeave"] -> factionLeaveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "rename", plugin.language["CmdFactionSet"] -> factionSetCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "vassalize", "vassalise", plugin.language["CmdFactionVassalize"] -> factionVassalizeCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "swearfealty", plugin.language["CmdFactionSwearFealty"] -> factionSwearFealtyCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "grantindependence", plugin.language["CmdFactionGrantIndependence"] -> factionGrantIndependenceCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "declareindependence", plugin.language["CmdFactionDeclareIndependence"] -> factionDeclareIndependenceCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "kick", plugin.language["CmdFactionKick"] -> factionKickCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "map", plugin.language["CmdFactionMap"] -> factionMapCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionUsage"]}")
                true
            }
        }
    }
}