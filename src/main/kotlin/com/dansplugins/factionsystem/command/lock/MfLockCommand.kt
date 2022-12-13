package com.dansplugins.factionsystem.command.lock

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.LOCKING
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

class MfLockCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.lock")) {
            sender.sendMessage("$RED${plugin.language["CommandLockNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandLockNotAPlayer"]}")
            return true
        }
        val cancel = args.isNotEmpty() && args.first().equals("cancel", ignoreCase = true)
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandLockFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val interactionService = plugin.services.interactionService
                val status = interactionService.getInteractionStatus(mfPlayer.id)
                if (cancel) {
                    if (status != LOCKING) {
                        sender.sendMessage("$RED${plugin.language["CommandLockCancelNotLocking"]}")
                        return@Runnable
                    }
                    interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandLockFailedToSetInteractionStatus"]}")
                        plugin.logger.log(SEVERE, "Failed to set player interaction status: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                    sender.sendMessage("$GREEN${plugin.language["CommandLockCancelSuccess"]}")
                    return@Runnable
                }
                interactionService.setInteractionStatus(mfPlayer.id, LOCKING).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandLockFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set player interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandLockSuccess"]}")
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
