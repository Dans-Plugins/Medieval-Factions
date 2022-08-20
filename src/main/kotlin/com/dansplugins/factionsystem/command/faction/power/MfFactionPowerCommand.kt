package com.dansplugins.factionsystem.command.faction.power

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.BOLD
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionPowerCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.power")) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionPowerNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionPowerNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer.fromBukkit(sender)).onFailure {
                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionPowerFailedToSavePlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            sender.sendMessage("$AQUA$BOLD${plugin.language["CommandFactionPowerTitle"]}")
            sender.sendMessage("$AQUA${plugin.language[
                    "CommandFactionPowerPlayerPower",
                    mfPlayer.power.toString(),
                    plugin.config.getInt("players.maxPower").toString()
            ]}")
            if (faction != null) {
                sender.sendMessage(
                    "$AQUA${
                        plugin.language[
                                "CommandFactionPowerFactionPower",
                                faction.power.toString(),
                                faction.maxPower.toString(),
                                faction.memberPower.toString(),
                                faction.maxMemberPower.toString(),
                                faction.vassalPower.toString(),
                                faction.maxVassalPower.toString(),
                                faction.bonusPower.toString()
                        ]
                    }"
                )
            }
        })
        return true
    }
}