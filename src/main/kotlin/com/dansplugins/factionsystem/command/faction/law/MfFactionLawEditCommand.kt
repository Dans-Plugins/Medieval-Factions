package com.dansplugins.factionsystem.command.faction.law

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.StringPrompt
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionLawEditCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.law.edit")) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawEditNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawEditNotAPlayer"]}")
            return true
        }
        if (args.size == 1) {
            val conversation = conversationFactory.buildConversation(sender)
            conversation.context.setSessionData("lawNumber", args[0])
            conversation.begin()
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawEditUsage"]}")
            return true
        }
        editLaw(sender, args)
        return true
    }

    private val conversationFactory = ConversationFactory(plugin)
        .withModality(true)
        .withFirstPrompt(EditPrompt())
        .withEscapeSequence(plugin.language["EscapeSequence"])
        .withLocalEcho(false)
        .thatExcludesNonPlayersWithMessage(plugin.language["CommandFactionLawEditNotAPlayer"])
        .addConversationAbandonedListener { event ->
            if (!event.gracefulExit()) {
                val conversable = event.context.forWhom
                if (conversable is Player) {
                    conversable.sendMessage(plugin.language["CommandFactionLawEditOperationCancelled"])
                }
            }
        }

    private inner class EditPrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String = plugin.language["CommandFactionLawEditPrompt", plugin.language["EscapeSequence"]]
        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            val conversable = context.forWhom
            if (conversable !is Player) return END_OF_CONVERSATION
            if (input == null) return END_OF_CONVERSATION
            editLaw(conversable, arrayOf(context.getSessionData("lawNumber").toString(), input))
            return END_OF_CONVERSATION
        }
    }

    private fun editLaw(player: Player, args: Array<out String>) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player)
                    ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                        player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawEditFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawEditMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.editLaw)) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawEditNoFactionPermission"]}")
                    return@Runnable
                }
                val lawService = plugin.services.lawService
                val textArgs = args.drop(1).toTypedArray()
                val law = lawService.getLaw(faction.id, args.elementAt(0).toInt()) ?: return@Runnable
                lawService.edit(law, textArgs.joinToString(" "))
                    .onFailure {
                        player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawEditLawFailedToSave"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to edit law: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                player.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionLawEditSuccess"]}")
            }
        )
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}
