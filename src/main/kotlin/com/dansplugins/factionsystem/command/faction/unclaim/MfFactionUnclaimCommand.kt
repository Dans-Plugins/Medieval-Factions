package com.dansplugins.factionsystem.command.faction.unclaim

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionUnclaimCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val factionUnclaimAutoCommand = MfFactionUnclaimAutoCommand(plugin)
    private val factionUnclaimCircleCommand = MfFactionUnclaimCircleCommand(plugin)

    private val autoAliases = listOf("auto", plugin.language["CmdFactionUnclaimAuto"])

    private val subcommands = autoAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (args.firstOrNull()?.lowercase()) {
            in autoAliases -> factionUnclaimAutoCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                return factionUnclaimCircleCommand.onCommand(sender, command, label, args)
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
        else -> emptyList()
    }
}
