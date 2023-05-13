package com.dansplugins.factionsystem.command.faction.claim

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

class MfFactionClaimAutoCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.claim.auto") || !sender.hasPermission("mf.autoclaim")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAutoclaimNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAutoclaimNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionAutoclaimFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAutoclaimMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.toggleAutoclaim)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAutoclaimNoFactionPermission"]}")
                    return@Runnable
                }
                val updatedFaction = factionService.save(faction.copy(autoclaim = !faction.autoclaim)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAutoclaimFailedToSaveFaction"]}")
                    return@Runnable
                }

                if (updatedFaction.autoclaim) {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionAutoclaimEnabled"]}")
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            updatedFaction.sendMessage(
                                plugin.language["AutoclaimEnabledNotificationTitle"],
                                plugin.language["AutoclaimEnabledNotificationBody"]
                            )
                        }
                    )
                } else {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionAutoclaimDisabled"]}")
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            updatedFaction.sendMessage(
                                plugin.language["AutoclaimDisabledNotificationTitle"],
                                plugin.language["AutoclaimDisabledNotificationBody"]
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
