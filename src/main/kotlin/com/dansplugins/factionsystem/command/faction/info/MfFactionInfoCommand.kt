package com.dansplugins.factionsystem.command.faction.info

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.INVITE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VIEW_INFO
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
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionInfoCommand(private val plugin: MedievalFactions) : CommandExecutor {
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
            // get player's faction
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer.fromBukkit(sender)).onFailure {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            if (faction == null) {
                sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoMustBeInAFaction"]}")
                return@Runnable
            }
            val role = faction.getRole(mfPlayer.id)
            if (role == null || !role.hasPermission(faction, VIEW_INFO)) {
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
            sender.sendMessage("${BukkitChatColor.GRAY}${plugin.language["CommandFactionInfoDescription", faction.description]}")
            if (sender.hasPermission("mf.desc")) {
                sender.spigot().sendMessage(TextComponent(plugin.language["CommandFactionInfoSetDescription"]).apply {
                    color = SpigotChatColor.GREEN
                    hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInfoSetDescriptionHover"]))
                    clickEvent = ClickEvent(RUN_COMMAND, "/faction set description")
                })
            }
            sender.sendMessage("${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoMembersTitle", faction.members.size.toString()]}")
            faction.members.groupBy { faction.getRole(it.player.id) }.forEach { (memberRole, members) ->
                sender.sendMessage("  ${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoMembersRoleTitle", memberRole?.name ?: plugin.language["NoRole"]]}")
                sender.sendMessage("    ${BukkitChatColor.GRAY}${members.joinToString { it.player.toBukkit().name ?: plugin.language["UnknownPlayer"] }}")
            }
            sender.sendMessage("${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoInvitesTitle", faction.invites.size.toString()]}")
            sender.sendMessage("  ${BukkitChatColor.GRAY}${faction.invites.joinToString { it.player.toBukkit().name ?: plugin.language["UnknownPlayer"] }}")
            if (sender.hasPermission("mf.invite") && role.hasPermission(faction, INVITE)) {
                sender.spigot().sendMessage(TextComponent(plugin.language["CommandFactionInfoInvite"]).apply {
                    color = SpigotChatColor.GREEN
                    hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInfoInviteHover"]))
                    clickEvent = ClickEvent(RUN_COMMAND, "/faction invite")
                })
            }
        })
        return true
    }
}
