package com.dansplugins.factionsystem.command.faction.admin

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.util.logging.Level.SEVERE

class MfFactionAdminCreateCommand(
    private val plugin: MedievalFactions,
) : CommandExecutor,
    TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (!sender.hasPermission("mf.admin.create")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAdminCreateNoPermission"]}")
            return true
        }

        val allowLeaderlessFactions = plugin.config.getBoolean("factions.allowLeaderlessFactions")
        if (!allowLeaderlessFactions) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAdminCreateFeatureDisabled"]}")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAdminCreateUsage"]}")
            return true
        }

        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val factionName = args.joinToString(" ")
                val maxFactionNameLength = plugin.config.getInt("factions.maxNameLength")

                if (factionName.length > maxFactionNameLength) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionCreateNameTooLong", maxFactionNameLength.toString()]}")
                    return@Runnable
                }

                val factionService = plugin.services.factionService
                if (factionService.getFaction(factionName) != null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionCreateFactionAlreadyExists"]}")
                    return@Runnable
                }

                val factionId = MfFactionId.generate()
                val roles = MfFactionRoles.defaults(plugin, factionId)
                val faction =
                    MfFaction(
                        plugin = plugin,
                        id = factionId,
                        name = factionName,
                        roles = roles,
                        members = emptyList(),
                    )

                val createdFaction =
                    factionService.save(faction).onFailure { failure ->
                        sender.sendMessage("$RED${plugin.language["CommandFactionCreateFactionFailedToSave"]}")
                        plugin.logger.log(SEVERE, "Failed to save faction: ${failure.reason.message}", failure.reason.cause)
                        return@Runnable
                    }

                sender.sendMessage("$GREEN${plugin.language["CommandFactionAdminCreateSuccess", createdFaction.name]}")
            },
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ) = emptyList<String>()
}
