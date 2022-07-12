package com.dansplugins.factionsystem.command.faction.law

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.LIST_LAWS
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
import java.util.logging.Level
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionLawListCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.laws")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionLawListNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionLawListNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer.fromBukkit(sender)).onFailure {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionLawListFailedToSavePlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            if (faction == null) {
                sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionLawListMustBeInAFaction"]}")
                return@Runnable
            }
            val role = faction.getRole(mfPlayer.id)
            if (role == null || !role.hasPermission(faction, LIST_LAWS)) {
                sender.sendMessage("${org.bukkit.ChatColor.RED}${plugin.language["CommandFactionLawListNoRolePermission"]}")
                return@Runnable
            }
            val lawService = plugin.services.lawService
            sender.sendMessage(BukkitChatColor.WHITE.toString() + plugin.language["CommandFactionLawListTitle"])
            lawService.getLaws(faction.id).forEachIndexed { i, law ->
                val deleteButton = TextComponent("x ")
                deleteButton.color = SpigotChatColor.RED
                deleteButton.clickEvent = ClickEvent(RUN_COMMAND, "/faction law remove ${law.id.value}")
                deleteButton.hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionLawListDeleteButtonHover"]))
                val text = TextComponent(plugin.language["CommandFactionLawListLaw", (i + 1).toString(), law.text])
                text.color = SpigotChatColor.AQUA
                sender.spigot().sendMessage(
                    deleteButton,
                    text
                )
            }
        })
        return true
    }
}