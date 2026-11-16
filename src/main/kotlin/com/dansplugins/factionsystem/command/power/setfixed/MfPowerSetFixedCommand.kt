package com.dansplugins.factionsystem.command.power.setfixed

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

/**
 * `/power setfixed [player] [amount]` (#1682): pins a player's power to [amount]. While it is set the
 * player's power is that amount and nothing changes it (gain and decay over time, kills, deaths,
 * `/power set`) until `/power clearfixed` removes it. The amount must lie within
 * `players.minPower`..`players.maxPower` when it is set, as for `/power set`.
 */
class MfPowerSetFixedCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val decimalFormat = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(plugin.language.locale))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.power.fixed") && !sender.hasPermission("mf.force.power")) {
            sender.sendMessage("$RED${plugin.language["CommandPowerFixedNoPermission"]}")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetFixedUsage"]}")
            return true
        }
        val target = plugin.server.getOfflinePlayer(args[0])
        if (!target.isOnline && !target.hasPlayedBefore()) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetInvalidTarget"]}")
            return true
        }
        val power = args[1].toDoubleOrNull()
        if (power == null || power.isNaN() || power.isInfinite()) {
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
                // powerAtLogout is set as well, so that offline decay resumes from the fixed amount
                // once the fixed power is cleared.
                playerService.update(targetMfPlayer) {
                    it.copy(power = power, powerAtLogout = power, fixedPower = power)
                }.onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandPowerSetFailedToSaveTargetPlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage(
                    "$GREEN${plugin.language["CommandPowerSetFixedSuccess", target.name ?: plugin.language["UnknownPlayer"], decimalFormat.format(power)]}"
                )
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
