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
import org.bukkit.OfflinePlayer
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
        val hasViewOtherPermission = sender.hasPermission("mf.info.other")
        if (sender !is Player && !hasViewOtherPermission) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoNotAPlayer"]}")
            return true
        }
        var targetPlayer: OfflinePlayer?
        if (hasViewOtherPermission && args.isNotEmpty()) {
            targetPlayer = plugin.server.getOfflinePlayer(args.first())
            if (!targetPlayer.isOnline && !targetPlayer.hasPlayedBefore()) {
                targetPlayer = null
            }
        } else if (sender is Player) {
            targetPlayer = sender
        } else {
            targetPlayer = null
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                // get player's faction
                val playerService = plugin.services.playerService
                val senderMfPlayer = if (sender is Player) {
                    playerService.getPlayer(sender)
                        ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoFailedToSavePlayer"]}")
                            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                            return@Runnable
                        }
                } else {
                    null
                }
                val targetMfPlayer = if (targetPlayer != null) {
                    playerService.getPlayer(targetPlayer)
                        ?: playerService.save(MfPlayer(plugin, targetPlayer)).onFailure {
                            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoFailedToSavePlayer"]}")
                            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                            return@Runnable
                        }
                } else {
                    null
                }
                val factionService = plugin.services.factionService
                val faction = when {
                    targetMfPlayer != null -> factionService.getFaction(targetMfPlayer.id)
                    hasViewOtherPermission -> factionService.getFaction(args.joinToString(" "))
                    else -> null
                }
                val senderFaction = if (senderMfPlayer != null) {
                    factionService.getFaction(senderMfPlayer.id)
                } else {
                    null
                }
                if (faction == null) {
                    if (args.isEmpty() || !hasViewOtherPermission) {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoMustBeInAFaction"]}")
                    } else {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoInvalidTarget"]}")
                    }
                    return@Runnable
                }
                val role = if (senderMfPlayer != null) {
                    faction.getRole(senderMfPlayer.id)
                } else {
                    null
                }
                if (!hasViewOtherPermission && (role == null || !role.hasPermission(faction, plugin.factionPermissions.viewInfo))) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInfoNoFactionPermission"]}")
                    return@Runnable
                }
                // send player faction info
                sender.sendMessage("${BukkitChatColor.AQUA}${plugin.language["CommandFactionInfoTitle", faction.name]}")
                if (sender.hasPermission("mf.rename") && senderFaction?.id == faction.id && role?.hasPermission(faction, plugin.factionPermissions.changeName) == true) {
                    sender.spigot().sendMessage(
                        TextComponent(plugin.language["CommandFactionInfoSetName"]).apply {
                            color = SpigotChatColor.GREEN
                            hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInfoSetNameHover"]))
                            clickEvent = ClickEvent(RUN_COMMAND, "/faction set name")
                        }
                    )
                }
                if (faction.prefix != null) {
                    sender.sendMessage("${BukkitChatColor.GRAY}${plugin.language["CommandFactionInfoPrefix", faction.prefix]}")
                } else {
                    sender.sendMessage("${BukkitChatColor.GRAY}${plugin.language["CommandFactionInfoNoPrefix"]}")
                }
                if (sender.hasPermission("mf.prefix") && senderFaction?.id == faction.id && role?.hasPermission(faction, plugin.factionPermissions.changePrefix) == true) {
                    sender.spigot().sendMessage(
                        TextComponent(plugin.language["CommandFactionInfoSetPrefix"]).apply {
                            color = SpigotChatColor.GREEN
                            hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInfoSetPrefixHover"]))
                            clickEvent = ClickEvent(RUN_COMMAND, "/faction set prefix")
                        }
                    )
                }
                sender.sendMessage("${BukkitChatColor.GRAY}${plugin.language["CommandFactionInfoDescription", faction.description]}")
                if (sender.hasPermission("mf.desc") && senderFaction?.id == faction.id && role?.hasPermission(faction, plugin.factionPermissions.changeDescription) == true) {
                    sender.spigot().sendMessage(
                        TextComponent(plugin.language["CommandFactionInfoSetDescription"]).apply {
                            color = SpigotChatColor.GREEN
                            hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInfoSetDescriptionHover"]))
                            clickEvent = ClickEvent(RUN_COMMAND, "/faction set description")
                        }
                    )
                }
                sender.sendMessage("${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoMembersTitle", faction.members.size.toString()]}")
                faction.members.groupBy { faction.getRole(it.playerId) }.forEach { (memberRole, members) ->
                    sender.sendMessage("  ${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoMembersRoleTitle", memberRole?.name ?: plugin.language["NoRole"]]}")
                    sender.sendMessage("    ${BukkitChatColor.GRAY}${members.joinToString { it.playerId.toBukkitPlayer().name ?: plugin.language["UnknownPlayer"] }}")
                }
                sender.sendMessage("${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoInvitesTitle", faction.invites.size.toString()]}")
                sender.sendMessage("  ${BukkitChatColor.GRAY}${faction.invites.joinToString { it.playerId.toBukkitPlayer().name ?: plugin.language["UnknownPlayer"] }}")
                if (sender.hasPermission("mf.invite") && senderFaction?.id == faction.id && role?.hasPermission(faction, plugin.factionPermissions.invite) == true) {
                    sender.spigot().sendMessage(
                        TextComponent(plugin.language["CommandFactionInfoInvite"]).apply {
                            color = SpigotChatColor.GREEN
                            hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInfoInviteHover"]))
                            clickEvent = ClickEvent(RUN_COMMAND, "/faction invite")
                        }
                    )
                }

                // send vassals information
                val vassals = plugin.services.factionRelationshipService.getVassals(faction.id).mapNotNull(factionService::getFaction)
                if (vassals.isNotEmpty()) {
                    sender.sendMessage("${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoVassalsTitle"]}")
                    sender.sendMessage("${BukkitChatColor.GRAY}" + vassals.joinToString(transform = MfFaction::name))
                }

                // send allies information
                val allies = plugin.services.factionRelationshipService.getAllies(faction.id).mapNotNull(factionService::getFaction)
                if (allies.isNotEmpty()) {
                    sender.sendMessage("${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoAlliesTitle"]}")
                    sender.sendMessage("${BukkitChatColor.GRAY}" + allies.joinToString(transform = MfFaction::name))
                }

                // send wars information
                val atWarWith = plugin.services.factionRelationshipService.getFactionsAtWarWith(faction.id).mapNotNull(factionService::getFaction)
                if (atWarWith.isNotEmpty()) {
                    sender.sendMessage("${BukkitChatColor.WHITE}${plugin.language["CommandFactionInfoEnemiesTitle"]}")
                    sender.sendMessage("${BukkitChatColor.GRAY}" + atWarWith.joinToString(transform = MfFaction::name))
                }

                factionService.fields
                    .filter { field -> field.isVisibleFor(faction.id.value, senderMfPlayer?.id?.value ?: return@filter false) }
                    .forEach { field ->
                        sender.spigot().sendMessage(
                            TextComponent("${field.name}: ").apply {
                                color = SpigotChatColor.GRAY
                            },
                            *field.get(faction.id.value)
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
    ) = when {
        args.isEmpty() -> plugin.services.factionService.factions.map(MfFaction::name) +
            plugin.server.offlinePlayers.mapNotNull(OfflinePlayer::getName)
        args.size == 1 -> (
            plugin.services.factionService.factions.map(MfFaction::name) +
                plugin.server.offlinePlayers.mapNotNull(OfflinePlayer::getName)
            )
            .filter { it.lowercase().startsWith(args[0].lowercase()) }
        else -> emptyList()
    }
}
