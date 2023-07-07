package com.dansplugins.factionsystem.command.faction.dev

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayerId
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionDevPowerCycleCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val onlinePlayers = plugin.server.onlinePlayers
        val onlineMfPlayerIds = onlinePlayers.map(MfPlayerId.Companion::fromBukkitPlayer)
        val disbandZeroPowerFactions = plugin.config.getBoolean("factions.zeroPowerFactionsGetDisbanded")
        val initialPower = plugin.config.getDouble("players.initialPower")
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                plugin.onPowerCycle(
                    onlineMfPlayerIds,
                    initialPower,
                    onlinePlayers,
                    disbandZeroPowerFactions
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
    ) = emptyList<String>()
}
