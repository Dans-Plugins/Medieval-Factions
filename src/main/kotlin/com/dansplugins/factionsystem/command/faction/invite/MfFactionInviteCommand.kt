package com.dansplugins.factionsystem.command.faction.invite

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionInvite
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.INVITE
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionInviteCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.invite")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionInviteNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionInviteUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionInviteNotAPlayer"]}")
            return true
        }
        val target = plugin.server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionInviteInvalidTarget"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer.fromBukkit(sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInviteFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val targetMfPlayer = playerService.getPlayer(target)
                ?: playerService.save(MfPlayer.fromBukkit(target)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInviteFailedToSaveTargetPlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            if (faction == null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionInviteMustBeInAFaction"]}")
                return@Runnable
            }
            val role = faction.getRole(mfPlayer.id)
            if (role == null || !role.hasPermission(faction, INVITE)) {
                sender.sendMessage("$RED${plugin.language["CommandFactionInviteNoRolePermission"]}")
                return@Runnable
            }
            if (faction.invites.any { it.player.id.value == targetMfPlayer.id.value }) {
                sender.sendMessage("$RED${plugin.language["CommandFactionInviteAlreadyInvited"]}")
                return@Runnable
            }
            if (faction.members.any { it.player.id.value == targetMfPlayer.id.value }) {
                sender.sendMessage("$RED${plugin.language["CommandFactionInviteAlreadyMember"]}")
                return@Runnable
            }
            factionService.save(faction.copy(
                invites = faction.invites + MfFactionInvite(targetMfPlayer)
            )).onFailure {
                sender.sendMessage("$RED${plugin.language["CommandFactionInviteFailedToSaveFaction"]}")
                plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            sender.sendMessage("$GREEN${plugin.language["CommandFactionInviteSuccess", target.name]}")
        })
        return true
    }
}