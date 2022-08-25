package com.dansplugins.factionsystem.command.faction.law

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.REMOVE_LAW
import com.dansplugins.factionsystem.law.MfLawId
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionLawRemoveCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.removelaw")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionLawRemoveNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionLawRemoveUsage"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionLawRemoveNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer.fromBukkit(sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionLawRemoveFailedToSavePlayer"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            if (faction == null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionLawRemoveMustBeInAFaction"]}")
                return@Runnable
            }
            val role = faction.getRole(mfPlayer.id)
            if (role == null || !role.hasPermission(faction, REMOVE_LAW)) {
                sender.sendMessage("$RED${plugin.language["CommandFactionLawRemoveNoFactionPermission"]}")
                return@Runnable
            }
            val lawService = plugin.services.lawService
            val law = lawService.getLaw(MfLawId(args[0]))
            if (law == null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionLawRemoveInvalidLawId"]}")
                return@Runnable
            }
            lawService.delete(law.id).onFailure {
                sender.sendMessage("$RED${plugin.language["CommandFactionLawRemoveFailedToDeleteLaw"]}")
                return@Runnable
            }
            sender.sendMessage("$RED${plugin.language["CommandFactionLawRemoveSuccess"]}")
        })
        return true
    }
}