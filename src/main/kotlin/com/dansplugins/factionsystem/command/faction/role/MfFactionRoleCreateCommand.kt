package com.dansplugins.factionsystem.command.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.CREATE_ROLE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.DELETE_ROLE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.MODIFY_ROLE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.SET_MEMBER_ROLE
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission.Companion.VIEW_ROLE
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.StringPrompt
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionRoleCreateCommand(private val plugin: MedievalFactions) : CommandExecutor {

    private val conversationFactory = ConversationFactory(plugin)
        .withModality(true)
        .withFirstPrompt(NamePrompt())
        .withEscapeSequence(plugin.language["EscapeSequence"])
        .withLocalEcho(false)
        .thatExcludesNonPlayersWithMessage(plugin.language["CommandFactionRoleCreateNotAPlayer"])
        .addConversationAbandonedListener { event ->
            if (!event.gracefulExit()) {
                val conversable = event.context.forWhom
                if (conversable is Player) {
                    conversable.sendMessage(plugin.language["CommandFactionRoleCreateOperationCancelled"])
                }
            }
        }

    private inner class NamePrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String = plugin.language["CommandFactionRoleCreateNamePrompt", plugin.language["EscapeSequence"]]
        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            val conversable = context.forWhom
            if (conversable !is Player) return END_OF_CONVERSATION
            if (input == null) return END_OF_CONVERSATION
            createRole(conversable, input, context.getSessionData("page") as? Int)
            return END_OF_CONVERSATION
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.role.create")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleCreateNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionRoleCreateNotAPlayer"]}")
            return true
        }
        var lastArgOffset = 0
        val returnPage = if (args.lastOrNull()?.startsWith("p=") == true) {
            lastArgOffset = 1
            args.last().substring("p=".length).toIntOrNull()
        } else null
        if (args.dropLast(lastArgOffset).isEmpty()) {
            val conversation = conversationFactory.buildConversation(sender)
            conversation.context.setSessionData("page", returnPage)
            conversation.begin()
            return true
        }
        createRole(sender, args.dropLast(lastArgOffset).joinToString(" "), returnPage)
        return true
    }

    private fun createRole(player: Player, name: String, returnPage: Int?) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(player)
                ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                    player.sendMessage("$RED${plugin.language["CommandFactionRoleCreateFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(mfPlayer.id)
            if (faction == null) {
                player.sendMessage("$RED${plugin.language["CommandFactionRoleCreateMustBeInAFaction"]}")
                return@Runnable
            }
            val role = faction.getRole(mfPlayer.id)
            if (role == null || !role.hasPermission(faction, CREATE_ROLE)) {
                player.sendMessage("$RED${plugin.language["CommandFactionRoleCreateNoFactionPermission"]}")
                return@Runnable
            }
            if (faction.roles.any { it.name.equals(name, ignoreCase = true) }) {
                player.sendMessage("$RED${plugin.language["CommandFactionRoleCreateRoleWithNameAlreadyExists"]}")
                return@Runnable
            }
            val newRole = MfFactionRole(name = name)
            factionService.save(
                faction.copy(
                    roles = MfFactionRoles(
                        faction.roles.defaultRoleId,
                        faction.roles.map { existingRole ->
                            existingRole.copy(permissions = existingRole.permissions + buildMap {
                                if (existingRole.hasPermission(faction, VIEW_ROLE(role.id))) {
                                    put(VIEW_ROLE(newRole.id), true)
                                }
                                if (existingRole.hasPermission(faction, MODIFY_ROLE(role.id))) {
                                    put(MODIFY_ROLE(newRole.id), true)
                                }
                                if (existingRole.hasPermission(faction, SET_MEMBER_ROLE(role.id))) {
                                    put(SET_MEMBER_ROLE(newRole.id), true)
                                }
                                if (existingRole.hasPermission(faction, DELETE_ROLE(role.id))) {
                                    put(DELETE_ROLE(newRole.id), true)
                                }
                            })
                        } + newRole
                    )
                )
            ).onFailure {
                player.sendMessage("$RED${plugin.language["CommandFactionRoleCreateFailedToSaveFaction"]}")
                plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            player.sendMessage("$GREEN${plugin.language["CommandFactionRoleCreateSuccess", name]}")
            if (returnPage != null) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    player.performCommand("faction role list $returnPage")
                })
            }
        })
    }
}