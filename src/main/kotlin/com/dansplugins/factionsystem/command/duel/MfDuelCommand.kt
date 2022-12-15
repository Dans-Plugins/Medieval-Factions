package com.dansplugins.factionsystem.command.duel

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.duel.accept.MfDuelAcceptCommand
import com.dansplugins.factionsystem.command.duel.cancel.MfDuelCancelCommand
import com.dansplugins.factionsystem.command.duel.challenge.MfDuelChallengeCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfDuelCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val duelChallengeCommand = MfDuelChallengeCommand(plugin)
    private val duelAcceptCommand = MfDuelAcceptCommand(plugin)
    private val duelCancelCommand = MfDuelCancelCommand(plugin)

    private val challengeAliases = listOf("challenge", plugin.language["CmdDuelChallenge"])
    private val acceptAliases = listOf("accept", plugin.language["CmdDuelAccept"])
    private val cancelAliases = listOf("cancel", "decline", plugin.language["CmdDuelCancel"])

    private val subcommands = challengeAliases + acceptAliases + cancelAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            in challengeAliases -> duelChallengeCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in acceptAliases -> duelAcceptCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            in cancelAliases -> duelCancelCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandDuelUsage"]}")
                true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = when {
        args.isEmpty() -> subcommands
        args.size == 1 -> subcommands.filter { it.startsWith(args[0].lowercase()) }
        else -> when (args.first().lowercase()) {
            in challengeAliases -> duelChallengeCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in acceptAliases -> duelAcceptCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            in cancelAliases -> duelCancelCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }
}
