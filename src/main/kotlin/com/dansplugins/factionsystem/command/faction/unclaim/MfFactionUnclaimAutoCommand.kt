package com.dansplugins.factionsystem.command.faction.unclaim

import com.dansplugins.factionsystem.MedievalFactions
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

class MfFactionUnclaimAutoCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.unclaim.auto") || !sender.hasPermission("mf.autounclaim")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAutounclaimNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAutounclaimNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionAutounclaimFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAutounclaimMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.toggleAutounclaim)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAutounclaimNoFactionPermission"]}")
                    return@Runnable
                }
                val updatedFaction = factionService.save(faction.copy(autounclaim = !faction.autounclaim)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAutounclaimFailedToSaveFaction"]}")
                    return@Runnable
                }

                if (updatedFaction.autounclaim) {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionAutounclaimEnabled"]}")
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            updatedFaction.sendMessage(
                                plugin.language["AutounclaimEnabledNotificationTitle"],
                                plugin.language["AutounclaimEnabledNotificationBody"]
                            )
                        }
                    )
                } else {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionAutounclaimDisabled"]}")
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            updatedFaction.sendMessage(
                                plugin.language["AutounclaimDisabledNotificationTitle"],
                                plugin.language["AutounclaimDisabledNotificationBody"]
                            )
                        }
                    )
                }
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
