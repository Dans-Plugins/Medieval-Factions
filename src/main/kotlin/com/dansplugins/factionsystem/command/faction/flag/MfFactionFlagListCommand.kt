package com.dansplugins.factionsystem.command.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
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
import preponderous.ponder.command.unquote
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

                // Parse arguments to determine if faction is specified
                val hasForcePermission = sender.hasPermission("mf.force.flag")
                val unquotedArgs = args.unquote()
                var targetFaction: MfFaction? = null
                var pageNumber = 0

                if (hasForcePermission && unquotedArgs.isNotEmpty()) {
                    // Try to parse first argument as faction name/ID
                    val potentialFaction = factionService.getFaction(MfFactionId(unquotedArgs[0])) ?: factionService.getFaction(unquotedArgs[0])

                    if (potentialFaction != null) {
                        // First arg is a valid faction
                        targetFaction = potentialFaction
                        pageNumber = unquotedArgs.getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0
                    } else {
                        // First arg is page number
                        pageNumber = unquotedArgs[0].toIntOrNull()?.minus(1) ?: 0
                    }
                } else {
                    // No force permission or no args, use standard parsing
                    pageNumber = args.lastOrNull()?.toIntOrNull()?.minus(1) ?: 0
                }

                val faction = if (targetFaction != null) {
                    targetFaction
                } else {
                    val playerFaction = factionService.getFaction(mfPlayer.id)
                    if (playerFaction == null) {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListMustBeInAFaction"]}")
                        return@Runnable
                    }
                    playerFaction
                }

                // Check permissions - either force permission or faction role permission
                val role = if (targetFaction != null) {
                    // Viewing another faction - requires force permission
                    if (!sender.hasPermission("mf.force.flag")) {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListNoPermission"]}")
                        return@Runnable
                    }
                    null // No role check needed for force permission
                } else {
                    // Viewing own faction - check role permission
                    val playerRole = faction.getRole(mfPlayer.id)
                    if (playerRole == null || !playerRole.hasPermission(faction, plugin.factionPermissions.viewFlags)) {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListNoFactionPermission"]}")
                        return@Runnable
                    }
                    playerRole
                }

                val factionNameParam = if (targetFaction != null) " \"${faction.name}\"" else ""
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
                                // Show set button if user has permission
                                val canSetFlag = if (targetFaction != null) {
                                    sender.hasPermission("mf.flag.set") && sender.hasPermission("mf.force.flag")
                                } else {
                                    sender.hasPermission("mf.flag.set") && role != null && role.hasPermission(faction, plugin.factionPermissions.setFlag(flag))
                                }
                                if (canSetFlag) {
                                    add(
                                        TextComponent("(${plugin.language["CommandFactionFlagListSet"]})").apply {
                                            color = SpigotChatColor.GREEN
                                            hoverEvent = HoverEvent(
                                                SHOW_TEXT,
                                                Text(plugin.language["CommandFactionFlagListSetHover", flag.name])
                                            )
                                            clickEvent =
                                                ClickEvent(RUN_COMMAND, "/faction flag set$factionNameParam ${flag.name} p=${pageNumber + 1}")
                                        }
                                    )
                                }
                            }.toTypedArray()
                        }
                    }
                ) { page -> "/faction flag list$factionNameParam ${page + 1}" }
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
    ): List<String> {
        val hasForcePermission = sender.hasPermission("mf.force.flag")
        if (!hasForcePermission) {
            return emptyList()
        }

        val factionService = plugin.services.factionService
        return when {
            args.isEmpty() -> factionService.factions.map { it.name }
            args.size == 1 -> factionService.factions.filter { it.name.lowercase().startsWith(args[0].lowercase()) }.map { it.name }
            else -> emptyList()
        }
    }
}
