package com.dansplugins.factionsystem.command.faction.dpc

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.GOLD
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.YELLOW
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

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
            "register" -> handleRegister(sender, args)
            "login" -> handleLogin(sender, args)
            "profile" -> handleProfile(sender)
            "generatekey" -> handleGenerateKey(sender, args)
            "deletekey" -> handleDeleteKey(sender, args)
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

    private fun handleRegister(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcPlayerOnly"]}")
            return
        }
        if (args.size < 3) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcRegisterUsage"]}")
            return
        }
        val username = args[1]
        val password = args[2]
        sender.sendMessage("$GRAY${plugin.language["CommandFactionDpcRegisterPending"]}")
        plugin.dpcApiService.register(
            sender.uniqueId,
            username,
            password,
            onSuccess = { returnedUsername ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcRegisterSuccess", returnedUsername]}")
                })
            },
            onFailure = { error ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDpcRegisterFail", error]}")
                })
            }
        )
    }

    private fun handleLogin(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcPlayerOnly"]}")
            return
        }
        if (args.size < 3) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcLoginUsage"]}")
            return
        }
        val username = args[1]
        val password = args[2]
        sender.sendMessage("$GRAY${plugin.language["CommandFactionDpcLoginPending"]}")
        plugin.dpcApiService.login(
            sender.uniqueId,
            username,
            password,
            onSuccess = { returnedUsername ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcLoginSuccess", returnedUsername]}")
                })
            },
            onFailure = { error ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDpcLoginFail", error]}")
                })
            }
        )
    }

    private fun handleProfile(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcPlayerOnly"]}")
            return
        }
        sender.sendMessage("$GRAY${plugin.language["CommandFactionDpcProfilePending"]}")
        plugin.dpcApiService.getProfile(
            sender.uniqueId,
            onSuccess = { json ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    val username = json.get("username")?.asString ?: "Unknown"
                    sender.sendMessage("$GOLD${plugin.language["CommandFactionDpcProfileHeader"]}")
                    sender.sendMessage("$YELLOW${plugin.language["CommandFactionDpcProfileUsername", username]}")
                    val apiKeys = json.getAsJsonArray("apiKeys")
                    if (apiKeys == null || apiKeys.size() == 0) {
                        sender.sendMessage("$GRAY${plugin.language["CommandFactionDpcProfileNoKeys"]}")
                    } else {
                        sender.sendMessage("$YELLOW${plugin.language["CommandFactionDpcProfileKeysHeader", apiKeys.size().toString()]}")
                        for (keyElement in apiKeys) {
                            val keyObj = keyElement.asJsonObject
                            val keyId = keyObj.get("id")?.asString ?: "?"
                            val serverName = keyObj.get("serverName")?.asString ?: "?"
                            sender.sendMessage("$GRAY  - $serverName ($keyId)")
                        }
                    }
                })
            },
            onFailure = { error ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (error == "NotLoggedIn" || error == "SessionExpired") {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDpcNotLoggedIn"]}")
                    } else {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDpcProfileFail", error]}")
                    }
                })
            }
        )
    }

    private fun handleGenerateKey(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcPlayerOnly"]}")
            return
        }
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcGenerateKeyUsage"]}")
            return
        }
        val serverName = args.drop(1).joinToString(" ")
        sender.sendMessage("$GRAY${plugin.language["CommandFactionDpcGenerateKeyPending"]}")
        plugin.dpcApiService.createApiKey(
            sender.uniqueId,
            serverName,
            onSuccess = { apiKey ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcGenerateKeySuccess"]}")
                    sender.sendMessage("$YELLOW${plugin.language["CommandFactionDpcGenerateKeyValue", apiKey]}")
                    sender.sendMessage("$RED${plugin.language["CommandFactionDpcGenerateKeyWarning"]}")
                })
            },
            onFailure = { error ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (error == "NotLoggedIn" || error == "SessionExpired") {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDpcNotLoggedIn"]}")
                    } else {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDpcGenerateKeyFail", error]}")
                    }
                })
            }
        )
    }

    private fun handleDeleteKey(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcPlayerOnly"]}")
            return
        }
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDpcDeleteKeyUsage"]}")
            return
        }
        val keyId = args[1]
        sender.sendMessage("$GRAY${plugin.language["CommandFactionDpcDeleteKeyPending"]}")
        plugin.dpcApiService.deleteApiKey(
            sender.uniqueId,
            keyId,
            onSuccess = {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionDpcDeleteKeySuccess"]}")
                })
            },
            onFailure = { error ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (error == "NotLoggedIn" || error == "SessionExpired") {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDpcNotLoggedIn"]}")
                    } else if (error == "NotFound") {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDpcDeleteKeyNotFound"]}")
                    } else {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDpcDeleteKeyFail", error]}")
                    }
                })
            }
        )
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
                val subcommands = listOf(
                    "optin", "optout", "reminder", "shareip", "discord",
                    "register", "login", "profile", "generatekey", "deletekey"
                )
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
