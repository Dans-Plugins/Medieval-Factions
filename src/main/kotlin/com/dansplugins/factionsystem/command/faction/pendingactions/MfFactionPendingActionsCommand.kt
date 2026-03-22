package com.dansplugins.factionsystem.command.faction.pendingactions

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.YELLOW
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionPendingActionsCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.approve")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionPendingActionsNoPermission"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val approvalService = plugin.services.approvalRequestService
                val requests = approvalService.getAllRequests()
                if (requests.isEmpty()) {
                    sender.sendMessage("$GRAY${plugin.language["CommandFactionPendingActionsNone"]}")
                    return@Runnable
                }
                val factionService = plugin.services.factionService
                sender.sendMessage("$AQUA${plugin.language["CommandFactionPendingActionsHeader"]}")
                for (request in requests) {
                    val faction = factionService.getFaction(request.factionId)
                    val target = factionService.getFaction(request.targetId)
                    val factionName = faction?.name ?: "Unknown"
                    val targetName = target?.name ?: "Unknown"
                    val reasonText = if (request.reason != null) " - ${request.reason}" else ""
                    sender.sendMessage(
                        "$YELLOW${plugin.language["CommandFactionPendingActionsEntry", request.id.value, request.type.name.lowercase(), factionName, targetName]}$GRAY$reasonText"
                    )
                }
                sender.sendMessage("$GRAY${plugin.language["CommandFactionPendingActionsFooter"]}")
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> = emptyList()
}
