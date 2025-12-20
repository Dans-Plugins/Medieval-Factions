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
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable { executeCommand(sender, args) })
        return true
    }

    private fun executeCommand(sender: Player, args: Array<out String>) {
        val mfPlayer = getOrSavePlayer(sender) ?: return
        val factionService = plugin.services.factionService
        val hasForcePermission = sender.hasPermission("mf.force.flag")
        
        val (targetFaction, pageNumber) = parseArguments(args, hasForcePermission, factionService)
        val faction = resolveFaction(sender, mfPlayer, targetFaction, factionService) ?: return
        val role = checkPermissions(sender, mfPlayer, faction, targetFaction) ?: return
        
        displayFactionFlags(sender, faction, targetFaction, role, pageNumber)
    }

    private fun getOrSavePlayer(sender: Player): MfPlayer? {
        val playerService = plugin.services.playerService
        return playerService.getPlayer(sender) ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListFailedToSavePlayer"]}")
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return null
        }
    }

    private data class ParsedArguments(val targetFaction: MfFaction?, val pageNumber: Int)

    private fun parseArguments(
        args: Array<out String>,
        hasForcePermission: Boolean,
        factionService: com.dansplugins.factionsystem.faction.MfFactionService
    ): ParsedArguments {
        val unquotedArgs = args.unquote()
        var targetFaction: MfFaction? = null
        var pageNumber = 0

        if (hasForcePermission && unquotedArgs.isNotEmpty()) {
            val potentialFaction = factionService.getFaction(MfFactionId(unquotedArgs[0])) 
                ?: factionService.getFaction(unquotedArgs[0])

            if (potentialFaction != null) {
                targetFaction = potentialFaction
                pageNumber = unquotedArgs.getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0
            } else {
                pageNumber = unquotedArgs[0].toIntOrNull()?.minus(1) ?: 0
            }
        } else {
            pageNumber = args.lastOrNull()?.toIntOrNull()?.minus(1) ?: 0
        }

        return ParsedArguments(targetFaction, pageNumber)
    }

    private fun resolveFaction(
        sender: Player,
        mfPlayer: MfPlayer,
        targetFaction: MfFaction?,
        factionService: com.dansplugins.factionsystem.faction.MfFactionService
    ): MfFaction? {
        return if (targetFaction != null) {
            targetFaction
        } else {
            factionService.getFaction(mfPlayer.id) ?: run {
                sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListMustBeInAFaction"]}")
                null
            }
        }
    }

    private fun checkPermissions(
        sender: Player,
        mfPlayer: MfPlayer,
        faction: MfFaction,
        targetFaction: MfFaction?
    ): com.dansplugins.factionsystem.faction.role.MfFactionRole? {
        return if (targetFaction != null) {
            if (!sender.hasPermission("mf.force.flag")) {
                sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListNoPermission"]}")
                return null
            }
            null
        } else {
            val playerRole = faction.getRole(mfPlayer.id)
            if (playerRole == null || !playerRole.hasPermission(faction, plugin.factionPermissions.viewFlags)) {
                sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListNoFactionPermission"]}")
                return null
            }
            playerRole
        }
    }

    private fun displayFactionFlags(
        sender: Player,
        faction: MfFaction,
        targetFaction: MfFaction?,
        role: com.dansplugins.factionsystem.faction.role.MfFactionRole?,
        pageNumber: Int
    ) {
        val factionNameParam = if (targetFaction != null) " \"${faction.name}\"" else ""
        val view = createPaginatedView(sender, faction, targetFaction, role, factionNameParam, pageNumber)
        
        if (pageNumber !in view.pages.indices) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionFlagListInvalidPageNumber"]}")
            return
        }
        view.sendPage(sender, pageNumber)
    }

    private fun createPaginatedView(
        sender: Player,
        faction: MfFaction,
        targetFaction: MfFaction?,
        role: com.dansplugins.factionsystem.faction.role.MfFactionRole?,
        factionNameParam: String,
        pageNumber: Int
    ): PaginatedView {
        return PaginatedView(
            plugin.language,
            lazy {
                arrayOf(
                    TextComponent(plugin.language["CommandFactionFlagListTitle", faction.name]).apply {
                        color = SpigotChatColor.AQUA
                        isBold = true
                    }
                )
            },
            plugin.flags.map { flag -> lazy { createFlagRow(sender, faction, targetFaction, role, flag, factionNameParam, pageNumber) } }
        ) { page -> "/faction flag list$factionNameParam ${page + 1}" }
    }

    private fun createFlagRow(
        sender: Player,
        faction: MfFaction,
        targetFaction: MfFaction?,
        role: com.dansplugins.factionsystem.faction.role.MfFactionRole?,
        flag: com.dansplugins.factionsystem.faction.flag.MfFlag<out Any>,
        factionNameParam: String,
        pageNumber: Int
    ): Array<TextComponent> {
        val flagValue = faction.flags[flag]
        return buildList {
            add(TextComponent(flag.name).apply { color = SpigotChatColor.GRAY })
            add(TextComponent(" (${flag.type.simpleName}): ").apply { color = SpigotChatColor.GRAY })
            add(TextComponent("$flagValue ").apply { color = SpigotChatColor.WHITE })
            
            val canSetFlag = canUserSetFlag(sender, faction, targetFaction, role, flag)
            if (canSetFlag) {
                add(createSetButton(flag, factionNameParam, pageNumber))
            }
        }.toTypedArray()
    }

    private fun canUserSetFlag(
        sender: Player,
        faction: MfFaction,
        targetFaction: MfFaction?,
        role: com.dansplugins.factionsystem.faction.role.MfFactionRole?,
        flag: com.dansplugins.factionsystem.faction.flag.MfFlag<out Any>
    ): Boolean {
        return if (targetFaction != null) {
            sender.hasPermission("mf.flag.set") && sender.hasPermission("mf.force.flag")
        } else {
            sender.hasPermission("mf.flag.set") && role != null && role.hasPermission(faction, plugin.factionPermissions.setFlag(flag))
        }
    }

    private fun createSetButton(
        flag: com.dansplugins.factionsystem.faction.flag.MfFlag<out Any>,
        factionNameParam: String,
        pageNumber: Int
    ): TextComponent {
        return TextComponent("(${plugin.language["CommandFactionFlagListSet"]})").apply {
            color = SpigotChatColor.GREEN
            hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionFlagListSetHover", flag.name]))
            clickEvent = ClickEvent(RUN_COMMAND, "/faction flag set$factionNameParam ${flag.name} p=${pageNumber + 1}")
        }
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
