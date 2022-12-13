package com.dansplugins.factionsystem.command.unlock

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.UNLOCKING
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfUnlockCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.unlock")) {
            sender.sendMessage("$RED${plugin.language["CommandUnlockNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandUnlockNotAPlayer"]}")
            return true
        }
        val cancel = args.isNotEmpty() && args.first().equals("cancel", ignoreCase = true)
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandUnlockFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val interactionService = plugin.services.interactionService
                val status = interactionService.getInteractionStatus(mfPlayer.id)
                if (cancel) {
                    if (status != UNLOCKING) {
                        sender.sendMessage("$RED${plugin.language["CommandUnlockCancelNotUnlocking"]}")
                        return@Runnable
                    }
                    interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandUnlockFailedToSetInteractionStatus"]}")
                        plugin.logger.log(SEVERE, "Failed to set player interaction status: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                    sender.sendMessage("$GREEN${plugin.language["CommandUnlockCancelSuccess"]}")
                    return@Runnable
                }
                interactionService.setInteractionStatus(mfPlayer.id, UNLOCKING).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandUnlockFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set player interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandUnlockSuccess"]}")
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
        args.isEmpty() -> listOf("cancel")
        args.size == 1 && "cancel".startsWith(args[0].lowercase()) -> listOf("cancel")
        else -> emptyList()
    }
}
