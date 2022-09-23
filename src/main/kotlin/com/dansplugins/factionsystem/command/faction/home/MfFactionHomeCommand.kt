package com.dansplugins.factionsystem.command.faction.home

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.GO_HOME
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionHomeCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.home")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionHomeNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionHomeNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer.fromBukkit(sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionHomeFailedToSavePlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            if (faction == null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionHomeMustBeInAFaction"]}")
                return@Runnable
            }
            val role = faction.getRole(mfPlayer.id)
            if (role == null || !role.hasPermission(faction, GO_HOME)) {
                sender.sendMessage("$RED${plugin.language["CommandFactionHomeNoFactionPermission"]}")
                return@Runnable
            }
            val home = faction.home?.toBukkitLocation()
            if (home == null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionHomeNoFactionHomeSet"]}")
                return@Runnable
            }
            plugin.server.scheduler.runTask(plugin, Runnable {
                sender.teleport(home)
                sender.sendMessage("$GREEN${plugin.language["CommandFactionHomeSuccess"]}")
            })
        })
        return true
    }
}