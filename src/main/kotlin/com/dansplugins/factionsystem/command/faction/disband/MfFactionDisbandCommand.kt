package com.dansplugins.factionsystem.command.faction.disband

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionDisbandCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.disband")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDisbandNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDisbandNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDisbandFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }

                // get faction
                val factionService = plugin.services.factionService
                val faction: MfFaction?
                if (args.isEmpty()) {
                    // attempt to use player's faction
                    faction = factionService.getFaction(mfPlayer.id)

                    if (faction == null) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDisbandMustBeInAFaction"]}")
                        return@Runnable
                    }

                    val role = faction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(faction, plugin.factionPermissions.disband)) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDisbandNoFactionPermission"]}")
                        return@Runnable
                    }
                    if (faction.members.size != 1) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDisbandFactionMustBeEmpty"]}")
                        return@Runnable
                    }
                } else {
                    // attempt to use specified faction
                    if (!sender.hasPermission("mf.disband.others")) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDisbandOthersNoPermission"]}")
                        return@Runnable
                    }

                    val nameOfFaction = args.joinToString(" ")
                    faction = factionService.getFaction(nameOfFaction)

                    if (faction == null) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionDisbandSpecifiedFactionNotFound"]}")
                        return@Runnable
                    }
                }

                // delete faction
                factionService.delete(faction.id).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDisbandFailedToDeleteFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to delete faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionDisbandSuccess"]}")
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val factionService = plugin.services.factionService
        return when {
            args.isEmpty() -> factionService.factions.map(MfFaction::name)
            args.size == 1 ->
                factionService.factions
                    .filter { it.name.lowercase().startsWith(args[0].lowercase()) }
                    .map(MfFaction::name)
            else -> emptyList()
        }
    }
}
