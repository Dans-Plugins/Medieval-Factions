package com.dansplugins.factionsystem.command.faction.join

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionJoinCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.join")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionJoinNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionJoinUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionJoinNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer.fromBukkit(sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionJoinFailedToSavePlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val playerFaction = factionService.getFaction(mfPlayer.id)
            if (playerFaction != null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionJoinAlreadyInFaction"]}")
                return@Runnable
            }
            val faction = factionService.getFaction(args.joinToString(" "))
            if (faction == null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionJoinInvalidFaction"]}")
                return@Runnable
            }
            if (!faction.invites.any { it.player.id.value == mfPlayer.id.value }) {
                sender.sendMessage("$RED${plugin.language["CommandFactionJoinNotInvited"]}")
                return@Runnable
            }
            val updatedFaction = factionService.save(faction.copy(
                members = faction.members + MfFactionMember(mfPlayer, faction.roles.default),
                invites = faction.invites.filter { it.player.id.value != mfPlayer.id.value }
            )).onFailure {
                sender.sendMessage("$RED${plugin.language["CommandFactionJoinFailedToSaveFaction"]}")
                plugin.logger.log(Level.SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            updatedFaction.sendMessage(
                plugin.language["FactionNewMemberNotificationTitle", sender.name],
                plugin.language["FactionNewMemberNotificationBody", sender.name]
            )
            sender.sendMessage(
                plugin.language["CommandFactionJoinSuccess", faction.name]
            )
        })
        return true
    }
}