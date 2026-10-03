package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.splitLeadingArg
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionRoleCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val factionRoleViewCommand = MfFactionRoleViewCommand(plugin)
    private val factionRoleSetPermissionCommand = MfFactionRoleSetPermissionCommand(plugin)
    private val factionRoleListCommand = MfFactionRoleListCommand(plugin)
    private val factionRoleSetCommand = MfFactionRoleSetCommand(plugin)
    private val factionRoleCreateCommand = MfFactionRoleCreateCommand(plugin)
    private val factionRoleDeleteCommand = MfFactionRoleDeleteCommand(plugin)
    private val factionRoleRenameCommand = MfFactionRoleRenameCommand(plugin)
    private val factionRoleSetDefaultCommand = MfFactionRoleSetDefaultCommand(plugin)

    private val viewAliases = listOf("view", plugin.language["CmdFactionRoleView"])
    private val setPermissionAliases = listOf("setpermission", plugin.language["CmdFactionRoleSetPermission"])
    private val listAliases = listOf("list", plugin.language["CmdFactionRoleList"])
    private val setAliases = listOf("set", plugin.language["CmdFactionRoleSet"])
    private val createAliases = listOf("create", "add", plugin.language["CmdFactionRoleCreate"])
    private val deleteAliases = listOf("delete", "remove", plugin.language["CmdFactionRoleDelete"])
    private val renameAliases = listOf("rename", "setname", plugin.language["CmdFactionRoleRename"])
    private val setDefaultAliases = listOf("setdefault", plugin.language["CmdFactionRoleSetDefault"])

    private val subcommands = viewAliases +
        setPermissionAliases +
        listAliases +
        setAliases +
        createAliases +
        deleteAliases +
        renameAliases +
        setDefaultAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val first = args.firstOrNull()
        // A leading argument that is not a subcommand names another faction: /f role <faction> <subcommand> ...
        // A quoted leading argument is always a faction, so a faction named like a subcommand can still be given.
        if (first != null && !first.startsWith("\"") && first.lowercase() in subcommands) {
            return dispatch(sender, command, label, args, null)
        }
        if (first == null || !sender.hasPermission("mf.force.role")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleUsage"]}")
            return true
        }
        val (factionName, remainingArgs) = args.splitLeadingArg() ?: run {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleUsage"]}")
            return true
        }
        // With mf.force.role, the named faction replaces the sender's own, and the force permission replaces that
        // faction's role-permission checks, matching how the other mf.force.* nodes are honoured. A name that does not
        // resolve is refused rather than falling back to the sender's own faction.
        val factionService = plugin.services.factionService
        val faction = factionService.getFaction(MfFactionId(factionName)) ?: factionService.getFaction(factionName)
        if (faction == null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleInvalidFaction", factionName]}")
            return true
        }
        if (remainingArgs.firstOrNull()?.lowercase() !in subcommands) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleUsage"]}")
            return true
        }
        return dispatch(sender, command, label, remainingArgs, faction.id)
    }

    private fun dispatch(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
        forcedFactionId: MfFactionId?
    ): Boolean {
        val subcommandArgs = args.drop(1).toTypedArray()
        return when (args.first().lowercase()) {
            in viewAliases -> factionRoleViewCommand.execute(sender, subcommandArgs, forcedFactionId)
            in setPermissionAliases -> factionRoleSetPermissionCommand.execute(sender, subcommandArgs, forcedFactionId)
            in listAliases -> factionRoleListCommand.execute(sender, subcommandArgs, forcedFactionId)
            in setAliases -> factionRoleSetCommand.execute(sender, subcommandArgs, forcedFactionId)
            in createAliases -> factionRoleCreateCommand.execute(sender, subcommandArgs, forcedFactionId)
            in deleteAliases -> factionRoleDeleteCommand.execute(sender, subcommandArgs, forcedFactionId)
            in renameAliases -> factionRoleRenameCommand.execute(sender, subcommandArgs, forcedFactionId)
            in setDefaultAliases -> factionRoleSetDefaultCommand.execute(sender, subcommandArgs, forcedFactionId)
            else -> {
                sender.sendMessage("$RED${plugin.language["CommandFactionRoleUsage"]}")
                true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val canForce = sender.hasPermission("mf.force.role")
        return when {
            args.isEmpty() -> subcommands
            args.size == 1 -> {
                val factionNames = if (canForce) {
                    plugin.services.factionService.factions.map { it.name }.filter { !it.contains(' ') }
                } else {
                    emptyList()
                }
                (subcommands + factionNames).filter { it.lowercase().startsWith(args[0].lowercase()) }
            }
            args.first().lowercase() !in subcommands -> {
                // /f role <faction> <subcommand>: only the subcommand position is completed for another faction
                if (canForce && args.size == 2) subcommands.filter { it.startsWith(args[1].lowercase()) } else emptyList()
            }
            else -> when (args.first().lowercase()) {
                in viewAliases -> factionRoleViewCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
                in setPermissionAliases -> factionRoleSetPermissionCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
                in listAliases -> factionRoleListCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
                in setAliases -> factionRoleSetCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
                in createAliases -> factionRoleCreateCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
                in deleteAliases -> factionRoleDeleteCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
                in renameAliases -> factionRoleRenameCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
                in setDefaultAliases -> factionRoleSetDefaultCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
                else -> emptyList()
            }
        }
    }
}

/**
 * The command prefix used for clickable buttons and return-to-page commands, so that an admin managing another faction
 * with mf.force.role stays on that faction. The faction is given by ID, which cannot be confused with a subcommand.
 */
internal fun roleCommandPrefix(forcedFactionId: MfFactionId?) =
    if (forcedFactionId == null) "faction role" else "faction role ${forcedFactionId.value}"
