package com.dansplugins.factionsystem.command.faction.admin

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.dropFirst
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.faction.role.MfFactionRoles.Companion.OWNER_ROLE_NAME
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.util.logging.Level.SEVERE

class MfFactionAdminSetLeaderCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.admin.setleader")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderNoPermission"]}")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderUsage"]}")
            return true
        }

        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                // Get target player
                val targetPlayer = plugin.server.getOfflinePlayer(args[0])
                if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderInvalidTargetPlayer"]}")
                    return@Runnable
                }

                val playerService = plugin.services.playerService
                val targetMfPlayer = playerService.getPlayer(targetPlayer)
                    ?: playerService.save(MfPlayer(plugin, targetPlayer)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }

                val factionService = plugin.services.factionService

                // Get target faction
                val targetFaction = factionService.getFaction(args.dropFirst().joinToString(" "))
                if (targetFaction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderInvalidTargetFaction"]}")
                    return@Runnable
                }

                // A player in another faction is refused. A current member of the target faction is promoted in
                // place, so a leaderless faction can be given a leader from its own members.
                val currentFaction = factionService.getFaction(targetMfPlayer.id)
                if (currentFaction != null && currentFaction.id != targetFaction.id) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderTargetPlayerAlreadyInFaction"]}")
                    return@Runnable
                }
                val existingMember = targetFaction.members.singleOrNull { it.playerId == targetMfPlayer.id }

                val maxMembers = plugin.config.getInt("factions.maxMembers")
                if (existingMember == null && maxMembers > 0 && targetFaction.members.size >= maxMembers) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderTargetFactionFull"]}")
                    return@Runnable
                }

                // Find the Owner role. If the faction's members have deleted it, nobody may be able to recreate it
                // from inside the faction, so this admin path rebuilds it from the default template instead of
                // refusing. See https://github.com/Dans-Plugins/Medieval-Factions/issues/1797.
                val existingOwnerRole = targetFaction.roles.find { it.name.equals(OWNER_ROLE_NAME, ignoreCase = true) }
                val ownerRole = existingOwnerRole
                    ?: MfFactionRoles.recreateOwner(plugin, targetFaction.id, targetFaction.roles.roles)
                val roles = if (existingOwnerRole == null) {
                    MfFactionRoles(targetFaction.roles.defaultRoleId, targetFaction.roles.roles + ownerRole)
                } else {
                    targetFaction.roles
                }

                if (existingMember != null && existingMember.role.id == ownerRole.id) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderAlreadyLeader", targetMfPlayer.name ?: plugin.language["CommandFactionAdminSetLeaderUnknownPlayer"], targetFaction.name]}")
                    return@Runnable
                }
                val members = if (existingMember != null) {
                    targetFaction.members.map { if (it.playerId == targetMfPlayer.id) it.copy(role = ownerRole) else it }
                } else {
                    targetFaction.members + MfFactionMember(targetMfPlayer.id, ownerRole)
                }

                // Add the player as the owner, or move an existing member into the Owner role
                val updatedFaction = factionService.save(
                    targetFaction.copy(
                        roles = roles,
                        members = members,
                        invites = targetFaction.invites.filter { it.playerId != targetMfPlayer.id }
                    )
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }

                val targetName = targetMfPlayer.name ?: plugin.language["CommandFactionAdminSetLeaderUnknownPlayer"]
                if (existingOwnerRole == null) {
                    plugin.logger.info(
                        "Faction ${targetFaction.name} (${targetFaction.id.value}) had no $OWNER_ROLE_NAME role; " +
                            "recreated it from the default template as role ${ownerRole.id.value} while ${sender.name} " +
                            "set $targetName as its leader"
                    )
                    sender.sendMessage("$GREEN${plugin.language["CommandFactionAdminSetLeaderRecreatedOwnerRole", targetFaction.name]}")
                }
                updatedFaction.sendMessage(
                    plugin.language["FactionNewLeaderNotificationTitle", targetName],
                    plugin.language["FactionNewLeaderNotificationBody", targetName]
                )
                sender.sendMessage(
                    "$GREEN${plugin.language["CommandFactionAdminSetLeaderSuccess", targetName, targetFaction.name]}"
                )

                try {
                    factionService.cancelAllApplicationsForPlayer(targetMfPlayer)
                } catch (e: Exception) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAdminSetLeaderFailedToCancelApplications"]}")
                    plugin.logger.log(SEVERE, "Failed to cancel applications: ${e.message}", e)
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
    ): List<String> {
        val factionService = plugin.services.factionService
        return when {
            args.isEmpty() ->
                plugin.server.onlinePlayers
                    .mapNotNull { it.name }
            args.size == 1 ->
                plugin.server.onlinePlayers
                    .filter { it.name?.lowercase()?.startsWith(args[0].lowercase()) == true }
                    .mapNotNull { it.name }
            args.size == 2 ->
                factionService.factions
                    .filter { it.name.lowercase().startsWith(args[1].lowercase()) }
                    .map(MfFaction::name)
            else -> emptyList()
        }
    }
}
