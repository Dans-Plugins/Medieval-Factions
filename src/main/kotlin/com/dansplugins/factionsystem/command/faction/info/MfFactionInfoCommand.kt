package com.dansplugins.factionsystem.command.faction.info

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.AQUA
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionInfoCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.info")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionInfoNoPermission"]}")
            return true
        }
        
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionInfoNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            // get player's faction
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer.fromBukkit(sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionInfoFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            if (faction == null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionInfoMustBeInAFaction"]}")
                return@Runnable
            }
            // send player faction info
            sender.sendMessage("$AQUA${plugin.language["CommandFactionInfoTitle", faction.name]}")
            sender.sendMessage("$AQUA${plugin.language["CommandFactionInfoDescription", faction.description]}")
            sender.sendMessage("$AQUA${plugin.language["CommandFactionInfoMembers", "" + faction.members.size]}")
        })
        return true
    }
}
