package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
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
import java.util.logging.Level.SEVERE

class MfFactionRoleDeleteCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val conversationFactory = ConversationFactory(plugin)
        .withModality(true)
        .withFirstPrompt(NamePrompt())
        .withEscapeSequence(plugin.language["EscapeSequence"])
        .withLocalEcho(false)
        .thatExcludesNonPlayersWithMessage(plugin.language["CommandFactionRoleDeleteNotAPlayer"])
        .addConversationAbandonedListener { event ->
            if (!event.gracefulExit()) {
                val conversable = event.context.forWhom
                if (conversable is Player) {
                    conversable.sendMessage(plugin.language["CommandFactionRoleDeleteOperationCancelled"])
                }
            }
        }

    private inner class NamePrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String = plugin.language["CommandFactionRoleDeleteNamePrompt", plugin.language["EscapeSequence"]]
        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            val conversable = context.forWhom
            if (conversable !is Player) return END_OF_CONVERSATION
            if (input == null) return END_OF_CONVERSATION
            deleteRole(conversable, input, context.getSessionData("page") as? Int, context.getSessionData("faction") as? MfFactionId)
            return END_OF_CONVERSATION
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean =
        execute(sender, args, null)

    /**
     * @param forcedFactionId a faction named with mf.force.role (see [MfFactionRoleCommand]). It replaces the sender's own
     * faction, and its role-permission checks are skipped.
     */
    fun execute(sender: CommandSender, args: Array<out String>, forcedFactionId: MfFactionId?): Boolean {
        if (!sender.hasPermission("mf.role.delete")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleDeleteNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleDeleteNotAPlayer"]}")
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
            val conversation = conversationFactory.buildConversation(sender)
            conversation.context.setSessionData("page", returnPage)
            conversation.context.setSessionData("faction", forcedFactionId)
            conversation.begin()
            return true
        }
        deleteRole(sender, args.dropLast(lastArgOffset).joinToString(" "), returnPage, forcedFactionId)
        return true
    }

    private fun deleteRole(player: Player, name: String, returnPage: Int?, forcedFactionId: MfFactionId?) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player)
                    ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                        player.sendMessage("$RED${plugin.language["CommandFactionRoleDeleteFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = if (forcedFactionId != null) {
                    factionService.getFaction(forcedFactionId) ?: run {
                        player.sendMessage("$RED${plugin.language["CommandFactionRoleInvalidFaction", forcedFactionId.value]}")
                        return@Runnable
                    }
                } else {
                    factionService.getFaction(mfPlayer.id) ?: run {
                        player.sendMessage("$RED${plugin.language["CommandFactionRoleDeleteMustBeInAFaction"]}")
                        return@Runnable
                    }
                }
                val roleToRemove = faction.roles.getRole(MfFactionRoleId(name)) ?: faction.roles.getRole(name)
                if (roleToRemove == null) {
                    player.sendMessage("$RED${plugin.language["CommandFactionRoleDeleteInvalidRole"]}")
                    return@Runnable
                }
                if (forcedFactionId == null) {
                    val role = faction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(faction, plugin.factionPermissions.deleteRole(roleToRemove.id))) {
                        player.sendMessage("$RED${plugin.language["CommandFactionRoleDeleteNoFactionPermission"]}")
                        return@Runnable
                    }
                }
                if (faction.members.any { it.role.id == roleToRemove.id }) {
                    player.sendMessage("$RED${plugin.language["CommandFactionRoleDeleteCannotDeleteWithMembersInRole"]}")
                    return@Runnable
                }
                if (faction.roles.defaultRoleId == roleToRemove.id) {
                    player.sendMessage("$RED${plugin.language["CommandFactionRoleDeleteCannotDeleteDefaultRole"]}")
                    return@Runnable
                }
                factionService.save(
                    faction.copy(
                        roles = MfFactionRoles(
                            faction.roles.defaultRoleId,
                            faction.roles.filter { it.id != roleToRemove.id }
                        )
                    )
                ).onFailure {
                    player.sendMessage("$RED${plugin.language["CommandFactionRoleDeleteFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                player.sendMessage("$GREEN${plugin.language["CommandFactionRoleDeleteSuccess", roleToRemove.name]}")
                if (returnPage != null) {
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            player.performCommand("${roleCommandPrefix(forcedFactionId)} list $returnPage")
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
