package com.dansplugins.factionsystem.command.faction.claim

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionClaimCommandManager(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter
{
    private val factionClaimCommand = MfFactionClaimCommand(plugin)
    private val factionClaimAutoCommand = MfFactionClaimAutoCommand(plugin)
    private val factionClaimFillCommand = MfFactionClaimFillCommand(plugin)
    private val factionClaimCheckCommand = MfFactionClaimCheckCommand(plugin)

    private val autoAliases = listOf("auto", plugin.language["CmdFactionClaimAuto"])
    private val fillAliases = listOf("fill", plugin.language["CmdFactionClaimFill"])
    private val checkAliases = listOf("check", plugin.language["CmdFactionClaimCheck"])
    //private val circleAliases = listOf("circle", plugin.language["CmdFactionCheckClaim"])
    //private val squareAliases = listOf("square", plugin.language["CmdFactionCheckClaim"])
    //private val rectangleAliases = listOf("rectangle", plugin.language["CmdFactionCheckClaim"])

    private val subcommands = autoAliases + fillAliases + checkAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean
    {
        return when (args.firstOrNull()?.lowercase())
        {
            null -> factionClaimCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in autoAliases -> factionClaimAutoCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in fillAliases -> factionClaimFillCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in checkAliases -> factionClaimCheckCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            //in circleAliases -> FactionClaimCircleCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            //in squareAliases -> FactionClaimSquareCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            //in rectangleAliases -> FactionClaimRectangleCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> { return true }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>) = when
    {
        args.isEmpty() -> subcommands
        args.size == 1 -> subcommands.filter { it.startsWith(args[0].lowercase()) }
        else -> emptyList()
    }
}