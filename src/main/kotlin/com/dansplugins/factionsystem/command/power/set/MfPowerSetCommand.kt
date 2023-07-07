package com.dansplugins.factionsystem.command.power.set

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.logging.Level

class MfPowerSetCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val decimalFormat = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(plugin.language.locale))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.power.set") && !sender.hasPermission("mf.force.power")) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetNoPermission"]}")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetUsage"]}")
            return true
        }
        val target = plugin.server.getOfflinePlayer(args[0])
        if (!target.isOnline && !target.hasPlayedBefore()) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetInvalidTarget"]}")
            return true
        }
        val power = args[1].toDoubleOrNull()
        if (power == null) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetInvalidPowerMustBeNumber"]}")
            return true
        }
        val minPower = plugin.config.getDouble("players.minPower")
        if (power < minPower) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetInvalidPowerTooLow", decimalFormat.format(minPower)]}")
            return true
        }
        val maxPower = plugin.config.getDouble("players.maxPower")
        if (power > maxPower) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetInvalidPowerTooHigh", decimalFormat.format(maxPower)]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val targetMfPlayer = playerService.getPlayer(target) ?: MfPlayer(plugin, target)
                playerService.save(targetMfPlayer.copy(power = power)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandPowerSetFailedToSaveTargetPlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandPowerSetSuccess", target.name ?: plugin.language["UnknownPlayer"], decimalFormat.format(power), decimalFormat.format(maxPower)]}")
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = when {
        args.isEmpty() ->
            plugin.server.offlinePlayers
                .mapNotNull { it.name }
        args.size == 1 ->
            plugin.server.offlinePlayers
                .filter { it.name?.lowercase()?.startsWith(args[0].lowercase()) == true }
                .mapNotNull { it.name }
        else -> emptyList()
    }
}
