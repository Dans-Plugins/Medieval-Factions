package com.dansplugins.factionsystem.command.power.set

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.util.logging.Level

class MfPowerSetCommand(private val plugin: MedievalFactions) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.power.set")) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetNoPermission"]}")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetUsage"]}")
            return true
        }
        val target = plugin.server.getOfflinePlayer(args[0])
        if (!target.hasPlayedBefore()) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetInvalidTarget"]}")
            return true
        }
        val power = args[1].toIntOrNull()
        if (power == null) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetInvalidPowerMustBeInteger"]}")
            return true
        }
        if (power < 0) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetInvalidPowerCannotBeNegative"]}")
            return true
        }
        val maxPower = plugin.config.getInt("players.maxPower")
        if (power > maxPower) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetInvalidPowerTooHigh", maxPower.toString()]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val targetMfPlayer = playerService.getPlayer(target) ?: MfPlayer.fromBukkit(target)
            playerService.save(targetMfPlayer.copy(power = power)).onFailure {
                sender.sendMessage("$RED${plugin.language["CommandPowerSetFailedToSaveTargetPlayer"]}")
                plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            sender.sendMessage("$GREEN${plugin.language["CommandPowerSetSuccess", target.name ?: "unknown player", power.toString(), maxPower.toString()]}")
        })
        return true
    }

}