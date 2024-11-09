package com.dansplugins.factionsystem.command.faction.apply

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionApplication
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionApplyCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.apply")) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyUsage"]}")
            return true
        }
        val targetFactionName = args.dropLast(1).joinToString(" ")
        val lastArg = args.last()
        if (lastArg.equals("cancel", ignoreCase = true)) {
            cancelApplication(sender, targetFactionName)
        } else {
            sendApplication(sender, args.joinToString(" "))
        }
        return true
    }

    private fun sendApplication(sender: Player, targetFactionName: String) {
        plugin.logger.info("Player " + sender.name + " is applying to faction " + targetFactionName)
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val factionService = plugin.services.factionService
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val faction = factionService.getFaction(mfPlayer.id)
                val target = factionService.getFaction(targetFactionName)
                if (target == null) {
                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyInvalidTarget"]}")
                    return@Runnable
                }
                if (faction != null) {
                    if (target.id.value == faction.id.value) {
                        sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyCannotApplyToCurrentFaction"]}")
                        return@Runnable
                    }
                }

                factionService.save(target.copy(applications = target.applications + MfFactionApplication(target.id, mfPlayer.id))).onFailure {
                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            }
        )
    }

    private fun cancelApplication(sender: Player, targetFactionName: String) {
        plugin.logger.info("Player " + sender.name + " is cancelling application to faction " + targetFactionName)
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val factionService = plugin.services.factionService
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyCancelFailedToSaveFaction"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val target = factionService.getFaction(targetFactionName)
                if (target == null) {
                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyCancelInvalidTarget"]}")
                    return@Runnable
                }

                // if the player has not applied to the faction, do nothing
                if (!target.applications.any { it.applicantId == mfPlayer.id }) {
                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyCancelNotApplied"]}")
                    return@Runnable
                }

                val updatedApplications = target.applications.filterNot { it.applicantId == mfPlayer.id }
                factionService.save(target.copy(applications = updatedApplications)).onFailure {
                    sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyCancelFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionApplyCancelSuccess", target.name]}")
            }
        )
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String>? {
        if (args.size == 1) {
            val factionService = plugin.services.factionService
            val factions = factionService.factions
            return factions.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }.toMutableList()
        }
        return null
    }
}
