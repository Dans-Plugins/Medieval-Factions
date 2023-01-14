package com.dansplugins.factionsystem.command.gate.create

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.gate.MfGateCreationContext
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.SELECTING_GATE_POSITION_1
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

class MfGateCreateCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.gate")) {
            sender.sendMessage("$RED${plugin.language["CommandGateCreateNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandGateCreateNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandGateCreateFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandGateCreateMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.createGate)) {
                    sender.sendMessage("$RED${plugin.language["CommandGateCreateNoFactionPermission"]}")
                    return@Runnable
                }
                val gateService = plugin.services.gateService
                val existingGates = gateService.getGatesByFaction(faction.id)
                val maxGates = plugin.config.getInt("gates.maxPerFaction")
                if (existingGates.size >= maxGates) {
                    sender.sendMessage("$RED${plugin.language["CommandGateCreateMaxGatesReached", maxGates.toString()]}")
                    return@Runnable
                }
                val gateCreationContext = gateService.getGateCreationContext(mfPlayer.id).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandGateCreateFailedToGetGateCreationContext"]}")
                    plugin.logger.log(SEVERE, "Failed to get gate creation context: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                if (gateCreationContext != null) {
                    sender.sendMessage("$RED${plugin.language["CommandGateCreateFailedAlreadyCreatingGate"]}")
                    return@Runnable
                }
                gateService.save(MfGateCreationContext(mfPlayer.id)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandGateCreateFailedToSaveGateCreationContext"]}")
                    plugin.logger.log(SEVERE, "Failed to save gate creation context: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val interactionService = plugin.services.interactionService
                interactionService.setInteractionStatus(mfPlayer.id, SELECTING_GATE_POSITION_1).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandGateCreateFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["GateCreateSelectFirstPosition"]}")
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
