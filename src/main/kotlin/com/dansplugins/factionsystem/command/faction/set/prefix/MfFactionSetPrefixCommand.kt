package com.dansplugins.factionsystem.command.faction.set.prefix

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

class MfFactionSetPrefixCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val conversationFactory = ConversationFactory(plugin)
        .withModality(true)
        .withFirstPrompt(PrefixPrompt())
        .withEscapeSequence(plugin.language["EscapeSequence"])
        .withLocalEcho(false)
        .thatExcludesNonPlayersWithMessage(plugin.language["CommandFactionSetPrefixNotAPlayer"])
        .addConversationAbandonedListener { event ->
            if (!event.gracefulExit()) {
                val conversable = event.context.forWhom
                if (conversable is Player) {
                    conversable.sendMessage(plugin.language["CommandFactionSetPrefixOperationCancelled"])
                }
            }
        }

    private inner class PrefixPrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String = plugin.language["CommandFactionSetPrefixPrefixPrompt", plugin.language["EscapeSequence"]]
        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            val conversable = context.forWhom
            if (conversable !is Player) return END_OF_CONVERSATION
            if (input == null) return END_OF_CONVERSATION
            setFactionPrefix(conversable, input)
            return END_OF_CONVERSATION
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.prefix")) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionSetPrefixNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionSetPrefixNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            conversationFactory.buildConversation(sender).begin()
            return true
        }
        setFactionPrefix(sender, args.joinToString(" "))
        return true
    }

    private fun setFactionPrefix(player: Player, prefix: String) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player)
                    ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                        player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionSetPrefixFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionSetPrefixMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.changePrefix)) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionSetPrefixNoFactionPermission"]}")
                    return@Runnable
                }
                factionService.save(faction.copy(prefix = prefix)).onFailure {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionSetPrefixFailedToSaveFaction"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                player.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionSetPrefixSuccess", prefix]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        player.performCommand("faction info")
                    }
                )
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
