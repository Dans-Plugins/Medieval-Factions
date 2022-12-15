package com.dansplugins.factionsystem.command.faction.claim

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class MfFactionClaimCheckCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.claim.check") && !sender.hasPermission("mf.checkclaim")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionCheckClaimNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionCheckClaimNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val claimService = plugin.services.claimService
                val claim = claimService.getClaim(sender.location.chunk)
                if (claim == null) {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionCheckClaimNotClaimed"]}")
                    return@Runnable
                }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(claim.factionId)
                if (faction == null) {
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionCheckClaimNotClaimed"]}")
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionCheckClaimClaimed", faction.name]}")
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
