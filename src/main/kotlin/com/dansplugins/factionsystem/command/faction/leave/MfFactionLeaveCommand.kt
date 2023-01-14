package com.dansplugins.factionsystem.command.faction.leave

import com.dansplugins.factionsystem.MedievalFactions
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

class MfFactionLeaveCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.leave")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionLeaveNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionLeaveNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionLeaveFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionLeaveMustBeInAFaction"]}")
                    return@Runnable
                }
                if (faction.members.size == 1) {
                    factionService.delete(faction.id)
                        .onFailure {
                            sender.sendMessage("$RED${plugin.language["CommandFactionLeaveFailedToDisbandFaction"]}")
                            return@Runnable
                        }
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionLeaveSuccess"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role != null && faction.members.filter { it.playerId != mfPlayer.id }.none {
                    val memberRole = faction.getRole(it.playerId)
                    memberRole?.hasPermission(faction, plugin.factionPermissions.setMemberRole(role.id)) == true
                }
                ) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionLeaveNoOneCanSetYourRole"]}")
                    return@Runnable
                }
                factionService.save(faction.copy(members = faction.members.filter { it.playerId != mfPlayer.id }))
                    .onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionLeaveFailedToSaveFaction"]}")
                        plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionLeaveSuccess"]}")
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
