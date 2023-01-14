package com.dansplugins.factionsystem.command.faction.law

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.law.MfLawId
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

class MfFactionLawMoveCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.law.move")) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawMoveNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawMoveNotAPlayer"]}")
            return true
        }
        if (args.size == 1) {
            val conversation = conversationFactory.buildConversation(sender)
            conversation.context.setSessionData("lawReference", args[0])
            conversation.begin()
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawMoveUsage"]}")
            return true
        }
        moveLaw(sender, args)
        return true
    }

    private val conversationFactory = ConversationFactory(plugin)
        .withModality(true)
        .withFirstPrompt(MovePrompt())
        .withEscapeSequence(plugin.language["EscapeSequence"])
        .withLocalEcho(false)
        .thatExcludesNonPlayersWithMessage(plugin.language["CommandFactionLawMoveNotAPlayer"])
        .addConversationAbandonedListener { event ->
            if (!event.gracefulExit()) {
                val conversable = event.context.forWhom
                if (conversable is Player) {
                    conversable.sendMessage(plugin.language["CommandFactionLawMoveOperationCancelled"])
                }
            }
        }

    private inner class MovePrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String = plugin.language["CommandFactionLawMovePrompt", plugin.language["EscapeSequence"]]
        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            val conversable = context.forWhom
            if (conversable !is Player) return END_OF_CONVERSATION
            if (input == null) return END_OF_CONVERSATION
            moveLaw(conversable, arrayOf(context.getSessionData("lawReference").toString(), input))
            return END_OF_CONVERSATION
        }
    }

    private fun moveLaw(player: Player, args: Array<out String>) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player)
                    ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                        player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawMoveFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawMoveMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.moveLaw)) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawMoveNoFactionPermission"]}")
                    return@Runnable
                }
                val lawService = plugin.services.lawService
                val law = lawService.getLaw(MfLawId(args.elementAt(0))) ?: lawService.getLaw(faction.id, args.elementAt(0).toIntOrNull())
                if (law == null) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawMoveNotNumberOrId"]}")
                    return@Runnable
                }
                if (law.factionId != faction.id) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawMoveNotYourFaction"]}")
                    return@Runnable
                }
                lawService.move(law, args.elementAt(1).toInt())
                    .onFailure {
                        player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawMoveFailedToMove"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to move law: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                player.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionLawMoveSuccess"]}")
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
