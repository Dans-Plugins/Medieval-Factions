package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.StringPrompt
import org.bukkit.entity.Player
import preponderous.ponder.command.unquote
import java.util.logging.Level

class MfFactionRoleRenameCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val conversationFactory = ConversationFactory(plugin)
        .withModality(true)
        .withFirstPrompt(NamePrompt())
        .withEscapeSequence(plugin.language["EscapeSequence"])
        .withLocalEcho(false)
        .thatExcludesNonPlayersWithMessage(plugin.language["CommandFactionRoleRenameNotAPlayer"])
        .addConversationAbandonedListener { event ->
            if (!event.gracefulExit()) {
                val conversable = event.context.forWhom
                if (conversable is Player) {
                    conversable.sendMessage(plugin.language["CommandFactionRoleRenameOperationCancelled"])
                }
            }
        }

    private inner class NamePrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String = plugin.language["CommandFactionRoleRenameNamePrompt", plugin.language["EscapeSequence"]]
        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            val conversable = context.forWhom
            if (conversable !is Player) return END_OF_CONVERSATION
            if (input == null) return END_OF_CONVERSATION
            val targetRole = context.getSessionData("role") as? MfFactionRole ?: return END_OF_CONVERSATION
            renameRole(conversable, targetRole, input, context.getSessionData("page") as? Int)
            return END_OF_CONVERSATION
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.role.rename")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleRenameNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleRenameNotAPlayer"]}")
            return true
        }
        var lastArgOffset = 0
        val returnPage = if (args.lastOrNull()?.startsWith("p=") == true) {
            lastArgOffset = 1
            args.last().substring("p=".length).toIntOrNull()
        } else {
            null
        }
        if (args.dropLast(lastArgOffset).isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleRenameUsage"]}")
            return true
        }
        val unquotedArgs = args.dropLast(lastArgOffset).toTypedArray().unquote()
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionRoleRenameFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleRenameMustBeInAFaction"]}")
                    return@Runnable
                }
                val targetRole = faction.roles.getRole(MfFactionRoleId(unquotedArgs[0])) ?: faction.roles.getRole(unquotedArgs[0])
                if (targetRole == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionRoleRenameInvalidTargetRole"]}")
                    return@Runnable
                }
                if (unquotedArgs.size < 2) {
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            val conversation = conversationFactory.buildConversation(sender)
                            conversation.context.setSessionData("role", targetRole)
                            conversation.context.setSessionData("page", returnPage)
                            conversation.begin()
                        }
                    )
                    return@Runnable
                }
                renameRole(sender, targetRole, unquotedArgs.drop(1).joinToString(" "), returnPage)
            }
        )
        return true
    }

    private fun renameRole(player: Player, targetRole: MfFactionRole, name: String, returnPage: Int?) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player)
                    ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                        player.sendMessage("$RED${plugin.language["CommandFactionRoleRenameFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    player.sendMessage("$RED${plugin.language["CommandFactionRoleRenameMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.modifyRole(targetRole.id))) {
                    player.sendMessage("$RED${plugin.language["CommandFactionRoleRenameNoFactionPermission"]}")
                    return@Runnable
                }
                if (faction.roles.any { it.name.equals(name, ignoreCase = true) }) {
                    player.sendMessage("$RED${plugin.language["CommandFactionRoleRenameRoleWithNameAlreadyExists"]}")
                    return@Runnable
                }
                factionService.save(
                    faction.copy(
                        roles = faction.roles.copy(
                            roles = faction.roles.map { existingRole ->
                                if (existingRole.id == targetRole.id) {
                                    existingRole.copy(name = name)
                                } else {
                                    existingRole
                                }
                            }
                        )
                    )
                ).onFailure {
                    player.sendMessage("$RED${plugin.language["CommandFactionRoleRenameFailedToSaveFaction"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                player.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionRoleRenameSuccess", targetRole.name, name]}")
                if (returnPage != null) {
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            player.performCommand("faction role list $returnPage")
                        }
                    )
                }
            }
        )
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player) return emptyList()
        val playerId = MfPlayerId.fromBukkitPlayer(sender)
        val factionService = plugin.services.factionService
        val faction = factionService.getFaction(playerId) ?: return emptyList()
        return when {
            args.isEmpty() -> faction.roles.map { it.name }
            args.size == 1 -> faction.roles.filter { it.name.lowercase().startsWith(args[0].lowercase()) }.map { it.name }
            else -> emptyList()
        }
    }
}
