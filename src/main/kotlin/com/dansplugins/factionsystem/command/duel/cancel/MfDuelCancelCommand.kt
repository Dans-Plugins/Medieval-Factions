package com.dansplugins.factionsystem.command.duel.cancel

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfDuelCancelCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
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
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
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
                val invite = duelService.getInvite(targetMfPlayer.id, mfPlayer.id)
                    ?: duelService.getInvite(mfPlayer.id, targetMfPlayer.id)
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
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player) return emptyList()
        val senderMfPlayerId = MfPlayerId.fromBukkitPlayer(sender)
        val duelService = plugin.services.duelService
        return when {
            args.isEmpty() -> plugin.server.onlinePlayers.filter { bukkitPlayer ->
                duelService.getInvitesByInvitee(senderMfPlayerId).any {
                    it.inviterId == MfPlayerId.fromBukkitPlayer(bukkitPlayer)
                } || duelService.getInvitesByInviter(senderMfPlayerId).any {
                    it.inviteeId == MfPlayerId.fromBukkitPlayer(bukkitPlayer)
                }
            }.map(Player::getName)
            args.size == 1 -> plugin.server.onlinePlayers.filter { bukkitPlayer ->
                if (!bukkitPlayer.name.lowercase().startsWith(args[0].lowercase())) return@filter false
                duelService.getInvitesByInvitee(senderMfPlayerId).any {
                    it.inviterId == MfPlayerId.fromBukkitPlayer(bukkitPlayer)
                } || duelService.getInvitesByInviter(senderMfPlayerId).any {
                    it.inviteeId == MfPlayerId.fromBukkitPlayer(bukkitPlayer)
                }
            }.map(Player::getName)
            else -> emptyList()
        }
    }
}
