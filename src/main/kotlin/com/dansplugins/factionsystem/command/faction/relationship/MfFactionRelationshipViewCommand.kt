package com.dansplugins.factionsystem.command.faction.relationship

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
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
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionRelationshipViewCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.relationship.view")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipViewNoPermission"]}")
            return true
        }
        val unquotedArgs = args.unquote()
        if (unquotedArgs.size < 2) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipViewUsage"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val factionService = plugin.services.factionService
                val faction1 = factionService.getFaction(MfFactionId(unquotedArgs[0])) ?: factionService.getFaction(unquotedArgs[0])
                if (faction1 == null) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipViewInvalidFaction", unquotedArgs[0]]}")
                    return@Runnable
                }
                val faction2 = factionService.getFaction(MfFactionId(unquotedArgs[1])) ?: factionService.getFaction(unquotedArgs[1])
                if (faction2 == null) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipViewInvalidFaction", unquotedArgs[1]]}")
                    return@Runnable
                }
                if (faction1.id == faction2.id) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionRelationshipViewSameFaction"]}")
                    return@Runnable
                }
                val relationshipService = plugin.services.factionRelationshipService
                val relationships = relationshipService.getRelationships(faction1.id, faction2.id)
                sender.sendMessage("${BukkitChatColor.AQUA}${plugin.language["CommandFactionRelationshipViewRelationshipsTitle"]}")
                sender.sendRelationshipView(relationships)
                if (sender.hasPermission("mf.relationship.add")) {
                    sender.spigot().sendMessage(
                        TextComponent(plugin.language["CommandFactionRelationshipViewCreateRelationshipButton"]).apply {
                            color = SpigotChatColor.GREEN
                            hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionRelationshipViewCreateRelationshipButtonHover"]))
                            clickEvent = ClickEvent(RUN_COMMAND, "/faction relationship add ${faction1.id.value} ${faction2.id.value}")
                        }
                    )
                }
                val reverseRelationships = relationshipService.getRelationships(faction2.id, faction1.id)
                sender.sendMessage("${BukkitChatColor.AQUA}${plugin.language["CommandFactionRelationshipViewReverseRelationshipsTitle"]}")
                sender.sendRelationshipView(reverseRelationships)
                sender.spigot().sendMessage(
                    TextComponent(plugin.language["CommandFactionRelationshipViewCreateReverseRelationshipButton"]).apply {
                        color = SpigotChatColor.GREEN
                        hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionRelationshipViewCreateReverseRelationshipButtonHover"]))
                        clickEvent = ClickEvent(RUN_COMMAND, "/faction relationship add ${faction2.id.value} ${faction1.id.value}")
                    }
                )
            }
        )
        return true
    }

    private fun CommandSender.sendRelationshipView(
        relationships: List<MfFactionRelationship>
    ) {
        relationships.forEach { relationship ->
            spigot().sendMessage(
                *buildList {
                    add(
                        TextComponent(
                            relationship.type.toTranslation()
                        ).apply {
                            color = SpigotChatColor.GRAY
                        }
                    )
                    if (hasPermission("mf.relationship.remove")) {
                        add(TextComponent(" "))
                        add(
                            TextComponent(plugin.language["CommandFactionRelationshipViewDeleteRelationshipButton"]).apply {
                                color = SpigotChatColor.RED
                                hoverEvent =
                                    HoverEvent(
                                        SHOW_TEXT,
                                        Text(plugin.language["CommandFactionRelationshipViewDeleteRelationshipButtonHover"])
                                    )
                                clickEvent = ClickEvent(
                                    RUN_COMMAND,
                                    "/faction relationship remove ${relationship.factionId.value} ${relationship.targetId.value} ${relationship.type.name}"
                                )
                            }
                        )
                    }
                }.toTypedArray()
            )
        }
    }

    private fun MfFactionRelationshipType.toTranslation() = when (this) {
        MfFactionRelationshipType.ALLY -> plugin.language["Ally"]
        MfFactionRelationshipType.AT_WAR -> plugin.language["AtWar"]
        MfFactionRelationshipType.VASSAL -> plugin.language["Vassal"]
        MfFactionRelationshipType.LIEGE -> plugin.language["Liege"]
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
            else -> emptyList()
        }
    }
}
