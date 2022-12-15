package com.dansplugins.factionsystem.command.duel.challenge

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.duel.MfDuelInvite
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE
import net.md_5.bungee.api.ChatColor as SpigotChatColor

class MfDuelChallengeCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.duel")) {
            sender.sendMessage("$RED${plugin.language["CommandDuelChallengeNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandDuelChallengeNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandDuelChallengeUsage"]}")
            return true
        }
        val target = plugin.server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("$RED${plugin.language["CommandDuelChallengeInvalidTarget"]}")
            return true
        }
        if (sender == target) {
            sender.sendMessage("$RED${plugin.language["CommandDuelChallengeCannotDuelSelf"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandDuelChallengeFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val targetMfPlayer = playerService.getPlayer(target)
                    ?: playerService.save(MfPlayer(plugin, target)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandDuelChallengeFailedToSaveTargetPlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val duelService = plugin.services.duelService
                val existingDuel = duelService.getDuel(mfPlayer.id)
                if (existingDuel != null) {
                    sender.sendMessage("$RED${plugin.language["CommandDuelChallengeAlreadyInADuel"]}")
                    return@Runnable
                }
                val targetExistingDuel = duelService.getDuel(targetMfPlayer.id)
                if (targetExistingDuel != null) {
                    sender.sendMessage("$RED${plugin.language["CommandDuelChallengeTargetAlreadyInDuel"]}")
                    return@Runnable
                }
                val existingInvite = duelService.getInvite(mfPlayer.id, targetMfPlayer.id)
                if (existingInvite != null) {
                    sender.sendMessage("$RED${plugin.language["CommandDuelChallengeTargetAlreadyInvited"]}")
                    return@Runnable
                }
                duelService.save(
                    MfDuelInvite(
                        mfPlayer.id,
                        targetMfPlayer.id
                    )
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandDuelChallengeFailedToSaveInvite"]}")
                    plugin.logger.log(SEVERE, "Failed to save duel invite: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandDuelChallengeSuccess", target.name]}")
                target.sendMessage("$GRAY${plugin.language["CommandDuelChallengeReceived", sender.name]}")
                target.spigot().sendMessage(
                    TextComponent(plugin.language["CommandDuelChallengeReceivedAccept"]).apply {
                        color = SpigotChatColor.GREEN
                        isBold = true
                        hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandDuelChallengeReceivedAcceptHover"]))
                        clickEvent = ClickEvent(RUN_COMMAND, "/duel accept ${sender.name}")
                    },
                    TextComponent(" "),
                    TextComponent(plugin.language["CommandDuelChallengeReceivedDecline"]).apply {
                        color = SpigotChatColor.RED
                        isBold = true
                        hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandDuelChallengeReceivedDeclineHover"]))
                        clickEvent = ClickEvent(RUN_COMMAND, "/duel cancel ${sender.name}")
                    }
                )
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
        args.isEmpty() -> plugin.server.onlinePlayers.map(Player::getName)
        args.size == 1 ->
            plugin.server.onlinePlayers
                .filter { it.name.lowercase().startsWith(args[0].lowercase()) }
                .map(Player::getName)
        else -> emptyList()
    }
}
