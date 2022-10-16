package com.dansplugins.factionsystem.command.duel.cancel

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfDuelCancelCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.duel")) {
            sender.sendMessage("$RED${plugin.language["CommandDuelCancelNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandDuelCancelNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandDuelCancelUsage"]}")
            return true
        }
        val target = plugin.server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("$RED${plugin.language["CommandDuelCancelInvalidTarget"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandDuelCancelFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val targetMfPlayer = playerService.getPlayer(target)
                ?: playerService.save(MfPlayer(plugin, target)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandDuelCancelFailedToSaveTargetPlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val duelService = plugin.services.duelService
            val invite = duelService.getInvite(targetMfPlayer.id, mfPlayer.id).onFailure {
                sender.sendMessage("$RED${plugin.language["CommandDuelCancelFailedToGetInvite"]}")
                plugin.logger.log(SEVERE, "Failed to get invite: ${it.reason.message}", it.reason.cause)
                return@Runnable
            } ?: duelService.getInvite(mfPlayer.id, targetMfPlayer.id).onFailure {
                sender.sendMessage("$RED${plugin.language["CommandDuelCancelFailedToGetInvite"]}")
                plugin.logger.log(SEVERE, "Failed to get invite: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            if (invite == null) {
                sender.sendMessage("$RED${plugin.language["CommandDuelCancelNoInvite", target.name]}")
                return@Runnable
            }
            duelService.deleteInvite(invite.inviterId, invite.inviteeId).onFailure {
                sender.sendMessage("$RED${plugin.language["CommandDuelCancelFailedToDeleteInvite"]}")
                plugin.logger.log(SEVERE, "Failed to delete invite", it.reason.cause)
                return@Runnable
            }
            if (invite.inviterId == mfPlayer.id) {
                sender.sendMessage("$GREEN${plugin.language["CommandDuelCancelSuccessCancelled", target.name]}")
                target.sendMessage("$RED${plugin.language["CommandDuelCancelChallengeCancelled", sender.name]}")
            } else {
                sender.sendMessage("$GREEN${plugin.language["CommandDuelCancelSuccessDeclined", target.name]}")
                target.sendMessage("$RED${plugin.language["CommandDuelCancelChallengeDeclined", sender.name]}")
            }

        })
        return true
    }
}