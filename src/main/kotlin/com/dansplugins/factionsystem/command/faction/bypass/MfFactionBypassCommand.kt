package com.dansplugins.factionsystem.command.faction.bypass

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
import java.util.logging.Level

class MfFactionBypassCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.bypass")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionBypassNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionBypassNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender) ?: MfPlayer(plugin, sender)
                val updatedMfPlayer = playerService.save(mfPlayer.copy(isBypassEnabled = !mfPlayer.isBypassEnabled)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionBypassFailedToSavePlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                if (updatedMfPlayer.isBypassEnabled) {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionBypassEnabled"]}")
                } else {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionBypassDisabled"]}")
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
    ) = emptyList<String>()
}
