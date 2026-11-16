package com.dansplugins.factionsystem.command.power.clearfixed

import com.dansplugins.factionsystem.MedievalFactions
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
 * `/power clearfixed [player]` (#1682): removes a fixed power set with `/power setfixed`. The player
 * keeps the fixed amount as their current power, and from there it changes normally again.
 */
class MfPowerClearFixedCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val decimalFormat = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(plugin.language.locale))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.power.fixed") && !sender.hasPermission("mf.force.power")) {
            sender.sendMessage("$RED${plugin.language["CommandPowerFixedNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandPowerClearFixedUsage"]}")
            return true
        }
        val target = plugin.server.getOfflinePlayer(args[0])
        if (!target.isOnline && !target.hasPlayedBefore()) {
            sender.sendMessage("$RED${plugin.language["CommandPowerSetInvalidTarget"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val targetName = target.name ?: plugin.language["UnknownPlayer"]
                val targetMfPlayer = playerService.getPlayer(target)
                if (targetMfPlayer == null || !targetMfPlayer.isPowerFixed) {
                    sender.sendMessage("$RED${plugin.language["CommandPowerClearFixedNotFixed", targetName]}")
                    return@Runnable
                }
                // powerAtLogout follows power so that offline decay starts from the fixed amount.
                val updated = playerService.update(targetMfPlayer) {
                    it.copy(powerAtLogout = it.power, fixedPower = null)
                }.onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandPowerSetFailedToSaveTargetPlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage(
                    "$GREEN${plugin.language["CommandPowerClearFixedSuccess", targetName, decimalFormat.format(updated.power)]}"
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
