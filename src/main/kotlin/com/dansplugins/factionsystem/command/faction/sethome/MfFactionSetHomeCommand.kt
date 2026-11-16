package com.dansplugins.factionsystem.command.faction.sethome

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.command.unquote
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
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

class MfFactionSetHomeCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.sethome")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionSetHomeNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionSetHomeNotAPlayer"]}")
            return true
        }
        val senderLocation = sender.location
        // With mf.force.sethome, `/f sethome <faction>` (by ID or name, quoted if it has spaces) sets the named
        // faction's home without its role check (#1898). The home must still be inside that faction's territory.
        val forcedFactionName = if (args.isNotEmpty() && sender.hasPermission("mf.force.sethome")) {
            args.unquote().joinToString(" ")
        } else {
            null
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionSetHomeFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction: MfFaction?
                if (forcedFactionName != null) {
                    faction = factionService.getFaction(MfFactionId(forcedFactionName)) ?: factionService.getFaction(forcedFactionName)
                    if (faction == null) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionForceInvalidFaction", forcedFactionName]}")
                        return@Runnable
                    }
                } else {
                    faction = factionService.getFaction(mfPlayer.id)
                    if (faction == null) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionSetHomeMustBeInAFaction"]}")
                        return@Runnable
                    }
                    val role = faction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(faction, plugin.factionPermissions.setHome)) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionSetHomeNoFactionPermission"]}")
                        return@Runnable
                    }
                }
                val claimService = plugin.services.claimService
                val claim = claimService.getClaim(senderLocation.chunk)
                if (claim == null || claim.factionId.value != faction.id.value) {
                    if (forcedFactionName != null) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionForceSetHomeNoClaim", faction.name]}")
                    } else {
                        sender.sendMessage("$RED${plugin.language["CommandFactionSetHomeNoClaim"]}")
                    }
                    return@Runnable
                }
                factionService.save(faction.copy(home = MfPosition.fromBukkitLocation(senderLocation))).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionSetHomeFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                if (forcedFactionName != null) {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionForceSetHomeSuccess", faction.name]}")
                    plugin.logger.info("${sender.name} set the home of ${faction.name} (${faction.id.value}) with mf.force.sethome")
                } else {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionSetHomeSuccessful"]}")
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
