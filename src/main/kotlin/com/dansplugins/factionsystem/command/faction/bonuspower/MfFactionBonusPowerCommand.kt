package com.dansplugins.factionsystem.command.faction.bonuspower

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.logging.Level.SEVERE

class MfFactionBonusPowerCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val decimalFormat = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(plugin.language.locale))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.force.bonuspower")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionBonusPowerNoPermission"]}")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionBonusPowerUsage"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val factionService = plugin.services.factionService
                val factionIdentifier = args.dropLast(1).joinToString(" ")
                val faction = factionService.getFaction(MfFactionId(factionIdentifier))
                    ?: factionService.getFaction(factionIdentifier)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionBonusPowerInvalidFaction"]}")
                    return@Runnable
                }
                val bonusPower = args.last().toDoubleOrNull()
                if (bonusPower == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionBonusPowerInvalidBonusPower"]}")
                    return@Runnable
                }
                val updatedFaction = factionService.save(faction.copy(bonusPower = bonusPower)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionBonusPowerFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionBonusPowerSuccess", updatedFaction.name, decimalFormat.format(updatedFaction.bonusPower)]}")
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
