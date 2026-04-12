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
                val messages = mutableListOf<String>()
                if (requests.isEmpty()) {
                    messages.add("$GRAY${plugin.language["CommandFactionPendingActionsNone"]}")
                } else {
                    val factionService = plugin.services.factionService
                    messages.add("$AQUA${plugin.language["CommandFactionPendingActionsHeader"]}")
                    for (request in requests) {
                        val faction = factionService.getFaction(request.factionId)
                        val target = factionService.getFaction(request.targetId)
                        val factionName = faction?.name ?: plugin.language["UnknownFaction"]
                        val targetName = target?.name ?: plugin.language["UnknownFaction"]
                        val requestTypeLocalized = plugin.language["ApprovalRequestType${request.type.name.lowercase().replaceFirstChar { it.uppercase() }}"]
                        val reasonText = if (request.reason != null) " - ${request.reason}" else ""
                        messages.add(
                            "$YELLOW${plugin.language["CommandFactionPendingActionsEntry", request.id.value, requestTypeLocalized, factionName, targetName]}$GRAY$reasonText"
                        )
                    }
                    messages.add("$GRAY${plugin.language["CommandFactionPendingActionsFooter"]}")
                }
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        for (message in messages) {
                            sender.sendMessage(message)
                        }
                    }
                )
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
