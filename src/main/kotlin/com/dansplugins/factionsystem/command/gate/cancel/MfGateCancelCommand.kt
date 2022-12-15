package com.dansplugins.factionsystem.command.gate.cancel

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfGateCancelCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandGateCancelMustBeAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandGateCancelFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val gateService = plugin.services.gateService
                val gateCreationContext = gateService.getGateCreationContext(mfPlayer.id).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandGateCancelFailedToGetGateCreationContext"]}")
                    plugin.logger.log(SEVERE, "Failed to get gate creation context: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                if (gateCreationContext == null) {
                    sender.sendMessage("$RED${plugin.language["CommandGateCancelFailedNotCreatingGate"]}")
                    return@Runnable
                }
                gateService.deleteGateCreationContext(mfPlayer.id).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandGateCancelFailedToDeleteGateCreationContext"]}")
                    plugin.logger.log(SEVERE, "Failed to delete gate creation context: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val interactionService = plugin.services.interactionService
                interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandGateCancelFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandGateCancelSuccess"]}")
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
