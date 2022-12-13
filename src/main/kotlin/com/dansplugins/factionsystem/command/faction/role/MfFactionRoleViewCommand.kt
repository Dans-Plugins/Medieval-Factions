package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.pagination.PaginatedView
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
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
import java.util.logging.Level
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionRoleViewCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.role.view")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRoleViewNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRoleViewNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRoleViewUsage"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRoleViewFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRoleViewMustBeInAFaction"]}")
                    return@Runnable
                }
                var pageNumber = args.last().toIntOrNull()?.minus(1)
                var pageSpecified = true
                if (pageNumber == null) {
                    pageNumber = 0
                    pageSpecified = false
                }
                val targetRole = faction.getRole((if (pageSpecified) args.dropLast(1) else args.toList()).joinToString(" "))
                if (targetRole == null) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRoleViewInvalidTargetRole"]}")
                    return@Runnable
                }
                val playerRole = faction.getRole(mfPlayer.id)
                if (playerRole == null || !playerRole.hasPermission(faction, plugin.factionPermissions.viewRole(targetRole.id))) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRoleViewNoFactionPermission"]}")
                    return@Runnable
                }
                val view = PaginatedView(
                    plugin.language,
                    lazy {
                        arrayOf(
                            TextComponent(plugin.language["CommandFactionRoleViewTitle", targetRole.name]).apply {
                                color = SpigotChatColor.AQUA
                                isBold = true
                            }
                        )
                    },
                    plugin.factionPermissions.permissionsFor(faction).map { permission ->
                        lazy {
                            val permissionValue = targetRole.getPermissionValue(permission)
                            if (playerRole.hasPermission(faction, plugin.factionPermissions.modifyRole(targetRole.id)) &&
                                playerRole.hasPermission(faction, plugin.factionPermissions.setRolePermission(permission))
                            ) {
                                arrayOf(
                                    TextComponent(
                                        plugin.language[
                                            "CommandFactionRoleViewPermission",
                                            permission.translate(faction)
                                        ]
                                    ).apply {
                                        color = SpigotChatColor.AQUA
                                    },
                                    TextComponent(" - ").apply { color = SpigotChatColor.GRAY },
                                    TextComponent(
                                        plugin.language["CommandFactionRoleViewAllow"].bracketIf { permissionValue == true }
                                    ).apply {
                                        color = if (permissionValue == true) SpigotChatColor.GREEN else SpigotChatColor.DARK_GREEN
                                        isBold = permissionValue == true
                                        clickEvent = ClickEvent(RUN_COMMAND, "/faction role setpermission ${targetRole.id.value} ${permission.name} allow p=${pageNumber + 1}")
                                        hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionRoleViewAllowHover", targetRole.name, permission.name]))
                                    },
                                    TextComponent(" / ").apply { color = SpigotChatColor.GRAY },
                                    TextComponent(
                                        plugin.language["CommandFactionRoleViewDeny"].bracketIf { permissionValue == false }
                                    ).apply {
                                        color = if (permissionValue == false) SpigotChatColor.RED else SpigotChatColor.DARK_RED
                                        isBold = permissionValue == false
                                        clickEvent = ClickEvent(RUN_COMMAND, "/faction role setpermission ${targetRole.id.value} ${permission.name} deny p=${pageNumber + 1}")
                                        hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionRoleViewDenyHover", targetRole.name, permission.name]))
                                    },
                                    TextComponent(" / ").apply { color = SpigotChatColor.GRAY },
                                    TextComponent(
                                        plugin.language["CommandFactionRoleViewDefault"].bracketIf { permissionValue == null }
                                    ).apply {
                                        color = if (permissionValue == null) SpigotChatColor.GRAY else SpigotChatColor.DARK_GRAY
                                        isBold = permissionValue == null
                                        clickEvent = ClickEvent(RUN_COMMAND, "/faction role setpermission ${targetRole.id.value} ${permission.name} default p=${pageNumber + 1}")
                                        hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionRoleViewDefaultHover", targetRole.name, permission.name]))
                                    }
                                )
                            } else {
                                arrayOf(
                                    TextComponent(
                                        plugin.language[
                                            "CommandFactionRoleViewPermission",
                                            permission.translate(faction)
                                        ]
                                    ).apply {
                                        color = SpigotChatColor.AQUA
                                    },
                                    TextComponent(" - ").apply { color = SpigotChatColor.GRAY },
                                    when (permissionValue) {
                                        true -> TextComponent(plugin.language["CommandFactionRoleViewAllow"].bracket()).apply {
                                            color = SpigotChatColor.GREEN
                                            hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionRoleViewAllowHoverNoPermission"]))
                                        }
                                        false -> TextComponent(plugin.language["CommandFactionRoleViewDeny"].bracket()).apply {
                                            color = SpigotChatColor.RED
                                            hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionRoleViewDenyHoverNoPermission"]))
                                        }
                                        null -> TextComponent(plugin.language["CommandFactionRoleViewDefault"].bracket()).apply {
                                            color = SpigotChatColor.GRAY
                                            hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionRoleViewDefaultHoverNoPermission"]))
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { page -> "/faction role view ${targetRole.id.value} ${page + 1}" }
                if (pageNumber !in view.pages.indices) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRoleViewInvalidPageNumber"]}")
                    return@Runnable
                }
                view.sendPage(sender, pageNumber)
            }
        )
        return true
    }

    private fun String.bracket(openingBracket: String = "[", closingBracket: String = "]") = "$openingBracket$this$closingBracket"
    private fun String.bracketIf(openingBracket: String = "[", closingBracket: String = "]", condition: () -> Boolean) =
        if (condition()) bracket(openingBracket, closingBracket) else this

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player) return emptyList()
        val playerId = MfPlayerId.fromBukkitPlayer(sender)
        val factionService = plugin.services.factionService
        val faction = factionService.getFaction(playerId) ?: return emptyList()
        return when {
            args.isEmpty() -> faction.roles.map { it.name }
            args.size == 1 -> faction.roles.filter { it.name.lowercase().startsWith(args[0].lowercase()) }.map { it.name }
            else -> emptyList()
        }
    }
}
