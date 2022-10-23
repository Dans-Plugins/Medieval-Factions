package com.dansplugins.factionsystem.command.faction.power

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionPowerCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.power")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionPowerNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionPowerNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionPowerFailedToSavePlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            sender.sendMessage("$AQUA$BOLD${plugin.language["CommandFactionPowerTitle"]}")
            sender.sendMessage("$GRAY${plugin.language[
                    "CommandFactionPowerPlayerPower",
                    mfPlayer.power.toString(),
                    plugin.config.getInt("players.maxPower").toString()
            ]}")
            if (faction != null) {
                sender.sendMessage(
                    "$GRAY${
                        plugin.language[
                                "CommandFactionPowerFactionPower",
                                faction.power.toString(),
                                faction.maxPower.toString(),
                                faction.memberPower.toString(),
                                faction.maxMemberPower.toString(),
                                faction.vassalPower.toString(),
                                faction.maxVassalPower.toString(),
                                if (faction.flags[plugin.flags.acceptBonusPower]) faction.bonusPower.toString() else "0"
                        ]
                    }"
                )
            }
        })
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}