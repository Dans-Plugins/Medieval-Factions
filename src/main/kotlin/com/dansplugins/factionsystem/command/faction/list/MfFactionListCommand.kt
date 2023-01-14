package com.dansplugins.factionsystem.command.faction.list

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.pagination.PaginatedView
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.floor
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionListCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols.getInstance(plugin.language.locale))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.list")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionListNoPermission"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val factionService = plugin.services.factionService
                val claimService = plugin.services.claimService
                val pageNumber = args.lastOrNull()?.toIntOrNull()?.minus(1) ?: 0
                val view = PaginatedView(
                    plugin.language,
                    lazy {
                        arrayOf(
                            TextComponent(plugin.language["CommandFactionListTitle"]).apply {
                                color = SpigotChatColor.AQUA
                                isBold = true
                            }
                        )
                    },
                    factionService.factions
                        .sortedByDescending { it.power }
                        .flatMap { faction ->
                            listOf(
                                lazy {
                                    arrayOf(
                                        TextComponent(
                                            plugin.language[
                                                "CommandFactionListItem",
                                                faction.name
                                            ]
                                        ).apply {
                                            color = SpigotChatColor.AQUA
                                        }
                                    )
                                },
                                lazy {
                                    arrayOf(
                                        TextComponent(
                                            "  " + plugin.language[
                                                "CommandFactionListPower",
                                                decimalFormat.format(floor(faction.power))
                                            ]
                                        ).apply {
                                            color = SpigotChatColor.GRAY
                                        }
                                    )
                                },
                                lazy {
                                    arrayOf(
                                        TextComponent(
                                            "  " + plugin.language[
                                                "CommandFactionListMembers",
                                                faction.members.size.toString()
                                            ]
                                        ).apply {
                                            color = SpigotChatColor.GRAY
                                        }
                                    )
                                },
                                lazy {
                                    arrayOf(
                                        TextComponent(
                                            "  " + plugin.language[
                                                "CommandFactionListLand",
                                                claimService.getClaims(faction.id).size.toString()
                                            ]
                                        ).apply {
                                            color = SpigotChatColor.GRAY
                                        }
                                    )
                                }
                            )
                        },
                    // Each faction is currently 4 lines so this should be a multiple of 4.
                    // If we change the amount of lines then this should change.
                    pageLength = 16
                ) { page -> "/faction list ${page + 1}" }
                if (view.pages.isEmpty()) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionListNoFactions"]}")
                    return@Runnable
                }
                if (pageNumber !in view.pages.indices) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionListInvalidPageNumber"]}")
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
