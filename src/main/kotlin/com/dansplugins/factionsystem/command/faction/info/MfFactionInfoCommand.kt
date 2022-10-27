package com.dansplugins.factionsystem.command.faction.info

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
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

class MfFactionInfoCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.info")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val factionService = plugin.services.factionService
            var faction: MfFaction? = null
            var mfPlayer: MfPlayer? = null

            // if a faction name was provided, grab that faction
            if (args.size > 0) {
                faction = plugin.services.factionService.getFaction(args[0])
                if (faction == null) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoFactionNotFound"]}")
                    return@Runnable
                }
            }
            else {
                // if the player is in a faction, grab that faction
                val player = plugin.services.playerService.getPlayer(sender)
                if (player != null) {

                    mfPlayer = playerService.getPlayer(sender)
                        ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoFailedToSavePlayer"]}")
                            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                            return@Runnable
                        }
                    faction = factionService.getFaction(mfPlayer.id)
                }
                // if the player is not in a faction, tell them to provide a faction name
                if (faction == null) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoNoFactionSpecified"]}")
                    return@Runnable
                }
            }

            // check player's role
            val role = faction.getRole(mfPlayer!!.id)
            if (role == null || !role.hasPermission(faction, plugin.factionPermissions.viewInfo)) {
                sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoNoFactionPermission"]}")
                return@Runnable
            }

            // send player faction info
            sender.sendMessage("${BukkitChatColor.AQUA}${plugin.language["CommandFactionInfoTitle", faction.name]}")
            if (sender.hasPermission("mf.rename")) {
                sender.spigot().sendMessage(TextComponent(plugin.language["CommandFactionInfoSetName"]).apply {
                    color = SpigotChatColor.GREEN
                    hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInfoSetNameHover"]))
                    clickEvent = ClickEvent(RUN_COMMAND, "/faction set name")
                })
            }
            if (faction.prefix != null) {
                sender.sendMessage("${BukkitChatColor.GRAY}${plugin.language["CommandFactionInfoPrefix", faction.prefix!!]}")
            } else {
                sender.sendMessage("${BukkitChatColor.GRAY}${plugin.language["CommandFactionInfoNoPrefix"]}")
            }
            if (sender.hasPermission("mf.prefix")) {
                sender.spigot().sendMessage(TextComponent(plugin.language["CommandFactionInfoSetPrefix"]).apply {
                    color = SpigotChatColor.GREEN
                    hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInfoSetPrefixHover"]))
                    clickEvent = ClickEvent(RUN_COMMAND, "/faction set prefix")
                })
            }
            sender.sendMessage("${BukkitChatColor.GRAY}${plugin.language["CommandFactionInfoDescription", faction.description]}")
            if (sender.hasPermission("mf.desc")) {
                sender.spigot().sendMessage(TextComponent(plugin.language["CommandFactionInfoSetDescription"]).apply {
                    color = SpigotChatColor.GREEN
                    hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInfoSetDescriptionHover"]))
                    clickEvent = ClickEvent(RUN_COMMAND, "/faction set description")
                })
            }
            sender.sendMessage("${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoMembersTitle", faction.members.size.toString()]}")
            faction.members.groupBy { faction.getRole(it.playerId) }.forEach { (memberRole, members) ->
                sender.sendMessage("  ${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoMembersRoleTitle", memberRole?.name ?: plugin.language["NoRole"]]}")
                sender.sendMessage("    ${BukkitChatColor.GRAY}${members.joinToString { it.playerId.toBukkitPlayer().name ?: plugin.language["UnknownPlayer"] }}")
            }
            sender.sendMessage("${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoInvitesTitle", faction.invites.size.toString()]}")
            sender.sendMessage("  ${BukkitChatColor.GRAY}${faction.invites.joinToString { it.playerId.toBukkitPlayer().name ?: plugin.language["UnknownPlayer"] }}")
            if (sender.hasPermission("mf.invite") && role.hasPermission(faction, plugin.factionPermissions.invite)) {
                sender.spigot().sendMessage(TextComponent(plugin.language["CommandFactionInfoInvite"]).apply {
                    color = SpigotChatColor.GREEN
                    hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInfoInviteHover"]))
                    clickEvent = ClickEvent(RUN_COMMAND, "/faction invite")
                })
            }
            factionService.fields
                .filter { field -> field.isVisibleFor(faction.id.value, mfPlayer.id.value) }
                .forEach { field ->
                    sender.spigot().sendMessage(
                        TextComponent("${field.name}: ").apply {
                            color = SpigotChatColor.GRAY
                        },
                        *field.get(faction.id.value)
                    )
                }
        })
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}
