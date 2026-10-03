package com.dansplugins.factionsystem.command.faction.unclaim

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionUnclaimCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val factionUnclaimAutoCommand = MfFactionUnclaimAutoCommand(plugin)
    private val factionUnclaimCircleCommand = MfFactionUnclaimCircleCommand(plugin)

    // listOfNotNull, not listOf: the language lookup returns null when the key is
    // absent from a language file, and a null in this list would make the
    // no-argument case ("/f unclaim") match and dispatch to the auto subcommand
    // instead of unclaiming the chunk the sender is standing in.
    private val autoAliases = listOfNotNull("auto", plugin.language["CmdFactionUnclaimAuto"])
        .map(String::lowercase)

    private val subcommands = autoAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val subcommand = args.firstOrNull()?.lowercase()
        return if (subcommand != null && subcommand in autoAliases) {
            factionUnclaimAutoCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
        } else {
            factionUnclaimCircleCommand.onCommand(sender, command, label, args)
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
