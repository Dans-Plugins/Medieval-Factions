package com.dansplugins.factionsystem.command.faction.admin

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.YELLOW
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionAdminCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val adminCreateCommand = MfFactionAdminCreateCommand(plugin)
    
    private val createAliases = listOf("create", plugin.language["CmdFactionAdminCreate"])
    
    private val subcommands = createAliases

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.admin")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAdminNoPermission"]}")
            return true
        }
        
        return when (args.firstOrNull()?.lowercase()) {
            in createAliases -> adminCreateCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("$YELLOW${plugin.language["CommandFactionAdminUsage"]}")
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
            in createAliases -> adminCreateCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
            else -> emptyList()
        }
    }
}
