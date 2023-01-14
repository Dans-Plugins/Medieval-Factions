package com.dansplugins.factionsystem.command.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.pagination.PaginatedView
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionFlagListCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.flag.list")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.viewFlags)) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListNoFactionPermission"]}")
                    return@Runnable
                }
                val pageNumber = args.lastOrNull()?.toIntOrNull()?.minus(1) ?: 0
                val view = PaginatedView(
                    plugin.language,
                    lazy {
                        arrayOf(
                            TextComponent(plugin.language["CommandFactionFlagListTitle", faction.name]).apply {
                                color = SpigotChatColor.AQUA
                                isBold = true
                            }
                        )
                    },
                    plugin.flags.map { flag ->
                        lazy {
                            val flagValue = faction.flags[flag]
                            buildList {
                                add(
                                    TextComponent(flag.name).apply {
                                        color = SpigotChatColor.GRAY
                                    }
                                )
                                add(
                                    TextComponent(" (${flag.type.simpleName}): ").apply {
                                        color = SpigotChatColor.GRAY
                                    }
                                )
                                add(
                                    TextComponent("$flagValue ").apply {
                                        color = SpigotChatColor.WHITE
                                    }
                                )
                                if (sender.hasPermission("mf.flag.set") && role.hasPermission(faction, plugin.factionPermissions.setFlag(flag))) {
                                    add(
                                        TextComponent("(${plugin.language["CommandFactionFlagListSet"]})").apply {
                                            color = SpigotChatColor.GREEN
                                            hoverEvent = HoverEvent(
                                                SHOW_TEXT,
                                                Text(plugin.language["CommandFactionFlagListSetHover", flag.name])
                                            )
                                            clickEvent =
                                                ClickEvent(RUN_COMMAND, "/faction flag set ${flag.name} p=${pageNumber + 1}")
                                        }
                                    )
                                }
                            }.toTypedArray()
                        }
                    }
                ) { page -> "/faction flag list ${page + 1}" }
                if (pageNumber !in view.pages.indices) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListInvalidPageNumber"]}")
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
