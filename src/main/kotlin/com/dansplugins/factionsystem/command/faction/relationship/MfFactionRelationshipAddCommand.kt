package com.dansplugins.factionsystem.command.faction.relationship

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
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
import preponderous.ponder.command.unquote
import java.util.logging.Level.SEVERE
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionRelationshipAddCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.relationship.add")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddNoPermission"]}")
            return true
        }
        val unquotedArgs = args.unquote()
        if (unquotedArgs.size < 2) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddUsage"]}")
            return true
        }
        if (unquotedArgs.size < 3) {
            sender.sendMessage("${BukkitChatColor.AQUA}${plugin.language["CommandFactionRelationshipAddRelationshipTypeTitle"]}")
            MfFactionRelationshipType.values().forEach { relationshipType ->
                sender.spigot().sendMessage(
                    TextComponent(relationshipType.toTranslation()).apply {
                        color = SpigotChatColor.GRAY
                        hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionRelationshipAddTypeHover", relationshipType.toTranslation()]))
                        clickEvent = ClickEvent(RUN_COMMAND, "/faction relationship add ${args.joinToString(" ")} ${relationshipType.name}")
                    }
                )
            }
        } else {
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    val factionService = plugin.services.factionService
                    val faction1 = factionService.getFaction(MfFactionId(unquotedArgs[0])) ?: factionService.getFaction(
                        unquotedArgs[0]
                    )
                    if (faction1 == null) {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddInvalidFaction", unquotedArgs[0]]}")
                        return@Runnable
                    }
                    val faction2 = factionService.getFaction(MfFactionId(unquotedArgs[1])) ?: factionService.getFaction(
                        unquotedArgs[1]
                    )
                    if (faction2 == null) {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddInvalidFaction", unquotedArgs[1]]}")
                        return@Runnable
                    }
                    if (faction1.id == faction2.id) {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddCannotAddRelationshipToSelf"]}")
                        return@Runnable
                    }
                    val relationshipType = try {
                        MfFactionRelationshipType.valueOf(unquotedArgs[2].uppercase().replace(' ', '_'))
                    } catch (exception: IllegalArgumentException) {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddInvalidRelationshipType"]}")
                        return@Runnable
                    }
                    val relationshipService = plugin.services.factionRelationshipService
                    val relationships = relationshipService.getRelationships(faction1.id, faction2.id)
                    val reverseRelationships = relationshipService.getRelationships(faction2.id, faction1.id)
                    if (relationships.any { it.type == relationshipType }) {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddRelationshipExists"]}")
                        return@Runnable
                    }
                    if (relationshipType == ALLY) {
                        if ((relationships + reverseRelationships).any { it.type == AT_WAR }) {
                            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddCannotAllyWhileAtWar"]}")
                            return@Runnable
                        }
                    }
                    if (relationshipType == VASSAL) {
                        if (relationshipService.getVassalTree(faction2.id).contains(faction1.id)) {
                            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddWouldCreateCycle"]}")
                            return@Runnable
                        }
                    }
                    if (relationshipType == LIEGE) {
                        if (relationshipService.getLiegeChain(faction2.id).contains(faction1.id)) {
                            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddWouldCreateCycle"]}")
                            return@Runnable
                        }
                    }
                    if (relationshipType == AT_WAR) {
                        if (relationships.any { it.type == ALLY } && reverseRelationships.any { it.type == ALLY }) {
                            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddCannotAddWarWithAlly"]}")
                            return@Runnable
                        }
                    }
                    relationshipService.save(
                        MfFactionRelationship(
                            factionId = faction1.id,
                            targetId = faction2.id,
                            type = relationshipType
                        )
                    ).onFailure {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipAddFailedToSaveRelationship"]}")
                        plugin.logger.log(SEVERE, "Failed to save faction relationship: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                    sender.sendMessage("${BukkitChatColor.GREEN}${plugin.language["CommandFactionRelationshipAddSuccess"]}")
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            plugin.server.dispatchCommand(sender, "faction relationship view ${faction1.id.value} ${faction2.id.value}")
                        }
                    )
                }
            )
        }
        return true
    }

    private fun MfFactionRelationshipType.toTranslation() = when (this) {
        ALLY -> plugin.language["Ally"]
        AT_WAR -> plugin.language["AtWar"]
        VASSAL -> plugin.language["Vassal"]
        LIEGE -> plugin.language["Liege"]
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val factionService = plugin.services.factionService
        return when {
            args.isEmpty() -> factionService.factions.map(MfFaction::name)
            args.size == 1 ->
                factionService.factions
                    .filter { it.name.lowercase().startsWith(args[0].lowercase()) }
                    .map(MfFaction::name)
            args.size == 2 ->
                factionService.factions
                    .filter { it.name.lowercase().startsWith(args[1].lowercase()) }
                    .map(MfFaction::name)
            args.size == 3 -> MfFactionRelationshipType.values()
                .map { it.name.lowercase() }
                .filter { it.startsWith(args[2].lowercase()) }
            else -> emptyList()
        }
    }
}
