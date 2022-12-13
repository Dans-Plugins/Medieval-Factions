package com.dansplugins.factionsystem.command.faction.who

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.util.logging.Level.SEVERE

class MfFactionWhoCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.who")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionWhoNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionWhoUsage"]}")
            return true
        }
        val target = plugin.server.getOfflinePlayer(args[0])
        if (!target.isOnline && !target.hasPlayedBefore()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionWhoInvalidTarget"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val targetMfPlayer = playerService.getPlayer(target)
                    ?: playerService.save(MfPlayer(plugin, target)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionWhoFailedToSaveTargetPlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(targetMfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionWhoNotInAFaction", target.name ?: plugin.language["UnknownPlayer"]]}")
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionWhoSuccess", target.name ?: plugin.language["UnknownPlayer"], faction.name]}")
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
