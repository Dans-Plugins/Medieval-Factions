package com.dansplugins.factionsystem.command.faction.chat

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfFactionChatChannel
import com.dansplugins.factionsystem.chat.MfFactionChatChannel.FACTION
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionChatCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val chatHistoryCommand = MfFactionChatHistoryCommand(plugin)

    private val historyAliases = listOf("history", "logs", "log", plugin.language["CmdFactionChatHistory"])

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.chat")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionChatNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionChatNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender) ?: MfPlayer(plugin, sender)
                val chatChannel = if (args.isEmpty()) {
                    FACTION
                } else {
                    try {
                        MfFactionChatChannel.valueOf(args[0].uppercase())
                    } catch (exception: IllegalArgumentException) {
                        null
                    }
                }
                if (chatChannel == null) {
                    if (args[0].lowercase() in historyAliases) {
                        plugin.server.scheduler.runTask(
                            plugin,
                            Runnable {
                                chatHistoryCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
                            }
                        )
                        return@Runnable
                    }
                }
                val updatedMfPlayer = playerService.save(
                    mfPlayer.copy(
                        chatChannel = if (mfPlayer.chatChannel != chatChannel) {
                            chatChannel
                        } else {
                            null
                        }
                    )
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionChatFailedToSavePlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                if (updatedMfPlayer.chatChannel != null) {
                    sender.sendMessage(
                        "$GREEN${
                        plugin.language["CommandFactionChatFactionChatEnabled", chatChannel.toString().lowercase()]
                        }"
                    )
                } else {
                    sender.sendMessage(
                        "$GREEN${
                        plugin.language["CommandFactionChatFactionChatDisabled"]
                        }"
                    )
                }
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = when {
        args.isEmpty() -> MfFactionChatChannel.values().map { it.name.lowercase() } + historyAliases
        args.size == 1 -> (MfFactionChatChannel.values().map { it.name.lowercase() } + historyAliases)
            .filter { it.startsWith(args[0].lowercase()) }
        args.size > 1 && args[0].lowercase() in historyAliases -> chatHistoryCommand.onTabComplete(sender, command, label, args.drop(1).toTypedArray())
        else -> emptyList()
    }
}
