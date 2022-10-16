package com.dansplugins.factionsystem.command.duel

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.duel.accept.MfDuelAcceptCommand
import com.dansplugins.factionsystem.command.duel.cancel.MfDuelCancelCommand
import com.dansplugins.factionsystem.command.duel.challenge.MfDuelChallengeCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfDuelCommand(private val plugin: MedievalFactions) : CommandExecutor {
    private val duelChallengeCommand = MfDuelChallengeCommand(plugin)
    private val duelAcceptCommand = MfDuelAcceptCommand(plugin)
    private val duelCancelCommand = MfDuelCancelCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            "challenge", plugin.language["CmdDuelChallenge"] -> duelChallengeCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "accept", plugin.language["CmdDuelAccept"] -> duelAcceptCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "cancel", "decline", plugin.language["CmdDuelCancel"] -> duelCancelCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandDuelUsage"]}")
                true
            }
        }
    }
}