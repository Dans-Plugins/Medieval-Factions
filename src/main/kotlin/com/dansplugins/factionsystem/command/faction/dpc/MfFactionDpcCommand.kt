package com.dansplugins.factionsystem.command.faction.dpc

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionDpcCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.dpc")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcUsage"]}")
            return true
        }
        when (args[0].lowercase()) {
            "optin" -> handleOptIn(sender)
            "optout" -> handleOptOut(sender)
            "reminder" -> handleReminder(sender, args)
            "shareip" -> handleShareIp(sender, args)
            "discord" -> handleDiscord(sender, args)
            else -> sender.sendMessage("$RED${plugin.language["CommandFactionDpcUsage"]}")
        }
        return true
    }

    private fun handleOptIn(sender: CommandSender) {
        plugin.config.set("dpc-api.enabled", true)
        plugin.saveConfig()
        sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcOptInSuccess"]}")
    }

    private fun handleOptOut(sender: CommandSender) {
        plugin.config.set("dpc-api.enabled", false)
        plugin.saveConfig()
        sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcOptOutSuccess"]}")
    }

    private fun handleReminder(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcReminderUsage"]}")
            return
        }
        when (args[1].lowercase()) {
            "on" -> {
                plugin.config.set("dpc-api.login-reminder", true)
                plugin.saveConfig()
                sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcReminderOnSuccess"]}")
            }
            "off" -> {
                plugin.config.set("dpc-api.login-reminder", false)
                plugin.saveConfig()
                sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcReminderOffSuccess"]}")
            }
            else -> sender.sendMessage("$RED${plugin.language["CommandFactionDpcReminderUsage"]}")
        }
    }

    private fun handleShareIp(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcShareIpUsage"]}")
            return
        }
        when (args[1].lowercase()) {
            "on" -> {
                plugin.config.set("dpc-api.share-server-ip", true)
                plugin.saveConfig()
                sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcShareIpOnSuccess"]}")
            }
            "off" -> {
                plugin.config.set("dpc-api.share-server-ip", false)
                plugin.saveConfig()
                sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcShareIpOffSuccess"]}")
            }
            else -> sender.sendMessage("$RED${plugin.language["CommandFactionDpcShareIpUsage"]}")
        }
    }

    private fun handleDiscord(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcDiscordUsage"]}")
            return
        }
        when (args[1].lowercase()) {
            "clear" -> {
                plugin.config.set("dpc-api.discord-link", "")
                plugin.saveConfig()
                sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcDiscordClearSuccess"]}")
            }
            else -> {
                val link = args[1]
                plugin.config.set("dpc-api.discord-link", link)
                plugin.saveConfig()
                sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcDiscordSetSuccess", link]}")
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("mf.dpc")) return emptyList()
        return when {
            args.size <= 1 -> {
                val subcommands = listOf("optin", "optout", "reminder", "shareip", "discord")
                if (args.isEmpty()) subcommands else subcommands.filter { it.startsWith(args[0].lowercase()) }
            }
            args.size == 2 -> when (args[0].lowercase()) {
                "reminder" -> listOf("on", "off").filter { it.startsWith(args[1].lowercase()) }
                "shareip" -> listOf("on", "off").filter { it.startsWith(args[1].lowercase()) }
                "discord" -> listOf("clear").filter { it.startsWith(args[1].lowercase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
