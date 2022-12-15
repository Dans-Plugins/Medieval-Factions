package com.dansplugins.factionsystem.command.faction.chat

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.pagination.PaginatedView
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.SHORT
import java.util.logging.Level
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionChatHistoryCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.chat.history")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionChatHistoryNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionChatHistoryNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionChatHistoryFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionChatHistoryMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.chatHistory)) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionChatHistoryNoFactionPermission"]}")
                    return@Runnable
                }
                val chatService = plugin.services.chatService
                val pageNumber = args.lastOrNull()?.toIntOrNull()?.minus(1) ?: 0
                val pageLength = 10
                val messageCount = chatService.getChatChannelMessageCount(faction.id)
                val pages = (0..(messageCount / pageLength)).map { offsetPage ->
                    lazy {
                        chatService.getChatChannelMessages(faction.id, pageLength, offsetPage * pageLength)
                    }
                }
                val view = PaginatedView(
                    plugin.language,
                    lazy {
                        arrayOf(
                            TextComponent(plugin.language["CommandFactionChatHistoryTitle"]).apply {
                                color = SpigotChatColor.AQUA
                                isBold = true
                            }
                        )
                    },
                    (0..messageCount).map { messageIndex ->
                        lazy {
                            val pageIndex = messageIndex / pageLength
                            val page = if (pageIndex in pages.indices) {
                                pages[pageIndex].value
                            } else {
                                null
                            }
                            val messageInPageIndex = messageIndex - (pageIndex * pageLength)
                            val message = if (page != null && messageInPageIndex in page.indices) {
                                page[messageInPageIndex]
                            } else {
                                null
                            }
                            if (message != null) {
                                val messagePlayer = playerService.getPlayer(message.playerId)
                                val messageFaction = factionService.getFaction(message.factionId)
                                return@lazy arrayOf(
                                    TextComponent(
                                        plugin.language[
                                            "CommandFactionChatHistoryMessage",
                                            DateTimeFormatter.ofLocalizedDateTime(SHORT).format(LocalDateTime.ofInstant(message.timestamp, ZoneOffset.systemDefault())),
                                            messagePlayer?.toBukkit()?.name ?: plugin.language["UnknownPlayer"],
                                            messageFaction?.name ?: plugin.language["UnknownFaction"],
                                            message.chatChannel.toString().lowercase(),
                                            message.message
                                        ]
                                    ).apply {
                                        color = SpigotChatColor.WHITE
                                    }
                                )
                            } else {
                                return@lazy arrayOf()
                            }
                        }
                    }
                ) { page -> "/faction chat history ${page + 1}" }
                if (pageNumber !in view.pages.indices) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionChatHistoryInvalidPageNumber"]}")
                    return@Runnable
                }
                view.sendPage(sender, pageNumber)
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}
