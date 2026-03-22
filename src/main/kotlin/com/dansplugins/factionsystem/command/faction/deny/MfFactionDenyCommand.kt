package com.dansplugins.factionsystem.command.faction.deny

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.approval.MfApprovalRequestId
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MfFactionDenyCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.approve")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDenyNoPermission"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDenyUsage"]}")
            return true
        }
        val requestId = MfApprovalRequestId(args[0])
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val approvalService = plugin.services.approvalRequestService
                val request = approvalService.getRequest(requestId)
                if (request == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionDenyRequestNotFound"]}")
                    return@Runnable
                }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(request.factionId)
                val target = factionService.getFaction(request.targetId)
                val factionName = faction?.name ?: "Unknown"
                val targetName = target?.name ?: "Unknown"
                approvalService.removeRequest(requestId)
                sender.sendMessage("$GREEN${plugin.language["CommandFactionDenySuccess", factionName, targetName, request.type.name.lowercase()]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        faction?.sendMessage(
                            plugin.language["FactionApprovalDeniedNotificationTitle"],
                            plugin.language["FactionApprovalDeniedNotificationBody", request.type.name.lowercase(), targetName]
                        )
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
    ): List<String> {
        return when {
            args.isEmpty() || args.size == 1 -> {
                val approvalService = plugin.services.approvalRequestService
                val ids = approvalService.getAllRequests().map { it.id.value }
                if (args.isEmpty()) ids else ids.filter { it.lowercase().startsWith(args[0].lowercase()) }
            }
            else -> emptyList()
        }
    }
}
