package com.dansplugins.factionsystem.command.gate.remove

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Level.SEVERE

class MfGateRemoveCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.gate")) {
            sender.sendMessage("$RED${plugin.language["CommandGateRemoveNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandGateRemoveNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandGateRemoveFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandGateRemoveMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.removeGate)) {
                    sender.sendMessage("$RED${plugin.language["CommandGateRemoveNoFactionPermission"]}")
                    return@Runnable
                }
                val gateService = plugin.services.gateService
                val existingGates = gateService.getGatesByFaction(faction.id)
                // We work with squared distances since calculating square root is an expensive call and best avoided where possible.
                val gateDistanceSquared = existingGates.associateWith { gate ->
                    MfBlockPosition.fromBukkitLocation(sender.location)?.let { gate.area.distanceSquared(it) }
                }
                plugin.logger.log(INFO, gateDistanceSquared.map { (key, value) -> "${key.id.value}: $value" }.joinToString(", ", "{", "}"))
                val closestGate = gateDistanceSquared.keys.minByOrNull { gateDistanceSquared[it] ?: Int.MAX_VALUE }
                val distanceSquared = gateDistanceSquared[closestGate]
                val minRemovalDistanceSquared = plugin.config.getInt("gates.maxRemoveDistance") * plugin.config.getInt("gates.maxRemoveDistance")
                if (closestGate == null || distanceSquared == null || distanceSquared > minRemovalDistanceSquared) {
                    sender.sendMessage("$RED${plugin.language["CommandGateRemoveFailedToFindGate"]}")
                    return@Runnable
                }
                gateService.delete(closestGate.id).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandGateRemoveFailedToDeleteGate"]}")
                    plugin.logger.log(SEVERE, "Failed to delete gate: ${it.reason.message}", it.reason.message)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandGateRemoveSuccess"]}")
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
