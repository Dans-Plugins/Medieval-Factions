package com.dansplugins.factionsystem.command.faction.law

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
import java.util.logging.Level
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionLawListCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.law.list") || !sender.hasPermission("mf.laws")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionLawListNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionLawListNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionLawListFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction: MfFaction? = if (args.elementAtOrNull(0) != null) {
                    factionService.getFaction(args.elementAt(0))
                } else {
                    factionService.getFaction(mfPlayer.id)
                }
                if (faction == null) {
                    if (args.elementAtOrNull(0) == null) {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionLawListMustBeInAFaction"]}")
                        return@Runnable
                    } else {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionLawListFactionNotFound", args.elementAt(0)]}")
                        return@Runnable
                    }
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.listLaws)) {
                    sender.sendMessage("${org.bukkit.ChatColor.RED}${plugin.language["CommandFactionLawListNoFactionPermission"]}")
                    return@Runnable
                }
                val lawService = plugin.services.lawService
                sender.sendMessage(BukkitChatColor.WHITE.toString() + plugin.language["CommandFactionLawListTitle"])
                val laws = lawService.getLaws(faction.id)
                laws.forEachIndexed { i, law ->
                    // This ensures that all legacy laws will have an index number. Should probably do this somewhere else though...
                    if (law.number == null) {
                        lawService.save(law.copy(number = i + 1))
                    }
                    val buttonList: MutableList<TextComponent> = mutableListOf()
                    if ((sender.hasPermission("mf.law.remove") || sender.hasPermission("mf.removelaw")) && role.hasPermission(faction, plugin.factionPermissions.removeLaw)) {
                        val deleteButton = TextComponent("✖ ")
                        deleteButton.color = SpigotChatColor.RED
                        deleteButton.clickEvent = ClickEvent(RUN_COMMAND, "/faction law remove ${law.id.value}")
                        deleteButton.hoverEvent =
                            HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionLawListDeleteButtonHover"]))
                        buttonList.add(deleteButton)
                    }
                    if (sender.hasPermission("mf.law.edit") && role.hasPermission(faction, plugin.factionPermissions.editLaw)) {
                        val editButton = TextComponent("✎ ")
                        editButton.color = SpigotChatColor.YELLOW
                        editButton.clickEvent = ClickEvent(RUN_COMMAND, "/faction law edit ${law.id.value}")
                        editButton.hoverEvent =
                            HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionLawListEditButtonHover"]))
                        buttonList.add(editButton)
                    }
                    if (laws.size > 1 && sender.hasPermission("mf.law.move") && role.hasPermission(faction, plugin.factionPermissions.moveLaw)) {
                        val moveButton = TextComponent("☰ ")
                        moveButton.color = SpigotChatColor.BLUE
                        moveButton.clickEvent = ClickEvent(RUN_COMMAND, "/faction law move ${law.id.value}")
                        moveButton.hoverEvent =
                            HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionLawListMoveButtonHover"]))
                        buttonList.add(moveButton)
                    }
                    val text = TextComponent(plugin.language["CommandFactionLawListLaw", (i + 1).toString(), law.text])
                    text.color = SpigotChatColor.AQUA
                    sender.spigot().sendMessage(
                        *buttonList.toTypedArray(),
                        text
                    )
                }
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
