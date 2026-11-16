package com.dansplugins.factionsystem.command.faction.vassalage

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.faction.declareindependence.MfFactionDeclareIndependenceCommand
import com.dansplugins.factionsystem.command.faction.declinevassalization.MfFactionDeclineVassalizationCommand
import com.dansplugins.factionsystem.command.faction.grantindependence.MfFactionGrantIndependenceCommand
import com.dansplugins.factionsystem.command.faction.swearfealty.MfFactionSwearFealtyCommand
import com.dansplugins.factionsystem.command.faction.vassalize.MfFactionVassalizeCommand
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * `/faction vassalage` (#1639) gathers the vassalage commands under one parent. Every subcommand except `rescind` hands
 * its arguments to the existing top-level command, so permission checks, messages and behaviour stay in one place and
 * the top-level commands (`vassalize`, `swearfealty`, ...) keep working unchanged.
 */
class MfFactionVassalageCommand(
    private val plugin: MedievalFactions,
    private val offerCommand: MfFactionVassalizeCommand = MfFactionVassalizeCommand(plugin),
    private val acceptCommand: MfFactionSwearFealtyCommand = MfFactionSwearFealtyCommand(plugin),
    private val declineCommand: MfFactionDeclineVassalizationCommand = MfFactionDeclineVassalizationCommand(plugin),
    private val revoltCommand: MfFactionDeclareIndependenceCommand = MfFactionDeclareIndependenceCommand(plugin),
    private val releaseCommand: MfFactionGrantIndependenceCommand = MfFactionGrantIndependenceCommand(plugin),
    private val rescindCommand: MfFactionVassalageRescindCommand = MfFactionVassalageRescindCommand(plugin)
) : CommandExecutor, TabCompleter {

    private val offerAliases = listOf("offer", plugin.language["CmdFactionVassalageOffer"])
    private val acceptAliases = listOf("accept", plugin.language["CmdFactionVassalageAccept"])
    private val declineAliases = listOf("decline", plugin.language["CmdFactionVassalageDecline"])
    private val revoltAliases = listOf("revolt", plugin.language["CmdFactionVassalageRevolt"])
    private val releaseAliases = listOf("release", plugin.language["CmdFactionVassalageRelease"])
    private val rescindAliases = listOf("rescind", plugin.language["CmdFactionVassalageRescind"])

    private val subcommands = (offerAliases + acceptAliases + declineAliases + revoltAliases + releaseAliases + rescindAliases)
        .map { it.lowercase() }
        .distinct()

    private fun delegateFor(subcommand: String): Pair<CommandExecutor, TabCompleter>? = when (subcommand.lowercase()) {
        in offerAliases.lowercased() -> offerCommand to offerCommand
        in acceptAliases.lowercased() -> acceptCommand to acceptCommand
        in declineAliases.lowercased() -> declineCommand to declineCommand
        in revoltAliases.lowercased() -> revoltCommand to revoltCommand
        in releaseAliases.lowercased() -> releaseCommand to releaseCommand
        in rescindAliases.lowercased() -> rescindCommand to rescindCommand
        else -> null
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val delegate = args.firstOrNull()?.let(::delegateFor)
        if (delegate == null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionVassalageUsage"]}")
            return true
        }
        return delegate.first.onCommand(sender, command, label, args.drop(1).toTypedArray())
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> = when {
        args.isEmpty() -> subcommands
        args.size == 1 -> subcommands.filter { it.startsWith(args[0].lowercase()) }
        else -> delegateFor(args[0])?.second?.onTabComplete(sender, command, label, args.drop(1).toTypedArray()) ?: emptyList()
    }

    private fun List<String>.lowercased() = map { it.lowercase() }
}
